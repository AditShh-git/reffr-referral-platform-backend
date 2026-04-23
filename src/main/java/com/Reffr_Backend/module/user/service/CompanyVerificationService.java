package com.Reffr_Backend.module.user.service;

import com.Reffr_Backend.module.user.dto.UserDto;
import java.util.List;
import java.util.UUID;

public interface CompanyVerificationService {
    void addExperience(UUID userId, UserDto.ExperienceRequest request);
    void sendVerificationEmail(UUID userId, String email);
    void verifyOtp(UUID userId, String otp);
    void addPublicProof(UUID userId, UserDto.PublicProofRequest request);
    void uploadDocument(UUID userId, UserDto.DocumentProofRequest request);
    List<UserDto.VerificationResponse> getUserVerifications(UUID userId);
}
