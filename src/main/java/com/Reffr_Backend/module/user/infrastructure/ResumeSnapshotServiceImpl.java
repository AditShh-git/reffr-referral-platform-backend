package com.Reffr_Backend.module.user.infrastructure;

import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeSnapshotServiceImpl implements ResumeSnapshotService {

    private final FileStorageService fileStorageService;

    @Override
    public String snapshotForUser(User user) {
        if (!StringUtils.hasText(user.getResumeS3Key())) {
            throw new BusinessException(ErrorCodes.RESUME_NOT_FOUND,
                    "Please upload your resume before proceeding");
        }
        // Snapshot = same key captured at this moment.
        // Even if the user replaces their resume later, the stored key
        // on the post/referral record remains unchanged.
        String key = user.getResumeS3Key();
        log.debug("Resume snapshot captured — userId={} key={}", user.getId(), key);
        return key;
    }

    @Override
    public String generatePresignedUrl(String snapshotKey) {
        if (!StringUtils.hasText(snapshotKey)) {
            return null;
        }
        try {
            return fileStorageService.generateAccessUrl(snapshotKey);
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL for key={}: {}", snapshotKey, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isValid(String snapshotKey) {
        if (!StringUtils.hasText(snapshotKey)) return false;
        try {
            return fileStorageService.exists(snapshotKey);
        } catch (Exception e) {
            log.warn("Existence check failed for key={}: {}", snapshotKey, e.getMessage());
            return false;
        }
    }
}
