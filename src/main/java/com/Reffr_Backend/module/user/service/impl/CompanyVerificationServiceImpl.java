package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.ProofType;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.entity.UserCompanyVerification;
import com.Reffr_Backend.module.user.entity.VerificationStatus;
import com.Reffr_Backend.module.user.repository.UserCompanyVerificationRepository;
import com.Reffr_Backend.module.user.repository.UserRepository;
import com.Reffr_Backend.module.user.service.CompanyVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyVerificationServiceImpl implements CompanyVerificationService {

    private final UserRepository userRepository;
    private final UserCompanyVerificationRepository verificationRepository;
    private final StringRedisTemplate redisTemplate;
    
    private static final String OTP_PREFIX = "company_verify:";
    private static final String EMAIL_PENDING_PREFIX = "company_email_pending:";
    private static final String OTP_RETRY_PREFIX = "company_verify_retry:";
    private static final long OTP_TTL_MINUTES = 15;
    private static final int MAX_RETRIES = 5;

    @Override
    @Transactional
    public void addExperience(UUID userId, UserDto.ExperienceRequest request) {
        User user = findUser(userId);
        
        if (request.isCurrent()) {
            handleCompanySwitch(userId);
        }

        UserCompanyVerification verification = UserCompanyVerification.builder()
                .user(user)
                .company(request.getCompany())
                .role(request.getRole())
                .startYear(request.getStartYear())
                .endYear(request.getEndYear())
                .current(request.isCurrent())
                .verificationStatus(request.isCurrent() ? VerificationStatus.CURRENT : VerificationStatus.PAST)
                .verified(false)
                .build();

        verificationRepository.save(verification);
        log.info("Experience added for user {} at {}", userId, request.getCompany());
    }

    @Override
    @Transactional
    public void sendVerificationEmail(UUID userId, String email) {
        if (email == null || !email.contains("@")) {
            throw new BusinessException(ErrorCodes.INVALID_EMAIL, "Invalid email format");
        }

        findUser(userId); // Ensure user exists

        String otp = String.format("%06d", new SecureRandom().nextInt(999999));
        
        redisTemplate.opsForValue().set(OTP_PREFIX + userId, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(EMAIL_PENDING_PREFIX + userId, email, OTP_TTL_MINUTES, TimeUnit.MINUTES);

        log.info("MOCK EMAIL SENT to {}: Your OTP is {}", email, otp);
    }

    @Override
    @Transactional
    public void verifyOtp(UUID userId, String otp) {
        String savedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + userId);
        String pendingEmail = redisTemplate.opsForValue().get(EMAIL_PENDING_PREFIX + userId);

        if (savedOtp == null || pendingEmail == null) {
            throw new BusinessException(ErrorCodes.OTP_EXPIRED, "OTP expired or not found");
        }

        if (!savedOtp.equals(otp)) {
            handleFailedRetry(userId);
            throw new BusinessException(ErrorCodes.INVALID_OTP, "Invalid OTP");
        }

        User user = findUser(userId);
        
        // Rule: On email verify, mark as CURRENT and switch old ones to PAST
        handleCompanySwitch(userId);

        String domain = pendingEmail.substring(pendingEmail.indexOf("@") + 1);
        String companyName = domain.contains(".") ? domain.substring(0, domain.lastIndexOf(".")) : domain;

        UserCompanyVerification verification = UserCompanyVerification.builder()
                .user(user)
                .company(companyName.toUpperCase())
                .role(user.getCurrentRole() != null ? user.getCurrentRole() : "Member")
                .current(true)
                .verificationStatus(VerificationStatus.CURRENT)
                .verified(true)
                .proofType(ProofType.EMAIL)
                .email(pendingEmail)
                .verifiedAt(Instant.now())
                .build();

        verificationRepository.save(verification);

        redisTemplate.delete(List.of(OTP_PREFIX + userId, EMAIL_PENDING_PREFIX + userId, OTP_RETRY_PREFIX + userId));
        log.info("Company verified via email for user {}", userId);
    }

    @Override
    @Transactional
    public void addPublicProof(UUID userId, UserDto.PublicProofRequest request) {
        User user = findUser(userId);
        
        UserCompanyVerification verification = UserCompanyVerification.builder()
                .user(user)
                .company(request.getCompany())
                .role(user.getCurrentRole() != null ? user.getCurrentRole() : "Member")
                .current(false) // Public proofs are usually for history/profile trust
                .verificationStatus(VerificationStatus.UNVERIFIED) // Needs manual/automated check
                .verified(false) 
                .proofType(ProofType.PUBLIC)
                .profileUrl(request.getProfileUrl())
                .build();

        verificationRepository.save(verification);
    }

    @Override
    @Transactional
    public void uploadDocument(UUID userId, UserDto.DocumentProofRequest request) {
        User user = findUser(userId);
        
        UserCompanyVerification verification = UserCompanyVerification.builder()
                .user(user)
                .company(request.getCompany())
                .role(user.getCurrentRole() != null ? user.getCurrentRole() : "Member")
                .current(false)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .verified(false)
                .proofType(ProofType.DOCUMENT)
                .documentKey(request.getDocumentKey())
                .build();

        verificationRepository.save(verification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto.VerificationResponse> getUserVerifications(UUID userId) {
        return verificationRepository.findByUserId(userId).stream()
                .map(UserDto.VerificationResponse::from)
                .toList();
    }

    // ── Helpers ──

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));
    }

    private void handleCompanySwitch(UUID userId) {
        verificationRepository.findByUserIdAndVerificationStatus(userId, VerificationStatus.CURRENT)
                .ifPresent(v -> {
                    v.setVerificationStatus(VerificationStatus.PAST);
                    v.setCurrent(false);
                    verificationRepository.save(v);
                    log.info("Moved old CURRENT company {} to PAST for user {}", v.getCompany(), userId);
                });
    }

    private void handleFailedRetry(UUID userId) {
        String retryKey = OTP_RETRY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(retryKey);
        if (count != null && count >= MAX_RETRIES) {
            throw new BusinessException(ErrorCodes.OTP_LIMIT_EXCEEDED, "Too many failed attempts");
        }
    }
}
