package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.module.user.domain.UserDomain;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.infrastructure.FileStorageService;
import com.Reffr_Backend.module.user.service.ResumeParsingService;
import com.Reffr_Backend.module.user.service.resume.ResumeExtractionClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeParsingServiceImpl implements ResumeParsingService {

    private record SkillDefinition(String canonicalName, String category, List<String> aliases) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiParsedResumePayload(String role, List<AiParsedSkill> skills, String confidence) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiParsedSkill(String name, String category) {}

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final com.Reffr_Backend.module.user.repository.UserRepository userRepository;
    private final FileStorageService fileStorage;
    private final ResumeExtractionClient resumeExtractionClient;

    private static final String REDIS_KEY_PREFIX = "parsed_resume:";
    private static final String LOCK_PREFIX = "resume_parse_lock:";
    private static final long TTL_HOURS = 24;
    private static final long LOCK_TIMEOUT_SECONDS = 120;
    private static final int CACHE_VERSION = 2;
    private static final int MAX_SKILLS = 15;

    private static final List<SkillDefinition> SKILL_DEFINITIONS = List.of(
            new SkillDefinition("Java", "backend", List.of("java", "java 8", "java 11", "java 17", "java 21")),
            new SkillDefinition("Python", "backend", List.of("python")),
            new SkillDefinition("Spring Boot", "backend", List.of("spring boot", "springboot")),
            new SkillDefinition("Spring MVC", "backend", List.of("spring mvc")),
            new SkillDefinition("JPA", "backend", List.of("jpa", "spring data jpa", "jakarta persistence", "jpa specification")),
            new SkillDefinition("Hibernate", "backend", List.of("hibernate")),
            new SkillDefinition("REST APIs", "backend", List.of("rest api", "rest apis", "restful api", "restful apis", "rest")),
            new SkillDefinition("Pagination", "backend", List.of("pagination", "pageable")),
            new SkillDefinition("JPQL", "backend", List.of("jpql")),
            new SkillDefinition("Native SQL", "backend", List.of("native sql")),
            new SkillDefinition("Spring Security", "security", List.of("spring security")),
            new SkillDefinition("JWT", "security", List.of("jwt", "jwt authentication", "jwt based authentication")),
            new SkillDefinition("RBAC", "security", List.of("rbac", "role based access control", "role-based access control", "preauthorize")),
            new SkillDefinition("PostgreSQL", "database", List.of("postgresql", "postgres")),
            new SkillDefinition("MySQL", "database", List.of("mysql")),
            new SkillDefinition("Redis", "database", List.of("redis", "cacheable", "cache eviction", "api response caching", "caching")),
            new SkillDefinition("Query Optimization", "database", List.of("query optimization", "optimized query", "query efficiency")),
            new SkillDefinition("Indexing", "database", List.of("indexing", "indexes")),
            new SkillDefinition("Docker", "devops", List.of("docker", "containerization")),
            new SkillDefinition("AWS EC2", "devops", List.of("aws ec2", "ec2", "amazon ec2")),
            new SkillDefinition("Flyway", "devops", List.of("flyway")),
            new SkillDefinition("SLF4J", "devops", List.of("slf4j")),
            new SkillDefinition("Logback", "devops", List.of("logback")),
            new SkillDefinition("JUnit", "devops", List.of("junit")),
            new SkillDefinition("Swagger/OpenAPI", "devops", List.of("swagger", "openapi", "swagger openapi")),
            new SkillDefinition("Postman", "devops", List.of("postman")),
            new SkillDefinition("Maven", "devops", List.of("maven")),
            new SkillDefinition("Git", "devops", List.of("git")),
            new SkillDefinition("Scheduling", "devops", List.of("scheduled", "scheduler", "scheduled jobs"))
    );

    @Override
    public UserDto.ParsedResumeResponse parseResumeText(String text) {
        try {
            String structuredJson = resumeExtractionClient.extractStructuredJson(text);
            AiParsedResumePayload payload = objectMapper.readValue(structuredJson, AiParsedResumePayload.class);
            return buildValidatedResponse(payload, null, null);
        } catch (Exception e) {
            log.error("Resume text parsing failed", e);
            return UserDto.ParsedResumeResponse.builder()
                    .version(CACHE_VERSION)
                    .status(UserDto.ParsedResumeResponse.ParsingStatus.FAILED)
                    .confidence(UserDto.ParsedResumeResponse.ConfidenceLevel.LOW)
                    .error("Resume parsing failed")
                    .build();
        }
    }

    @Async("resumeParsingExecutor")
    @Override
    public void parseAndStore(UUID userId, String resumeKey, String originalFilename) {
        long startTime = System.currentTimeMillis();
        String lockKey = LOCK_PREFIX + userId;
        String dataKey = REDIS_KEY_PREFIX + userId;

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (acquired == null || !acquired) {
            log.warn("Parsing already in progress for user: {}", userId);
            return;
        }

        try {
            redisTemplate.opsForValue().set(dataKey, objectMapper.writeValueAsString(
                    UserDto.ParsedResumeResponse.builder()
                            .version(CACHE_VERSION)
                            .status(UserDto.ParsedResumeResponse.ParsingStatus.PENDING)
                            .resumeKey(resumeKey)
                            .build()
            ), TTL_HOURS, TimeUnit.HOURS);

            log.info("Starting resume parsing for user: {} (key: {})", userId, resumeKey);

            Optional<User> currentUser = userRepository.findById(userId);
            if (currentUser.isPresent() && !resumeKey.equals(currentUser.get().getResumeS3Key())) {
                log.warn("Resume key mismatch for user {}. Continuing parsing but logging warning.", userId);
            }

            String resumeText = extractResumeText(resumeKey, originalFilename);
            UserDto.ParsedResumeResponse parsed = parseResumeText(resumeText);
            parsed.setResumeKey(resumeKey);
            parsed.setParsedAt(Instant.now());

            persistParsedResume(userId, parsed, resumeText);

            redisTemplate.opsForValue().set(dataKey, objectMapper.writeValueAsString(parsed), TTL_HOURS, TimeUnit.HOURS);
            log.info("Resume parsing completed for user {} with status {} in {}ms",
                    userId, parsed.getStatus(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("Resume parsing failed for user: {}", userId, e);
            cacheFailedResult(dataKey, resumeKey, e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public UserDto.ParsedResumeResponse getParsedData(UUID userId) {
        String dataKey = REDIS_KEY_PREFIX + userId;
        String json = redisTemplate.opsForValue().get(dataKey);
        if (json == null) {
            return UserDto.ParsedResumeResponse.builder()
                    .version(CACHE_VERSION)
                    .status(UserDto.ParsedResumeResponse.ParsingStatus.NOT_FOUND)
                    .build();
        }

        try {
            UserDto.ParsedResumeResponse response = objectMapper.readValue(json, UserDto.ParsedResumeResponse.class);
            if (response.getVersion() == null || response.getVersion() < CACHE_VERSION) {
                UserDto.ParsedResumeResponse migrated = migrateLegacyParsedResponse(json);
                redisTemplate.opsForValue().set(dataKey, objectMapper.writeValueAsString(migrated), TTL_HOURS, TimeUnit.HOURS);
                log.info("Migrated parsed resume cache to version {} for user {}", CACHE_VERSION, userId);
                return migrated;
            }
            return response;
        } catch (Exception readError) {
            try {
                UserDto.ParsedResumeResponse migrated = migrateLegacyParsedResponse(json);
                redisTemplate.opsForValue().set(dataKey, objectMapper.writeValueAsString(migrated), TTL_HOURS, TimeUnit.HOURS);
                log.info("Recovered legacy parsed resume cache for user {}", userId);
                return migrated;
            } catch (Exception migrationError) {
                log.error("Failed to read parsed resume data for user: {}", userId, readError);
                return UserDto.ParsedResumeResponse.builder()
                        .version(CACHE_VERSION)
                        .status(UserDto.ParsedResumeResponse.ParsingStatus.FAILED)
                        .confidence(UserDto.ParsedResumeResponse.ConfidenceLevel.LOW)
                        .error("Could not read parsed resume data")
                        .build();
            }
        }
    }

    @Override
    public void clearParsedData(UUID userId) {
        redisTemplate.delete(REDIS_KEY_PREFIX + userId);
        log.info("Cleared parsed resume data for user: {}", userId);
    }

    @Transactional
    protected void persistParsedResume(UUID userId, UserDto.ParsedResumeResponse parsed, String resumeText) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for parsed resume persistence"));

        if (parsed.getRole() != null && !parsed.getRole().isBlank() && (user.getCurrentRole() == null || user.getCurrentRole().isBlank())) {
            user.setCurrentRole(parsed.getRole());
            log.info("Persisted parsed resume role for user {}", userId);
        }

        if (parsed.getSkills() != null && !parsed.getSkills().isEmpty() && (user.getSkills() == null || user.getSkills().isEmpty())) {
            boolean isVerified = parsed.getConfidence() == UserDto.ParsedResumeResponse.ConfidenceLevel.HIGH;
            UserDomain.replaceParsedSkills(user, parsed.getSkills(), isVerified);
            log.info("Persisted parsed resume {} skills for user {}", parsed.getSkills().size(), userId);
        } else if (parsed.getSkills() == null || parsed.getSkills().isEmpty()) {
            log.warn("Skipping skill persistence for user {} because extracted skill count was 0", userId);
        } else {
            log.info("Skipping skill persistence for user {} because skills already exist", userId);
        }

        if (user.getPrimaryEmail() == null || user.getPrimaryEmail().isBlank() || user.getPrimaryEmail().contains("noreply.github.com")) {
             String email = extractEmailFromText(resumeText);
             if (email != null) {
                 user.setPrimaryEmail(email);
                 user.setEmailVerified(false);
                 log.info("Extracted email {} from resume for user {}", email, userId);
             }
        }

        updateOnboardingState(user);
        userRepository.save(user);
    }

    private void updateOnboardingState(User user) {
        boolean isComplete = user.getPrimaryEmail() != null && !user.getPrimaryEmail().isBlank()
                && user.getCurrentRole() != null && !user.getCurrentRole().isBlank()
                && user.getSkills() != null && !user.getSkills().isEmpty();
        user.setOnboardingCompleted(isComplete);
    }

    private String extractEmailFromText(String text) {
        if (text == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}").matcher(text);
        if (m.find()) {
             return m.group();
        }
        return null;
    }

    private UserDto.ParsedResumeResponse buildValidatedResponse(AiParsedResumePayload payload, String resumeKey, Instant parsedAt) {
        String normalizedRole = normalizeRole(payload != null ? payload.role() : null);
        List<UserDto.ParsedResumeResponse.ParsedSkill> normalizedSkills = normalizeSkills(payload != null ? payload.skills() : null);

        UserDto.ParsedResumeResponse.ParsingStatus status = UserDto.ParsedResumeResponse.ParsingStatus.READY;
        String error = null;

        return UserDto.ParsedResumeResponse.builder()
                .version(CACHE_VERSION)
                .status(status)
                .skills(normalizedSkills)
                .role(normalizedRole != null ? normalizedRole : inferRoleFromSkills(normalizedSkills))
                .confidence(toConfidenceLevel(payload != null ? payload.confidence() : null, normalizedSkills.size(),
                        normalizedRole != null))
                .error(error)
                .resumeKey(resumeKey)
                .parsedAt(parsedAt)
                .build();
    }

    private List<UserDto.ParsedResumeResponse.ParsedSkill> normalizeSkills(List<AiParsedSkill> rawSkills) {
        if (rawSkills == null) {
            return List.of();
        }

        Map<String, UserDto.ParsedResumeResponse.ParsedSkill> normalized = new LinkedHashMap<>();
        for (AiParsedSkill rawSkill : rawSkills) {
            if (rawSkill == null || rawSkill.name() == null || rawSkill.name().isBlank()) {
                continue;
            }
            SkillDefinition definition = findSkillDefinition(rawSkill.name());
            String canonicalName = definition != null ? definition.canonicalName() : normalizeSkillName(rawSkill.name());
            String category = definition != null ? definition.category() : normalizeCategory(rawSkill.category());
            if (canonicalName == null || category == null) {
                continue;
            }
            normalized.putIfAbsent(canonicalName.toLowerCase(Locale.ROOT), UserDto.ParsedResumeResponse.ParsedSkill.builder()
                    .name(canonicalName)
                    .category(category)
                    .build());
        }

        return normalized.values().stream().limit(MAX_SKILLS).toList();
    }

    private SkillDefinition findSkillDefinition(String rawName) {
        String normalizedInput = normalize(rawName);
        for (SkillDefinition definition : SKILL_DEFINITIONS) {
            if (normalize(definition.canonicalName()).equals(normalizedInput)) {
                return definition;
            }
            boolean aliasMatch = definition.aliases().stream()
                    .map(this::normalize)
                    .anyMatch(normalizedInput::equals);
            if (aliasMatch) {
                return definition;
            }
        }
        return null;
    }

    private String normalizeSkillName(String rawName) {
        SkillDefinition definition = findSkillDefinition(rawName);
        if (definition != null) {
            return definition.canonicalName();
        }
        String cleaned = rawName == null ? null : rawName.trim();
        if (cleaned == null || cleaned.isBlank()) {
            return null;
        }
        return cleaned.replaceAll("\\s+", " ");
    }

    private String normalizeCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return null;
        }
        String normalized = rawCategory.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "backend", "security", "database", "devops" -> normalized;
            default -> null;
        };
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return null;
        }
        String normalized = normalize(rawRole);
        if (normalized.contains("java backend developer")) return "Java Backend Developer";
        if (normalized.contains("backend developer") || normalized.contains("backend engineer")) return "Backend Developer";
        if (normalized.contains("full stack developer")) return "Full Stack Developer";
        if (normalized.contains("software engineer")) return "Software Engineer";
        return toTitleCase(rawRole.trim());
    }

    private String inferRoleFromSkills(List<UserDto.ParsedResumeResponse.ParsedSkill> skills) {
        List<String> names = skills.stream().map(skill -> skill.getName().toLowerCase(Locale.ROOT)).toList();
        if (names.contains("java") && names.contains("spring boot") && names.contains("rest apis")) {
            return "Java Backend Developer";
        }
        if (names.contains("spring boot") || names.contains("jpa")) {
            return "Backend Developer";
        }
        return "Software Engineer";
    }

    private UserDto.ParsedResumeResponse.ConfidenceLevel toConfidenceLevel(String rawConfidence, int skillCount, boolean hasRole) {
        if (rawConfidence != null) {
            try {
                return UserDto.ParsedResumeResponse.ConfidenceLevel.valueOf(rawConfidence.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (hasRole && skillCount >= 8) return UserDto.ParsedResumeResponse.ConfidenceLevel.HIGH;
        if (skillCount >= 4) return UserDto.ParsedResumeResponse.ConfidenceLevel.MEDIUM;
        return UserDto.ParsedResumeResponse.ConfidenceLevel.LOW;
    }

    private UserDto.ParsedResumeResponse migrateLegacyParsedResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<UserDto.ParsedResumeResponse.ParsedSkill> migratedSkills = new ArrayList<>();
        JsonNode skillsNode = root.get("skills");

        if (skillsNode != null && skillsNode.isArray()) {
            for (JsonNode skillNode : skillsNode) {
                if (skillNode.isTextual()) {
                    UserDto.ParsedResumeResponse.ParsedSkill migrated = migrateLegacyStringSkill(skillNode.asText());
                    if (migrated != null) migratedSkills.add(migrated);
                } else if (skillNode.isObject()) {
                    String name = readText(skillNode, "name");
                    String category = readText(skillNode, "category");
                    SkillDefinition definition = findSkillDefinition(name);
                    if (definition != null || category != null) {
                        migratedSkills.add(UserDto.ParsedResumeResponse.ParsedSkill.builder()
                                .name(definition != null ? definition.canonicalName() : normalizeSkillName(name))
                                .category(definition != null ? definition.category() : normalizeCategory(category))
                                .build());
                    }
                }
            }
        } else if (skillsNode != null && skillsNode.isObject()) {
            migratedSkills.addAll(migrateLegacyGroupedSkills(skillsNode));
        }

        List<UserDto.ParsedResumeResponse.ParsedSkill> normalizedSkills = normalizeSkills(
                migratedSkills.stream()
                        .map(skill -> new AiParsedSkill(skill.getName(), skill.getCategory()))
                        .toList()
        );

        UserDto.ParsedResumeResponse.ParsingStatus status = readEnum(root, "status", UserDto.ParsedResumeResponse.ParsingStatus.class);
        if (status == null) status = normalizedSkills.size() >= 5
                ? UserDto.ParsedResumeResponse.ParsingStatus.READY
                : UserDto.ParsedResumeResponse.ParsingStatus.WEAK_DATA;

        return UserDto.ParsedResumeResponse.builder()
                .version(CACHE_VERSION)
                .status(status)
                .skills(normalizedSkills)
                .role(normalizeRole(readText(root, "role")))
                .confidence(toConfidenceLevel(readText(root, "confidence"), normalizedSkills.size(), readText(root, "role") != null))
                .error(readText(root, "error"))
                .parsedAt(readInstant(root, "parsedAt"))
                .resumeKey(readText(root, "resumeKey"))
                .build();
    }

    private List<UserDto.ParsedResumeResponse.ParsedSkill> migrateLegacyGroupedSkills(JsonNode groupedNode) {
        List<UserDto.ParsedResumeResponse.ParsedSkill> migrated = new ArrayList<>();
        for (String category : List.of("backend", "security", "database", "devops")) {
            JsonNode categoryNode = groupedNode.get(category);
            if (categoryNode == null || !categoryNode.isArray()) continue;
            for (JsonNode skillNode : categoryNode) {
                if (!skillNode.isTextual()) continue;
                SkillDefinition definition = findSkillDefinition(skillNode.asText());
                String name = definition != null ? definition.canonicalName() : normalizeSkillName(skillNode.asText());
                String resolvedCategory = definition != null ? definition.category() : category;
                if (name != null) {
                    migrated.add(UserDto.ParsedResumeResponse.ParsedSkill.builder()
                            .name(name)
                            .category(resolvedCategory)
                            .build());
                }
            }
        }
        return migrated;
    }

    private UserDto.ParsedResumeResponse.ParsedSkill migrateLegacyStringSkill(String rawSkill) {
        SkillDefinition definition = findSkillDefinition(rawSkill);
        if (definition == null) return null;
        return UserDto.ParsedResumeResponse.ParsedSkill.builder()
                .name(definition.canonicalName())
                .category(definition.category())
                .build();
    }

    private String extractResumeText(String resumeKey, String originalFilename) {
        byte[] bytes = fileStorage.read(resumeKey);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Uploaded resume is empty");
        }

        String lowerName = originalFilename != null ? originalFilename.toLowerCase(Locale.ROOT) : "";
        if (lowerName.endsWith(".pdf")) {
            return extractPdfText(bytes);
        }
        if (lowerName.endsWith(".docx")) {
            return extractDocxText(bytes);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extractPdfText(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse PDF resume", e);
        }
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

    private void cacheFailedResult(String dataKey, String resumeKey, Exception e) {
        try {
            UserDto.ParsedResumeResponse failed = UserDto.ParsedResumeResponse.builder()
                    .version(CACHE_VERSION)
                    .status(UserDto.ParsedResumeResponse.ParsingStatus.FAILED)
                    .resumeKey(resumeKey)
                    .confidence(UserDto.ParsedResumeResponse.ConfidenceLevel.LOW)
                    .error(e.getMessage() != null ? e.getMessage() : "Unknown parsing error")
                    .build();
            redisTemplate.opsForValue().set(dataKey, objectMapper.writeValueAsString(failed), 1, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
    }

    private String readText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private Instant readInstant(JsonNode root, String fieldName) {
        String value = readText(root, fieldName);
        return value != null && !value.isBlank() ? Instant.parse(value) : null;
    }

    private <T extends Enum<T>> T readEnum(JsonNode root, String fieldName, Class<T> enumType) {
        String value = readText(root, fieldName);
        if (value == null || value.isBlank()) return null;
        return Enum.valueOf(enumType, value);
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.@/\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String toTitleCase(String value) {
        String[] parts = value.toLowerCase(Locale.ROOT).split("\\s+");
        List<String> transformed = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) continue;
            transformed.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return String.join(" ", transformed);
    }
}
