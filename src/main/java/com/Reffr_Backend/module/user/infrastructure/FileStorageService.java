package com.Reffr_Backend.module.user.infrastructure;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Abstracts file storage — services never import S3 SDK directly.
 * Swap for local storage in tests, or GCS/Azure in future, with zero service changes.
 */
public interface FileStorageService {

    /**
     * Upload a resume file and return the storage key.
     * The key is opaque to callers — only this service knows the structure.
     */
    String uploadResume(MultipartFile file, UUID userId);

    /**
     * Delete a file by its storage key.
     * Fails silently — deletion errors should never block user-facing operations.
     */
    void delete(String storageKey);

    /**
     * Generate a time-limited URL to access a private file.
     * URL is valid for 1 hour by default.
     */
    String generateAccessUrl(String storageKey);

    /**
     * Check if a file exists at the given storage key.
     */
    boolean exists(String storageKey);
}
