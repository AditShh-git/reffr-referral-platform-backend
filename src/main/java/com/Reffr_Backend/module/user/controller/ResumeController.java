package com.Reffr_Backend.module.user.controller;

import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.user.service.ResumeAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeAccessService resumeAccessService;

    @GetMapping("/{resumeId}")
    public ResponseEntity<Void> openResume(@PathVariable UUID resumeId) {
        String accessUrl = resumeAccessService.getResumeAccessUrl(resumeId, tryGetCurrentUserId());
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, URI.create(accessUrl).toString())
                .build();
    }

    private UUID tryGetCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception ex) {
            return null;
        }
    }
}
