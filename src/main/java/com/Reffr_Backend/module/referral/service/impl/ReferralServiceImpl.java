package com.Reffr_Backend.module.referral.service.impl;

import com.Reffr_Backend.common.security.annotation.Idempotent;
import com.Reffr_Backend.common.util.SanitizerUtils;
import com.Reffr_Backend.common.util.NotificationMessages;
import com.Reffr_Backend.module.chat.service.ChatService;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import com.Reffr_Backend.module.notification.service.NotificationService;
import com.Reffr_Backend.module.referral.entity.ReferralStatus;
import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.repository.PostRepository;
import com.Reffr_Backend.module.referral.dto.ReferralRequestDto;
import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.repository.ReferralRequestRepository;
import com.Reffr_Backend.module.referral.service.ReferralService;
import com.Reffr_Backend.module.notification.service.EmailService;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    private final ReferralRequestRepository referralRepository;
    private final PostRepository            postRepository;
    private final UserRepository            userRepository;
    private final ChatService               chatService;
    private final NotificationService       notificationService;
    private final EmailService              emailService;

    // ── Create ────────────────────────────────────────────────────────

    @Override
    @Transactional
    @Idempotent
    public void createReferral(UUID postId, UUID currentUserId, String message) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        String cleanMessage = SanitizerUtils.sanitize(message);

        UUID requesterId;
        UUID referrerId;

        if (post.getType() == Post.PostType.OFFER) {
            requesterId = currentUserId;
            referrerId  = post.getAuthor().getId();
        } else {
            requesterId = post.getAuthor().getId();
            referrerId  = currentUserId;
        }

        if (requesterId.equals(referrerId)) {
            throw new BusinessException(ErrorCodes.SELF_REFERRAL, "You cannot act on your own post");
        }
        if (referralRepository.existsByPostIdAndRequesterIdAndReferrerId(
                postId, requesterId, referrerId)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_REQUEST,
                    "A referral request already exists for this post");
        }

        ReferralRequest referral = ReferralRequest.builder()
                .post(post)
                .requester(userRepository.getReferenceById(requesterId))
                .referrer(userRepository.getReferenceById(referrerId))
                .status(ReferralStatus.PENDING)
                .content(cleanMessage)
                .build();

        ReferralRequest saved = referralRepository.save(referral);
        log.info("Referral created — post={} requester={} referrer={}",
                postId, requesterId, referrerId);

        notificationService.send(
                referrerId,
                NotificationType.REFERRAL_REQUEST_RECEIVED,
                NotificationMessages.referralRequestTitle(),
                NotificationMessages.referralRequestBody(post.getCurrentRole(), post.getCompany()),
                "REFERRAL",
                saved.getId().toString()
        );
    }

    // ── Accept ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void acceptReferral(UUID referralId, UUID userId) {
        ReferralRequest r = referralRepository.findByIdWithDetails(referralId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REFERRAL_NOT_FOUND, "Referral not found"));

        if (!r.getReferrer().getId().equals(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the referrer can accept this request");
        }
        if (r.getStatus() != ReferralStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_STATE,
                    "Only PENDING referrals can be accepted (current: " + r.getStatus() + ")");
        }

        // Race condition: ensure only one accepted referral per seeker/post
        if (referralRepository.existsByPostIdAndRequesterIdAndStatus(r.getPost().getId(), r.getRequester().getId(), ReferralStatus.ACCEPTED)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_ACCEPTED, "An accepted referral already exists for this post");
        }

        r.setStatus(ReferralStatus.ACCEPTED);
        chatService.openChat(referralId, userId);
        log.info("Referral accepted — id={} referrer={}", referralId, userId);

        notificationService.send(
                r.getRequester().getId(),
                NotificationType.REFERRAL_ACCEPTED,
                NotificationMessages.referralAcceptedTitle(),
                NotificationMessages.referralAcceptedBody(r.getReferrer().getName()),
                "REFERRAL",
                referralId.toString()
        );

        if (r.getRequester().getPrimaryEmail() != null) {
            emailService.sendReferralAccepted(r.getRequester(), r.getReferrer().getName(), r.getId());
        }
    }

    // ── Reject ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void rejectReferral(UUID referralId, UUID userId) {
        ReferralRequest r = referralRepository.findByIdWithDetails(referralId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REFERRAL_NOT_FOUND, "Referral not found"));

        if (!r.getReferrer().getId().equals(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the referrer can reject this request");
        }
        if (r.getStatus() != ReferralStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_STATE,
                    "Only PENDING referrals can be rejected (current: " + r.getStatus() + ")");
        }

        r.setStatus(ReferralStatus.REJECTED);
        log.info("Referral rejected — id={} referrer={}", referralId, userId);

        notificationService.send(
                r.getRequester().getId(),
                NotificationType.REFERRAL_REJECTED,
                NotificationMessages.referralRejectedTitle(),
                NotificationMessages.referralRejectedBody(r.getReferrer().getName()),
                "REFERRAL",
                referralId.toString()
        );

        if (r.getRequester().getPrimaryEmail() != null) {
            emailService.sendReferralRejected(r.getRequester(), r.getReferrer().getName());
        }
    }

    // ── Withdraw ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public void withdrawReferral(UUID referralId, UUID userId) {
        ReferralRequest r = referralRepository.findByIdWithDetails(referralId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REFERRAL_NOT_FOUND, "Referral not found"));

        if (!r.getRequester().getId().equals(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the requester can withdraw");
        }
        if (r.getStatus() != ReferralStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_STATE,
                    "Only PENDING referrals can be withdrawn (current: " + r.getStatus() + ")");
        }

        r.setStatus(ReferralStatus.WITHDRAWN);
        log.info("Referral withdrawn — id={} requester={}", referralId, userId);

        notificationService.send(
                r.getReferrer().getId(),
                NotificationType.REFERRAL_WITHDRAWN,
                NotificationMessages.referralWithdrawnTitle(),
                NotificationMessages.referralWithdrawnBody(r.getRequester().getName()),
                "REFERRAL",
                referralId.toString()
        );
    }

    // ── Read ──────────────────────────────────────────────────────────

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

}