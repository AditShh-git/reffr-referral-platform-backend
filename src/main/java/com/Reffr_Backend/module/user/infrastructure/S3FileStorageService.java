package com.Reffr_Backend.module.user.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Profile("prod")
@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final S3Client    s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Override
    public String uploadResume(MultipartFile file, UUID userId) {
        String key = buildResumeKey(userId, file.getOriginalFilename());
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .serverSideEncryption(ServerSideEncryption.AES256)
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
            log.info("Resume uploaded — key={}", key);
            return key;
        } catch (IOException e) {
            log.error("S3 upload failed for user={}", userId, e);
            throw new RuntimeException("File upload failed. Please try again.", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(storageKey).build());
            log.info("Deleted S3 object — key={}", storageKey);
        } catch (Exception e) {
            log.error("S3 delete failed silently — key={}: {}", storageKey, e.getMessage(), e);
        }
    }

    @Override
    public String generateAccessUrl(String storageKey) {
        return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(1))
                        .getObjectRequest(r -> r.bucket(bucket).key(storageKey))
                        .build()
        ).url().toString();
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket).key(storageKey).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    private String buildResumeKey(UUID userId, String originalFilename) {
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase()
                : "pdf";
        return "resumes/" + userId + "/" + UUID.randomUUID() + "." + ext;
    }
}
