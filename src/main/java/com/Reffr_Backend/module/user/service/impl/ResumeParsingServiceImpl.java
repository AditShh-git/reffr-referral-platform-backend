package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.infrastructure.FileStorageService;
import com.Reffr_Backend.module.user.service.ResumeParsingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeParsingServiceImpl implements ResumeParsingService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final com.Reffr_Backend.module.user.repository.UserRepository userRepository;
    private final FileStorageService fileStorage;

    private static final String REDIS_KEY_PREFIX = "parsed_resume:";
    private static final String LOCK_PREFIX = "resume_parse_lock:";
    private static final long TTL_HOURS = 24;
    private static final long LOCK_TIMEOUT_SECONDS = 120; // Requirement 2: 2 minutes lock TTL

    private static final Map<String, List<String>> SKILL_PATTERNS = Map.ofEntries(
            Map.entry("java", List.of(" java ", "java,", "java.", "java\n")),
            Map.entry("spring boot", List.of("spring boot", "springboot")),
            Map.entry("mysql", List.of("mysql")),
            Map.entry("sql", List.of(" sql ", "sql,", "sql.")),
            Map.entry("postgresql", List.of("postgresql", "postgres")),
            Map.entry("redis", List.of("redis")),
            Map.entry("docker", List.of("docker")),
            Map.entry("kubernetes", List.of("kubernetes", "k8s")),
            Map.entry("aws", List.of(" aws ", "amazon web services")),
            Map.entry("node.js", List.of("node.js", "nodejs")),
            Map.entry("python", List.of("python")),
            Map.entry("react", List.of("react")),
            Map.entry("javascript", List.of("javascript")),
            Map.entry("typescript", List.of("typescript")),
            Map.entry("go", List.of(" golang ", " go ")),
            Map.entry("rust", List.of("rust")),
            Map.entry("c++", List.of("c++"))
    );
    private static final LinkedHashMap<String, List<String>> ROLE_PATTERNS = new LinkedHashMap<>();

    static {
        ROLE_PATTERNS.put("backend developer", List.of("backend developer", "backend engineer", "java spring boot developer"));
        ROLE_PATTERNS.put("software engineer", List.of("software engineer", "software developer"));
        ROLE_PATTERNS.put("full stack developer", List.of("full stack developer", "fullstack developer", "full stack engineer"));
        ROLE_PATTERNS.put("frontend developer", List.of("frontend developer", "front end developer", "frontend engineer"));
        ROLE_PATTERNS.put("data engineer", List.of("data engineer"));
        ROLE_PATTERNS.put("devops engineer", List.of("devops engineer", "site reliability engineer", "sre"));
    }

    @Async("resumeParsingExecutor") // Requirement 5: Bounded thread pool
    @Override
    public void parseAndStore(UUID userId, String resumeKey, String originalFilename) {
        long startTime = System.currentTimeMillis();
        String lockKey = LOCK_PREFIX + userId;
        String dataKey = REDIS_KEY_PREFIX + userId;

        // Requirement 1: Lock release safety ensured by try-finally
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (acquired == null || !acquired) {
            log.warn("Parsing already in progress for user: {}", userId);
            return;
        }

        try {
            // Requirement 1: Mark as PENDING
            UserDto.ParsedResumeResponse pendingResponse = UserDto.ParsedResumeResponse.builder()
                    .status(UserDto.ParsedResumeResponse.ParsingStatus.PENDING)
                    .resumeKey(resumeKey)
                    .build();
            redisTemplate.opsForValue().set(dataKey, objectMapper.writeValueAsString(pendingResponse), TTL_HOURS, TimeUnit.HOURS);

            log.info("Starting resume parsing for user: {} (key: {})", userId, resumeKey);
            
            Thread.sleep(800);

            // Requirement 4: Resume key mismatch check (Version control)
            var userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || !resumeKey.equals(userOpt.get().getResumeS3Key())) {
                log.warn("Discarding parsing result for user {}: Resume key mismatch (current: {}, parsed: {})", 
                        userId, userOpt.map(User::getResumeS3Key).orElse("NONE"), resumeKey);
                return;
            }

            String resumeText = extractResumeText(resumeKey, originalFilename);
            String normalizedText = normalize(" " + originalFilename + " " + resumeText + " ");

            List<String> extractedSkills = extractSkills(normalizedText);
            String suggestedRole = extractRole(normalizedText);
            UserDto.ParsedResumeResponse.ConfidenceLevel confidence =
                    calculateConfidence(extractedSkills, suggestedRole, normalizedText);

            if (extractedSkills.isEmpty() && suggestedRole == null) {
                throw new IllegalStateException("Could not extract meaningful onboarding suggestions from resume");
            }

            UserDto.ParsedResumeResponse parsedData = UserDto.ParsedResumeResponse.builder()
                    .status(UserDto.ParsedResumeResponse.ParsingStatus.READY)
                    .skills(extractedSkills)
                    .role(suggestedRole)
                    .confidence(confidence)
                    .resumeKey(resumeKey)
                    .parsedAt(java.time.Instant.now()) // Requirement 7
                    .build();

            redisTemplate.opsForValue().set(dataKey, objectMapper.writeValueAsString(parsedData), TTL_HOURS, TimeUnit.HOURS);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully parsed resume for user: {} in {}ms", userId, duration); // Requirement 6

        } catch (Exception e) {
            log.error("Failed to parse resume for user: {}", userId, e);
            try {
                // Requirement 3: Error visibility
                UserDto.ParsedResumeResponse failedResponse = UserDto.ParsedResumeResponse.builder()
                        .status(UserDto.ParsedResumeResponse.ParsingStatus.FAILED)
                        .error(e.getMessage() != null ? e.getMessage() : "Unknown parsing error")
                        .resumeKey(resumeKey)
                        .build();
                redisTemplate.opsForValue().set(dataKey, objectMapper.writeValueAsString(failedResponse), 1, TimeUnit.HOURS);
            } catch (Exception ignored) {}
        } finally {
            redisTemplate.delete(lockKey); // Requirement 1: Release lock
        }
    }

    @Override
    public UserDto.ParsedResumeResponse getParsedData(UUID userId) {
        String json = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + userId);
        if (json == null) {
            return UserDto.ParsedResumeResponse.builder()
                    .status(UserDto.ParsedResumeResponse.ParsingStatus.NOT_FOUND)
                    .build();
        }
        try {
            return objectMapper.readValue(json, UserDto.ParsedResumeResponse.class);
        } catch (Exception e) {
            log.error("Failed to read parsed resume data for user: {}", userId, e);
            return UserDto.ParsedResumeResponse.builder()
                    .status(UserDto.ParsedResumeResponse.ParsingStatus.FAILED)
                    .confidence(UserDto.ParsedResumeResponse.ConfidenceLevel.LOW)
                    .build();
        }
    }

    @Override
    public void clearParsedData(UUID userId) {
        redisTemplate.delete(REDIS_KEY_PREFIX + userId);
        log.info("Cleared parsed resume data for user: {}", userId);
    }

    private String extractResumeText(String resumeKey, String originalFilename) {
        byte[] bytes = fileStorage.read(resumeKey);

        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Uploaded resume is empty");
        }

        String lowerName = originalFilename != null ? originalFilename.toLowerCase(Locale.ROOT) : "";
        if (lowerName.endsWith(".docx")) {
            return extractDocxText(bytes);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extractDocxText(byte[] bytes) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    return xml.replaceAll("<[^>]+>", " ");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse DOCX resume", e);
        }
        throw new IllegalStateException("DOCX resume content not found");
    }

    private List<String> extractSkills(String normalizedText) {
        return SKILL_PATTERNS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(normalizedText::contains))
                .map(Map.Entry::getKey)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

    private String extractRole(String normalizedText) {
        for (Map.Entry<String, List<String>> entry : ROLE_PATTERNS.entrySet()) {
            boolean matched = entry.getValue().stream().anyMatch(normalizedText::contains);
            if (matched) {
                return entry.getKey();
            }
        }

        Matcher matcher = Pattern.compile("\\b([a-z]+(?:\\s+[a-z]+){0,2})\\s+(developer|engineer)\\b").matcher(normalizedText);
        if (matcher.find()) {
            return (matcher.group(1) + " " + matcher.group(2)).trim();
        }
        return null;
    }

    private UserDto.ParsedResumeResponse.ConfidenceLevel calculateConfidence(
            List<String> skills,
            String role,
            String normalizedText) {

        int score = 0;
        score += Math.min(skills.size(), 4);
        if (role != null) {
            score += 2;
        }
        if (normalizedText.contains("experience")) {
            score += 1;
        }

        if (score >= 5) {
            return UserDto.ParsedResumeResponse.ConfidenceLevel.HIGH;
        }
        if (score >= 3) {
            return UserDto.ParsedResumeResponse.ConfidenceLevel.MEDIUM;
        }
        return UserDto.ParsedResumeResponse.ConfidenceLevel.LOW;
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return (" " + input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#.]+", " ").replaceAll("\\s+", " ").trim() + " ");
    }
}
