package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.ForbiddenException;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.exception.UnauthorizedException;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.user.domain.UserDomain;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.infrastructure.FileStorageService;
import com.Reffr_Backend.module.user.repository.UserRepository;
import com.Reffr_Backend.module.user.service.UserProfileService;
import com.Reffr_Backend.module.user.service.UserResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserResumeServiceImpl implements UserResumeService {

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB

    private final UserRepository userRepository;
    private final FileStorageService fileStorage;     // Fix 2: interface, not S3Service
    private final UserProfileService userProfileService;
    private final com.Reffr_Backend.module.user.service.ResumeParsingService resumeParsingService;

    @Transactional
    @CacheEvict(value = "publicProfile", key = "#result.githubUsername")
    public UserDto.ProfileResponse upload(MultipartFile file) {
        validate(file);

        User user = userProfileService.findById(SecurityUtils.getCurrentUserId());

        // Delete old resume first — domain doesn't know about storage
        if (user.hasResume()) {
            fileStorage.delete(user.getResumeS3Key());
        }

        String key = fileStorage.uploadResume(file, user.getId());

        // Fix 3: domain owns mutation
        UserDomain.attachResume(user, key, file.getOriginalFilename());

        // Trigger asynchronous parsing
        resumeParsingService.clearParsedData(user.getId());
        resumeParsingService.parseAndStore(user.getId(), key, file.getOriginalFilename());

        return UserDto.ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    @CacheEvict(value = "publicProfile", allEntries = true)
    public void delete() {
        User user = userProfileService.findById(SecurityUtils.getCurrentUserId());

        if (!user.hasResume()) {
            throw new NotFoundException(ErrorCodes.RESUME_NOT_FOUND, "No resume found");
        }

        fileStorage.delete(user.getResumeS3Key());
        UserDomain.detachResume(user);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String generateAccessUrl(String username) {
        User targetUser = userProfileService.findByUsername(username);

        if (!targetUser.hasResume()) {
            throw new NotFoundException(ErrorCodes.RESUME_NOT_FOUND, "No resume uploaded");
        }

        validateResumeAccess(targetUser, SecurityUtils.getCurrentUserId());

        return fileStorage.generateAccessUrl(targetUser.getResumeS3Key());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null ||
                !(filename.endsWith(".pdf") || filename.endsWith(".doc") || filename.endsWith(".docx"))) {
            throw new IllegalArgumentException("Invalid file type");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 5 MB limit");
        }
    }

    private void validateResumeAccess(User targetUser, UUID currentUserId) {
        if (currentUserId == null) {
            throw new UnauthorizedException(ErrorCodes.AUTH_REQUIRED, "User not authenticated");
        }
        // Temporary: Allow all authenticated users to access resume
    }
}
