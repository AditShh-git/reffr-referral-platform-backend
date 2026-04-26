package com.Reffr_Backend.module.user.service;

import com.Reffr_Backend.module.user.dto.UserDto;
import java.util.UUID;

public interface ResumeParsingService {
    /**
     * Triggers asynchronous parsing of the resume.
     */
    void parseAndStore(UUID userId, String resumeKey, String originalFilename);

    /**
     * Retrieves parsed data for the user.
     */
    UserDto.ParsedResumeResponse getParsedData(UUID userId);

    /**
     * Clears any existing parsed data for the user.
     */
    void clearParsedData(UUID userId);
}
