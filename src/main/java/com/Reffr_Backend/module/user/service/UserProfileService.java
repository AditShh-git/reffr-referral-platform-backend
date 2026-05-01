package com.Reffr_Backend.module.user.service;

import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.User;

import java.util.UUID;

public interface UserProfileService {

    UserDto.ProfileResponse getMyProfile();

    UserDto.PublicProfileResponse getPublicProfile(String username, UUID viewerId);

    @Deprecated
    UserDto.ProfileResponse updateProfile(UserDto.UpdateProfileRequest req);

    UserDto.ProfileResponse updateEmail(UserDto.UpdateEmailRequest req);

    UserDto.ProfileResponse updateProfileDetails(UserDto.UpdateProfileDetailsRequest req);

    UserDto.ProfileResponse updateSkills(UserDto.UpdateSkillsRequest req);

    UserDto.ProfileResponse appendExperience(UserDto.ExperienceRequest req);

    void onboardUser(UserDto.OnboardingRequest req);

    UserDto.OnboardingStatusResponse getOnboardingStatus();

    void updateLastSeen(UUID userId);

    // internal helpers (optional exposure)
    User findById(UUID id);

    User findByUsername(String username);

    void deleteMyAccount();
}
