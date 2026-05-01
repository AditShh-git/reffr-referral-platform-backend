package com.Reffr_Backend.module.user.service.resume;

public interface ResumeExtractionClient {

    /**
     * Returns structured JSON for the parsed resume payload.
     * This interface is intentionally AI-provider agnostic so the backend
     * can swap a real LLM adapter in without changing service logic.
     */
    String extractStructuredJson(String resumeText);
}
