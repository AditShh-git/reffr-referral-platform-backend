package com.Reffr_Backend.module.user.service;

import java.util.UUID;

public interface ResumeAccessService {

    String getResumeAccessUrl(UUID resumeId, UUID currentUserId);
}
