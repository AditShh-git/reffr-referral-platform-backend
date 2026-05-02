package com.Reffr_Backend.module.referral.service.impl;

import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.security.annotation.Idempotent;
import com.Reffr_Backend.common.util.SanitizerUtils;
import com.Reffr_Backend.config.AppProperties;
import com.Reffr_Backend.module.chat.service.ChatService;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.repository.PostRepository;
import com.Reffr_Backend.module.referral.dto.ReferralRequestDto;
import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.entity.ReferralStatus;
import com.Reffr_Backend.module.referral.event.ReferralLifecycleEvent;
import com.Reffr_Backend.module.referral.repository.ReferralRequestRepository;
import com.Reffr_Backend.module.referral.service.ReferralAuditService;
import com.Reffr_Backend.module.referral.service.ReferralService;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.infrastructure.ResumeSnapshotService;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API-level orchestrator for referral operations.
 *
 * <p>This class is intentionally kept thin — it coordinates the following
 * focused services, each responsible for one concern:
 * <ul>
 *   <li>{@link com.Reffr_Backend.module.referral.service.ReferralAuditService} — audit log writes</li>
 *   <li>{@link com.Reffr_Backend.module.referral.service.ReferralNotificationService} — in-app + email notifications</li>
 *   <li>{@link com.Reffr_Backend.module.chat.service.ChatService} — chat creation (ONLY after acceptance)</li>
 *   <li>{@link com.Reffr_Backend.module.user.infrastructure.ResumeSnapshotService} — resume snapshot logic</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    private final ReferralRequestRepository referralRepository;
    private final PostRepository            postRepository;
    private final UserRepository            userRepository;
    private final ChatService               chatService;
    private final ReferralAuditService      auditService;
    private final ResumeSnapshotService     resumeSnapshotService;
    private final AppProperties             appProperties;
    /** Used to publish {@link ReferralLifecycleEvent}s that are consumed by
     *  {@link com.Reffr_Backend.module.referral.event.ReferralNotificationEventListener}
     *  <em>after</em> the transaction commits, preventing spurious notifications
     *  on rollback and ensuring the referral row is visible to notification recipients. */
    private final ApplicationEventPublisher eventPublisher;

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE — Volunteer on REQUEST post
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Called when a volunteer clicks "Refer" on a REQUEST post.
     * Sets status = PENDING. Does NOT open chat. Does NOT auto-accept.
     *
     * Flow: PENDING → (seeker accepts) → ACCEPTED → chat created.
     */
    @Override
    @Transactional
    @Idempotent
    public void createReferral(UUID postId, UUID currentUserId, String message) {
        Post post = requireOpenPost(postId);

        if (post.getType() != Post.PostType.REQUEST) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST,
                    "Use POST /referrals/{postId}/apply for OFFER posts");
        }

        UUID requesterId = post.getAuthor().getId();   // seeker = post author
        UUID referrerId  = currentUserId;              // volunteer = current user

        validateNotSelf(requesterId, referrerId);
        validateNoDuplicate(postId, requesterId, referrerId);

        // Limit check — count PENDING + ON_HOLD as "active" slots
        long activeCount = referralRepository.countByPostIdAndStatusIn(
                postId, List.of(ReferralStatus.PENDING, ReferralStatus.ON_HOLD));
        if (activeCount >= post.getMaxVolunteers()) {
            post.markFull();
            postRepository.save(post);
            throw new BusinessException(ErrorCodes.MAX_VOLUNTEERS_REACHED,
                    "This post has reached its maximum number of volunteers (" + post.getMaxVolunteers() + ")");
        }

        User referrer = userRepository.findById(referrerId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        ReferralRequest referral = ReferralRequest.builder()
                .post(post)
                .requester(userRepository.getReferenceById(requesterId))
                .referrer(referrer)
                .status(ReferralStatus.PENDING)
                .volunteerNote(SanitizerUtils.sanitize(message))
                .expiresAt(LocalDateTime.now().plusDays(appProperties.getReferral().getExpiryDays()))
                .build();

        // saveAndFlush guarantees the referral INSERT is sent to the DB before the
        // audit log INSERT below — prevents the referral_audit_log FK violation.
        ReferralRequest saved = referralRepository.saveAndFlush(referral);

        // Increment post's referral counter
        postRepository.incrementReferralCount(postId);

        // Audit (referral row is now flushed — FK is safe)
        auditService.log(saved, referrer, "CREATED", null, ReferralStatus.PENDING, "Volunteer offered to refer");

        // Notification fires AFTER_COMMIT — never sent if transaction rolls back
        eventPublisher.publishEvent(ReferralLifecycleEvent.volunteerReceived(this, saved, referrer));

        log.info("Referral(REQUEST) created — postId={} referrer={} status=PENDING", postId, referrerId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // APPLY — Applicant on OFFER post
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Called when an applicant clicks "Apply" on an OFFER post.
     * Stores resume snapshot, github, linkedin on the referral record.
     * Sets status = PENDING. Does NOT open chat.
     */
    @Override
    @Transactional
    @Idempotent
    public void applyToOffer(UUID postId, UUID currentUserId, ReferralRequestDto.ApplyRequest request) {
        Post post = requireOpenPost(postId);

        if (post.getType() != Post.PostType.OFFER) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST,
                    "Use POST /referrals/{postId} to volunteer on REQUEST posts");
        }

        UUID requesterId = currentUserId;              // applicant = current user
        UUID referrerId  = post.getAuthor().getId();   // referrer = post author (OFFER)

        validateNotSelf(requesterId, referrerId);
        validateNoDuplicate(postId, requesterId, referrerId);

        // Limit check for OFFER posts
        long activeCount = referralRepository.countByPostIdAndStatusIn(
                postId, List.of(ReferralStatus.PENDING, ReferralStatus.ON_HOLD));
        if (activeCount >= post.getMaxApplicants()) {
            post.markFull();
            postRepository.save(post);
            throw new BusinessException(ErrorCodes.MAX_APPLICANTS_REACHED,
                    "This post has reached its maximum applicant limit (" + post.getMaxApplicants() + ")");
        }

        User applicant = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        // Resume snapshot — fails loudly if applicant has no resume
        String resumeKey = resumeSnapshotService.snapshotForUser(applicant);

        ReferralRequest referral = ReferralRequest.builder()
                .post(post)
                .requester(applicant)
                .referrer(userRepository.getReferenceById(referrerId))
                .status(ReferralStatus.PENDING)
                .resumeSnapshotKey(resumeKey)
                .applicantGithubLink(SanitizerUtils.sanitize(request.getGithubLink()))
                .applicantLinkedinLink(SanitizerUtils.sanitize(request.getLinkedinLink()))
                .content(SanitizerUtils.sanitize(request.getNote()))
                .expiresAt(LocalDateTime.now().plusDays(appProperties.getReferral().getExpiryDays()))
                .build();

        // saveAndFlush guarantees the referral INSERT is sent to the DB before the
        // audit log INSERT below — prevents the referral_audit_log FK violation.
        ReferralRequest saved = referralRepository.saveAndFlush(referral);

        postRepository.incrementReferralCount(postId);

        // Audit (referral row is now flushed — FK is safe)
        auditService.log(saved, applicant, "CREATED", null, ReferralStatus.PENDING, "Applicant applied to OFFER");

        // Notification fires AFTER_COMMIT — never sent if transaction rolls back
        eventPublisher.publishEvent(ReferralLifecycleEvent.applicantReceived(this, saved, applicant));

        log.info("Referral(OFFER) created — postId={} applicant={} status=PENDING", postId, requesterId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACCEPT — Post-type-aware authorization
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Accepts a PENDING referral. Authorization depends on post type:
     * <ul>
     *   <li>REQUEST → seeker (post author = requester) chooses a volunteer</li>
     *   <li>OFFER   → referrer (post author) picks an applicant</li>
     * </ul>
     * Only after this call does the chat get created.
     */
    @Override
    @Transactional
    public void acceptReferral(UUID referralId, UUID userId) {
        ReferralRequest r = referralRepository.findByIdWithDetails(referralId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REFERRAL_NOT_FOUND, "Referral not found"));

        requireAcceptableStatus(r);

        // ── Authorization: who can accept depends on post type ────────────
        boolean isRequestPost = r.getPost().getType() == Post.PostType.REQUEST;
        UUID    decisionMakerId = isRequestPost
                ? r.getRequester().getId()   // seeker picks the volunteer
                : r.getReferrer().getId();   // referrer (offer author) picks applicant

        if (!userId.equals(decisionMakerId)) {
            String role = isRequestPost ? "seeker" : "referrer";
            throw new BusinessException(ErrorCodes.UNAUTHORIZED,
                    "Only the " + role + " can accept this referral");
        }

        // ── Concurrency guard: one accepted per seeker+post pair ──────────
        if (referralRepository.existsByPostIdAndRequesterIdAndStatus(
                r.getPost().getId(), r.getRequester().getId(), ReferralStatus.ACCEPTED)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_ACCEPTED,
                    "An accepted referral already exists for this post");
        }

        ReferralStatus oldStatus = r.getStatus();
        r.setStatus(ReferralStatus.ACCEPTED);

        // Put all other PENDING volunteers/applicants ON_HOLD (not auto-rejected)
        int putOnHold = referralRepository.putOthersOnHold(
                r.getPost().getId(), r.getRequester().getId(), referralId);

        // For OFFER posts: mark the post FULFILLED after acceptance
        if (!isRequestPost) {
            Post post = r.getPost();
            post.markFulfilled();
            postRepository.save(post);
        }

        // ── Open chat — ONLY here, after ACCEPTED ─────────────────────────
        chatService.openChat(referralId, userId);

        User actor = userRepository.getReferenceById(userId);
        auditService.log(r, actor, "ACCEPTED", oldStatus, ReferralStatus.ACCEPTED,
                "Accepted; " + putOnHold + " others set to ON_HOLD");

        // Notification fires AFTER_COMMIT — never sent if transaction rolls back
        eventPublisher.publishEvent(ReferralLifecycleEvent.accepted(this, r));

        log.info("Referral accepted — id={} by={} type={} onHold={}",
                referralId, userId, r.getPost().getType(), putOnHold);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REJECT
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void rejectReferral(UUID referralId, UUID userId) {
        ReferralRequest r = referralRepository.findByIdWithDetails(referralId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REFERRAL_NOT_FOUND, "Referral not found"));

        requirePendingStatus(r, "rejected");

        // Same authorization pattern as accept
        boolean isRequestPost   = r.getPost().getType() == Post.PostType.REQUEST;
        UUID    decisionMakerId = isRequestPost
                ? r.getRequester().getId()
                : r.getReferrer().getId();

        if (!userId.equals(decisionMakerId)) {
            String role = isRequestPost ? "seeker" : "referrer";
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the " + role + " can reject this referral");
        }

        ReferralStatus oldStatus = r.getStatus();
        r.setStatus(ReferralStatus.REJECTED);

        User actor = userRepository.getReferenceById(userId);
        auditService.log(r, actor, "REJECTED", oldStatus, ReferralStatus.REJECTED, null);

        // Notification fires AFTER_COMMIT — never sent if transaction rolls back
        eventPublisher.publishEvent(ReferralLifecycleEvent.rejected(this, r));

        log.info("Referral rejected — id={} by={}", referralId, userId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WITHDRAW
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void withdrawReferral(UUID referralId, UUID userId) {
        ReferralRequest r = referralRepository.findByIdWithDetails(referralId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REFERRAL_NOT_FOUND, "Referral not found"));

        if (!r.getRequester().getId().equals(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the requester can withdraw");
        }
        if (r.getStatus() != ReferralStatus.PENDING && r.getStatus() != ReferralStatus.ON_HOLD) {
            throw new BusinessException(ErrorCodes.INVALID_STATE,
                    "Only PENDING or ON_HOLD referrals can be withdrawn (current: " + r.getStatus() + ")");
        }

        ReferralStatus oldStatus = r.getStatus();
        r.setStatus(ReferralStatus.WITHDRAWN);

        User actor = userRepository.getReferenceById(userId);
        auditService.log(r, actor, "WITHDRAWN", oldStatus, ReferralStatus.WITHDRAWN, null);

        // Notification fires AFTER_COMMIT — never sent if transaction rolls back
        eventPublisher.publishEvent(ReferralLifecycleEvent.withdrawn(this, r));

        log.info("Referral withdrawn — id={} requester={}", referralId, userId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<ReferralRequestDto.Response> getMyRequests(UUID userId, Pageable pageable) {
        return referralRepository.findByRequesterId(userId, pageable)
                .map(ReferralRequestDto.Response::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReferralRequestDto.Response> getIncomingRequests(UUID userId, Pageable pageable) {
        return referralRepository.findByReferrerId(userId, pageable)
                .map(ReferralRequestDto.Response::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReferralRequestDto.Response> getVolunteers(UUID postId, UUID seekerId, Pageable pageable) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        if (!post.isOwnedBy(seekerId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the post author can view volunteers");
        }

        // Show PENDING + ON_HOLD together so seeker sees all backup options
        return referralRepository.findByPostIdAndStatusIn(
                        postId, List.of(ReferralStatus.PENDING, ReferralStatus.ON_HOLD), pageable)
                .map(ReferralRequestDto.Response::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReferralRequestDto.Response> getApplicants(UUID postId, UUID referrerId, Pageable pageable) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        if (!post.isOwnedBy(referrerId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the post author can view applicants");
        }

        // FIFO — pageable is passed through, so caller can supply Sort for future ranking
        return referralRepository.findByPostIdAndStatusIn(
                        postId, List.of(ReferralStatus.PENDING, ReferralStatus.ON_HOLD), pageable)
                .map(ReferralRequestDto.Response::from);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private Post requireOpenPost(UUID postId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));
        if (!post.isAcceptingEntries()) {
            throw new BusinessException(ErrorCodes.POST_CLOSED,
                    "This post is no longer accepting entries (status: " + post.getStatus() + ")");
        }
        return post;
    }

    private void validateNotSelf(UUID requesterId, UUID referrerId) {
        if (requesterId.equals(referrerId)) {
            throw new BusinessException(ErrorCodes.SELF_REFERRAL, "You cannot act on your own post");
        }
    }

    private void validateNoDuplicate(UUID postId, UUID requesterId, UUID referrerId) {
        if (referralRepository.existsByPostIdAndRequesterIdAndReferrerId(postId, requesterId, referrerId)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_REQUEST,
                    "A referral request already exists for this post");
        }
    }

    private void requirePendingStatus(ReferralRequest r, String action) {
        if (r.getStatus() == ReferralStatus.EXPIRED) {
            throw new BusinessException(ErrorCodes.REFERRAL_EXPIRED, "This referral has expired");
        }
        if (r.getStatus() != ReferralStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_STATE,
                    "Only PENDING referrals can be " + action + " (current: " + r.getStatus() + ")");
        }
    }

    private void requireAcceptableStatus(ReferralRequest r) {
        if (r.getStatus() == ReferralStatus.ACCEPTED) {
            log.info("Referral already accepted — id={}", r.getId());
            return; // idempotent
        }
        requirePendingStatus(r, "accepted");
    }
}
