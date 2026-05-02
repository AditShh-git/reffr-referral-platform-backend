package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.ForbiddenException;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.entity.PostVisibility;
import com.Reffr_Backend.module.feed.repository.PostRepository;
import com.Reffr_Backend.module.referral.repository.ReferralRequestRepository;
import com.Reffr_Backend.module.user.infrastructure.ResumeSnapshotService;
import com.Reffr_Backend.module.user.service.ResumeAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeAccessServiceImpl implements ResumeAccessService {

    private final PostRepository postRepository;
    private final ReferralRequestRepository referralRequestRepository;
    private final ResumeSnapshotService resumeSnapshotService;

    @Override
    @Transactional(readOnly = true)
    public String getResumeAccessUrl(UUID resumeId, UUID currentUserId) {
        Post post = postRepository.findActiveById(resumeId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.RESUME_NOT_FOUND, "Resume not found"));

        if (post.getType() != Post.PostType.REQUEST || post.getResumeSnapshotKey() == null) {
            throw new NotFoundException(ErrorCodes.RESUME_NOT_FOUND, "Resume not found");
        }

        if (!canAccess(post, currentUserId)) {
            throw new ForbiddenException(ErrorCodes.ACCESS_DENIED, "You are not allowed to access this resume");
        }

        return resumeSnapshotService.generatePresignedUrl(post.getResumeSnapshotKey());
    }

    private boolean canAccess(Post post, UUID currentUserId) {
        if (post.getResumeVisibility() == PostVisibility.PUBLIC) {
            return true;
        }
        if (currentUserId == null) {
            return false;
        }
        if (post.isOwnedBy(currentUserId)) {
            return true;
        }
        return referralRequestRepository.existsAcceptedParticipant(post.getId(), currentUserId);
    }
}
