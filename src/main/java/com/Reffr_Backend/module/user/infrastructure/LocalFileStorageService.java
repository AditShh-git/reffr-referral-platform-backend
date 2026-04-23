package com.Reffr_Backend.module.user.infrastructure;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("dev")
@Service
public class LocalFileStorageService implements FileStorageService {

    private final String uploadDir = "uploads/";

    @Override
    public String uploadResume(MultipartFile file, UUID userId) {
        try {
            String filename = userId + "_" + file.getOriginalFilename().replaceAll("\\s+", "_");
            Path path = Paths.get(uploadDir + filename);

            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully: {} for user: {}", filename, userId);

            return filename;
        } catch (IOException e) {
            log.error("File upload failed for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path path = Paths.get(uploadDir + key);
            boolean deleted = Files.deleteIfExists(path);

            if (deleted) {
                log.info("File deleted successfully: {}", key);
            } else {
                log.warn("File not found for deletion: {}", key);
            }

        } catch (IOException e) {
            log.error("File delete failed for key {}: {}", key, e.getMessage(), e);
            throw new RuntimeException("File delete failed", e);
        }
    }

    @Override
    public String generateAccessUrl(String key) {
        String url = "http://localhost:8080/uploads/" + key;
        log.debug("Generated access URL: {}", url);
        return url;
    }

    @Override
    public boolean exists(String storageKey) {
        Path path = Paths.get(uploadDir + storageKey);
        boolean exists = Files.exists(path);

        log.debug("File exists check: {} -> {}", storageKey, exists);

        return exists;
    }
}