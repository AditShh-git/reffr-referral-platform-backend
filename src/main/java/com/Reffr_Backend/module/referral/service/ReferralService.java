package com.Reffr_Backend.module.referral.service;

import com.Reffr_Backend.module.referral.dto.ReferralRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReferralService {

    void createReferral(UUID postId, UUID userId, String message);

    void acceptReferral(UUID referralId, UUID userId);

    void rejectReferral(UUID referralId, UUID userId);

    Page<ReferralRequestDto.Response> getMyRequests(UUID userId, Pageable pageable);

    Page<ReferralRequestDto.Response> getIncomingRequests(UUID userId, Pageable pageable);

    void withdrawReferral(UUID referralId, UUID userId);
}
