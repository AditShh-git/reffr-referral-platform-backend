package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.common.constants.SecurityConstants;
import com.Reffr_Backend.common.exception.ConflictException;
import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.user.domain.UserDomain;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;
import com.Reffr_Backend.module.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto.ProfileResponse getMyProfile() {
        User user = findById(SecurityUtils.getCurrentUserId());
        
        if (UserDomain.isInvalidPrimaryEmail(user.getPrimaryEmail())) {
            // Force re-onboarding if legacy noreply email is found
            user.setOnboardingCompleted(false);
            userRepository.save(user);
            
            throw new com.Reffr_Backend.common.exception.ForbiddenException(ErrorCodes.EMAIL_UPDATE_REQUIRED, 
                    "Your current email is invalid. Please update your email to continue.");
        }
        
        return UserDto.ProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = SecurityConstants.CACHE_PUBLIC_PROFILE, key = "#username")
    public UserDto.PublicProfileResponse getPublicProfile(String username) {
        // Resume URL is resolved by UserResumeService — not this service's concern
        return UserDto.PublicProfileResponse.from(findByUsername(username), null);
    }

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = SecurityConstants.CACHE_PUBLIC_PROFILE, key = "#result.githubUsername"),
        @CacheEvict(value = "feed", key = "#result.id")
    })
    public UserDto.ProfileResponse updateProfile(UserDto.UpdateProfileRequest req) {
        User user = findById(SecurityUtils.getCurrentUserId());

        if (req.getName()              != null) user.setName(req.getName());
        if (req.getBio()               != null) user.setBio(req.getBio());
        if (req.getLocation()          != null) user.setLocation(req.getLocation());
        if (req.getCurrentCompany()    != null) user.setCurrentCompany(req.getCurrentCompany());
        if (req.getCurrentRole()       != null) user.setCurrentRole(req.getCurrentRole());
        if (req.getYearsOfExperience() != null) user.setYearsOfExperience(req.getYearsOfExperience());

        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(user.getPrimaryEmail())) {
            // Email change: check uniqueness. NO LONGER resets verification.
            if (userRepository.existsByPrimaryEmail(req.getEmail())) {
                throw new ConflictException(ErrorCodes.INVALID_EMAIL, "Email is already in use");
            }
            user.setPrimaryEmail(req.getEmail());
            user.setEmailVerified(false);
            log.info("User {} changed email to {} — primary email updated", user.getId(), req.getEmail());
        }

        // Fix 3: Domain object owns mutation logic — not the service
        if (req.getSkills() != null) {
            UserDomain.replaceSkills(user, req.getSkills());
        }
        if (req.getExperiences() != null) {
            var commands = req.getExperiences().stream()
                    .map(e -> new UserDomain.ExperienceCommand(
                            e.getCompany(), e.getRole(),
                            e.getStartYear(), e.getEndYear(), e.isCurrent()))
                    .toList();
            UserDomain.replaceExperiences(user, commands);
        }

        return UserDto.ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void onboardUser(UserDto.OnboardingRequest req) {
        User user = findById(SecurityUtils.getCurrentUserId());

        if (user.isOnboardingCompleted()) {
            throw new ConflictException(ErrorCodes.ONBOARDING_REQUIRED, "User is already onboarded");
        }

        // Email uniqueness check
        if (userRepository.existsByPrimaryEmail(req.getEmail())) {
            throw new BusinessException(ErrorCodes.INVALID_EMAIL, "Email is already in use");
        }

        user.setCurrentRole(req.getCurrentRole());
        user.setPrimaryEmail(req.getEmail());
        user.setYearsOfExperience(req.getYearsOfExperience() != null ? req.getYearsOfExperience().shortValue() : 0);
        user.setResumeS3Key(req.getResumeUrl()); // store url here for now

        if (req.getSkills() != null) {
            UserDomain.replaceSkills(user, req.getSkills());
        }

        user.setOnboardingCompleted(true);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean getOnboardingStatus() {
        return findById(SecurityUtils.getCurrentUserId()).isOnboardingCompleted();
    }

    @Async
    @Transactional
    public void updateLastSeen(UUID userId) {
        userRepository.updateLastSeen(userId);
    }

    // ── Package-level helpers used by sibling services ────────────────

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found with id: " + id));
    }

    public User findByUsername(String username) {
        return userRepository.findByGithubUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found with username: " + username));
    }

    @Transactional
    public void deleteMyAccount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        userRepository.deleteById(userId);
    }
}