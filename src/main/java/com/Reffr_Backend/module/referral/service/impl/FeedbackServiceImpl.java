package com.Reffr_Backend.module.referral.service.impl;

import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.module.chat.entity.ChatWorkflowStatus;
import com.Reffr_Backend.module.chat.repository.ChatRepository;
import com.Reffr_Backend.module.referral.dto.FeedbackDto;
import com.Reffr_Backend.module.referral.entity.ReferralFeedback;
import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.repository.ReferralFeedbackRepository;
import com.Reffr_Backend.module.referral.repository.ReferralRequestRepository;
import com.Reffr_Backend.module.referral.service.FeedbackService;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final ReferralRequestRepository referralRepository;
    private final ReferralFeedbackRepository feedbackRepository;
    private final ChatRepository            chatRepository;
    private final UserRepository            userRepository;

    @Override
    @Transactional
    public FeedbackDto.Response submitFeedback(UUID referralId, UUID seekerId, FeedbackDto.Request request) {

        ReferralRequest referral = referralRepository.findByIdWithDetails(referralId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REFERRAL_NOT_FOUND, "Referral not found"));

        // Gate 1: only the seeker (requester) may give feedback
        if (!referral.getRequester().getId().equals(seekerId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED,
                    "Only the seeker who applied can submit feedback");
        }

        // Gate 2: referral must have reached REFERRED stage (checked via Chat)
        chatRepository.findByReferralId(referralId).ifPresentOrElse(chat -> {
            if (chat.getWorkflowStatus() != ChatWorkflowStatus.REFERRED) {
                throw new BusinessException(ErrorCodes.FEEDBACK_NOT_ALLOWED,
                        "Feedback is only allowed after the referral has been submitted (REFERRED stage)");
            }
        }, () -> {
            throw new BusinessException(ErrorCodes.FEEDBACK_NOT_ALLOWED,
                    "No active chat found for this referral");
        });

        // Gate 3: one feedback per referral
        if (feedbackRepository.existsByReferralId(referralId)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_FEEDBACK,
                    "Feedback has already been submitted for this referral");
        }

        User referrer = referral.getReferrer();

        ReferralFeedback feedback = ReferralFeedback.builder()
                .referral(referral)
                .reviewer(userRepository.getReferenceById(seekerId))
                .referrer(referrer)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        feedbackRepository.save(feedback);

        // Update referrer's reputation counters atomically
        if (request.getRating() == ReferralFeedback.FeedbackRating.UP) {
            referrer.applyPositiveFeedback();
        } else {
            referrer.applyNegativeFeedback();
        }
        userRepository.save(referrer);

        log.info("Feedback submitted — referralId={} rating={} referrer={}",
                referralId, request.getRating(), referrer.getId());

        return FeedbackDto.Response.from(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackDto.ReputationSummary getReputation(UUID referrerId) {
        if (!userRepository.existsById(referrerId)) {
            throw new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found");
        }
        long total    = feedbackRepository.countAll(referrerId);
        long positive = feedbackRepository.countPositive(referrerId);
        long negative = feedbackRepository.countNegative(referrerId);
        return FeedbackDto.ReputationSummary.from(total, positive, negative);
    }
}
