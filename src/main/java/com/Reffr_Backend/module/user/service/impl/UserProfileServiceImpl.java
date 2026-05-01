package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.common.constants.SecurityConstants;
import com.Reffr_Backend.common.exception.ConflictException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.user.domain.UserDomain;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;
import com.Reffr_Backend.module.user.service.ProfileViewService;
import com.Reffr_Backend.module.user.service.ResumeParsingService;
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
    private final ProfileViewService profileViewService;
    private final ResumeParsingService resumeParsingService;

    @Transactional(readOnly = true)
    public UserDto.ProfileResponse getMyProfile() {
        User user = userRepository.findByIdWithProfile(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));
        return UserDto.ProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = SecurityConstants.CACHE_PUBLIC_PROFILE, key = "#username")
    public UserDto.PublicProfileResponse getPublicProfile(String username, UUID viewerId) {
        User targetUser = findByUsername(username);

        if (viewerId != null) {
            try {
                User viewer = findById(viewerId);
                profileViewService.recordProfileView(viewerId, viewer.getName(), targetUser.getId());
            } catch (Exception e) {
                log.warn("Profile view tracking skipped viewer={} target={} reason={}",
                        viewerId, targetUser.getId(), e.getMessage());
            }
        }

        return UserDto.PublicProfileResponse.from(targetUser, null);
    }

    @Transactional
    @Deprecated
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = SecurityConstants.CACHE_PUBLIC_PROFILE, key = "#result.githubUsername"),
        @CacheEvict(value = "feed", key = "#result.id")
    })
    public UserDto.ProfileResponse updateProfile(UserDto.UpdateProfileRequest req) {
        User user = findById(SecurityUtils.getCurrentUserId());

        if (req.getName() != null) user.setName(req.getName());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getLocation() != null) user.setLocation(req.getLocation());
        if (req.getCurrentCompany() != null) user.setCurrentCompany(req.getCurrentCompany());
        if (req.getCurrentRole() != null) user.setCurrentRole(req.getCurrentRole());
        if (req.getYearsOfExperience() != null) user.setYearsOfExperience(req.getYearsOfExperience());

        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(user.getPrimaryEmail())) {
            if (userRepository.existsByPrimaryEmail(req.getEmail())) {
                throw new ConflictException(ErrorCodes.INVALID_EMAIL, "Email is already in use");
            }
            user.setPrimaryEmail(req.getEmail());
            user.setEmailVerified(false);
        }

        if (req.getSkills() != null) {
            UserDomain.replaceSkills(user, req.getSkills());
        }
        if (req.getExperiences() != null) {
            var commands = req.getExperiences().stream()
                    .map(e -> new UserDomain.ExperienceCommand(
                            e.getCompany(), e.getRole(),
                            e.getStartYear(), e.getEndYear(), e.isCurrent()))
                    .toList();
            UserDomain.appendExperiences(user, commands);
        }
        updateOnboardingState(user);
        return UserDto.ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = SecurityConstants.CACHE_PUBLIC_PROFILE, key = "#result.githubUsername"),
        @CacheEvict(value = "feed", key = "#result.id")
    })
    public UserDto.ProfileResponse updateEmail(UserDto.UpdateEmailRequest req) {
        User user = findById(SecurityUtils.getCurrentUserId());
        if (req.getEmail() != null) {
            String email = req.getEmail().trim().toLowerCase();
            if (email.equals(user.getPrimaryEmail())) {
                return UserDto.ProfileResponse.from(user);
            }
            if (userRepository.existsByPrimaryEmail(email)) {
                throw new ConflictException(ErrorCodes.INVALID_EMAIL, "Email is already in use");
            }
            user.setPrimaryEmail(email);
            user.setEmailVerified(false);
            log.info("User {} changed email to {} - primary email updated", user.getId(), email);
        }
        updateOnboardingState(user);
        return UserDto.ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = SecurityConstants.CACHE_PUBLIC_PROFILE, key = "#result.githubUsername"),
        @CacheEvict(value = "feed", key = "#result.id")
    })
    public UserDto.ProfileResponse updateProfileDetails(UserDto.UpdateProfileDetailsRequest req) {
        User user = findById(SecurityUtils.getCurrentUserId());
        if (req.getName() != null) user.setName(req.getName());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getLocation() != null) user.setLocation(req.getLocation());
        if (req.getCurrentCompany() != null) user.setCurrentCompany(req.getCurrentCompany());
        if (req.getCurrentRole() != null) user.setCurrentRole(req.getCurrentRole());
        if (req.getYearsOfExperience() != null) user.setYearsOfExperience(req.getYearsOfExperience());
        updateOnboardingState(user);
        return UserDto.ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = SecurityConstants.CACHE_PUBLIC_PROFILE, key = "#result.githubUsername"),
        @CacheEvict(value = "feed", key = "#result.id")
    })
    public UserDto.ProfileResponse updateSkills(UserDto.UpdateSkillsRequest req) {
        User user = findById(SecurityUtils.getCurrentUserId());
        if (req.getSkills() != null) {
            if (req.getSkills().isEmpty()) {
                user.getSkills().clear();
            } else {
                UserDomain.replaceSkills(user, req.getSkills());
            }
        }
        updateOnboardingState(user);
        return UserDto.ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = SecurityConstants.CACHE_PUBLIC_PROFILE, key = "#result.githubUsername"),
        @CacheEvict(value = "feed", key = "#result.id")
    })
    public UserDto.ProfileResponse appendExperience(UserDto.ExperienceRequest req) {
        User user = findById(SecurityUtils.getCurrentUserId());
        if (req != null) {
            var cmd = new UserDomain.ExperienceCommand(
                    req.getCompany(), req.getRole(),
                    req.getStartYear(), req.getEndYear(), req.isCurrent());
            UserDomain.appendExperiences(user, java.util.List.of(cmd));
        }
        return UserDto.ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void onboardUser(UserDto.OnboardingRequest req) {
        User user = findById(SecurityUtils.getCurrentUserId());

        if (!user.hasResume()) {
            log.warn("User {} onboarding without resume - feature restriction might apply later", user.getId());
        }

        userRepository.findByPrimaryEmail(req.getEmail())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(user.getId())) {
                        throw new ConflictException(ErrorCodes.INVALID_EMAIL, "Email is already in use by another account");
                    }
                });

        if (req.getName() != null) {
            user.setName(req.getName());
        }
        user.setPrimaryEmail(req.getEmail());
        user.setCurrentRole(req.getCurrentRole());

        if (req.getSkills() != null) {
            UserDomain.replaceSkills(user, req.getSkills());
        }

        user.setOnboardingCompleted(true);
        userRepository.save(user);
        log.info("Onboarding completed for user: {}", user.getId());
    }

    @Transactional(readOnly = true)
    public UserDto.OnboardingStatusResponse getOnboardingStatus() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = findById(userId);
        
        boolean isProfileComplete = user.getPrimaryEmail() != null && !user.getPrimaryEmail().isBlank()
                && user.getCurrentRole() != null && !user.getCurrentRole().isBlank()
                && user.getSkills() != null && !user.getSkills().isEmpty();
        
        return UserDto.OnboardingStatusResponse.builder()
                .profileCompleted(isProfileComplete)
                .resumeUploaded(user.hasResume())
                .skillsParsed(user.getSkills() != null && !user.getSkills().isEmpty())
                .build();
    }

    @Async
    @Transactional
    public void updateLastSeen(UUID userId) {
        userRepository.updateLastSeen(userId);
    }

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

    private void updateOnboardingState(User user) {
        boolean isComplete = user.getPrimaryEmail() != null && !user.getPrimaryEmail().isBlank()
                && user.getCurrentRole() != null && !user.getCurrentRole().isBlank()
                && user.getSkills() != null && !user.getSkills().isEmpty();
        user.setOnboardingCompleted(isComplete);
    }
}
