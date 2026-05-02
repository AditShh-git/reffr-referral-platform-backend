package com.Reffr_Backend.module.user.infrastructure;

import com.Reffr_Backend.module.user.entity.User;

/**
 * Abstracts the resume snapshot workflow used in two places:
 * <ol>
 *   <li>REQUEST post creation — snapshot seeker's current resume at post time</li>
 *   <li>OFFER post apply — snapshot applicant's current resume at apply time</li>
 * </ol>
 *
 * <p>A "snapshot" is a copy of the user's current resume S3 key stored at a
 * point-in-time. It doesn't duplicate the file — it simply captures the key
 * reference so that later resume updates don't retroactively change historical records.
 *
 * <p>Centralising here prevents duplicate snapshot logic in PostServiceImpl,
 * ReferralServiceImpl, etc.
 */
public interface ResumeSnapshotService {

    /**
     * Returns the S3 key to store as a snapshot for the given user.
     * Validates that the user actually has a resume uploaded.
     * Throws {@link com.Reffr_Backend.common.exception.BusinessException} if no resume is present.
     *
     * @param user the user whose resume should be snapshotted
     * @return the S3 key string (same as user.resumeS3Key at this moment in time)
     */
    String snapshotForUser(User user);

    /**
     * Generate a short-lived presigned URL for the given snapshot key.
     * Returns null if the key is blank or null (graceful — callers must null-check).
     *
     * @param snapshotKey the stored S3 key
     * @return presigned URL valid for a limited time, or null
     */
    String generatePresignedUrl(String snapshotKey);

    /**
     * Validates that a given snapshot key points to an existing file.
     * Used before storing a key to prevent dangling references.
     */
    boolean isValid(String snapshotKey);
}
