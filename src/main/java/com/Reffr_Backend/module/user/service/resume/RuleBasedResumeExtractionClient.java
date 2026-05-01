package com.Reffr_Backend.module.user.service.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RuleBasedResumeExtractionClient implements ResumeExtractionClient {

    private enum SkillCategory {
        BACKEND("backend"),
        SECURITY("security"),
        DATABASE("database"),
        DEVOPS("devops");

        private final String value;

        SkillCategory(String value) {
            this.value = value;
        }
    }

    private record SkillDefinition(String canonicalName, SkillCategory category, List<String> aliases) {}
    private record ParsedSkill(String name, String category) {}
    private record ExtractionPayload(String role, List<ParsedSkill> skills, String confidence) {}
    private record SectionBundle(String header, String summary, String technicalSkills, String projectExperience, String fullText) {}

    private final ObjectMapper objectMapper;

    private static final int MAX_TOTAL_SKILLS = 15;

    private static final List<SkillDefinition> SKILL_DEFINITIONS = List.of(
            new SkillDefinition("Java", SkillCategory.BACKEND, List.of("java", "java 8", "java 11", "java 17", "java 21")),
            new SkillDefinition("Python", SkillCategory.BACKEND, List.of("python")),
            new SkillDefinition("Spring Boot", SkillCategory.BACKEND, List.of("spring boot", "springboot")),
            new SkillDefinition("Spring MVC", SkillCategory.BACKEND, List.of("spring mvc")),
            new SkillDefinition("JPA", SkillCategory.BACKEND, List.of("jpa", "spring data jpa", "jakarta persistence", "jpa specification")),
            new SkillDefinition("Hibernate", SkillCategory.BACKEND, List.of("hibernate")),
            new SkillDefinition("REST APIs", SkillCategory.BACKEND, List.of("rest api", "rest apis", "restful api", "restful apis", "rest")),
            new SkillDefinition("Pagination", SkillCategory.BACKEND, List.of("pagination", "pageable")),
            new SkillDefinition("JPQL", SkillCategory.BACKEND, List.of("jpql")),
            new SkillDefinition("Native SQL", SkillCategory.BACKEND, List.of("native sql")),
            new SkillDefinition("Spring Security", SkillCategory.SECURITY, List.of("spring security")),
            new SkillDefinition("JWT", SkillCategory.SECURITY, List.of("jwt", "jwt authentication", "jwt based authentication")),
            new SkillDefinition("RBAC", SkillCategory.SECURITY, List.of("rbac", "role based access control", "role-based access control", "preauthorize")),
            new SkillDefinition("PostgreSQL", SkillCategory.DATABASE, List.of("postgresql", "postgres")),
            new SkillDefinition("MySQL", SkillCategory.DATABASE, List.of("mysql")),
            new SkillDefinition("Redis", SkillCategory.DATABASE, List.of("redis", "cacheable", "cache eviction", "api response caching", "caching")),
            new SkillDefinition("Query Optimization", SkillCategory.DATABASE, List.of("query optimization", "optimized query", "query efficiency")),
            new SkillDefinition("Indexing", SkillCategory.DATABASE, List.of("indexing", "indexes")),
            new SkillDefinition("Docker", SkillCategory.DEVOPS, List.of("docker", "containerization")),
            new SkillDefinition("AWS EC2", SkillCategory.DEVOPS, List.of("aws ec2", "ec2", "amazon ec2")),
            new SkillDefinition("Flyway", SkillCategory.DEVOPS, List.of("flyway")),
            new SkillDefinition("SLF4J", SkillCategory.DEVOPS, List.of("slf4j")),
            new SkillDefinition("Logback", SkillCategory.DEVOPS, List.of("logback")),
            new SkillDefinition("JUnit", SkillCategory.DEVOPS, List.of("junit")),
            new SkillDefinition("Swagger/OpenAPI", SkillCategory.DEVOPS, List.of("swagger", "openapi", "swagger openapi")),
            new SkillDefinition("Postman", SkillCategory.DEVOPS, List.of("postman")),
            new SkillDefinition("Maven", SkillCategory.DEVOPS, List.of("maven")),
            new SkillDefinition("Git", SkillCategory.DEVOPS, List.of("git")),
            new SkillDefinition("Scheduling", SkillCategory.DEVOPS, List.of("scheduled", "scheduler", "scheduled jobs"))
    );

    private static final Map<SkillCategory, List<String>> CATEGORY_PRIORITY = Map.of(
            SkillCategory.BACKEND, List.of("Java", "Python", "Spring Boot", "Spring MVC", "JPA", "Hibernate", "REST APIs", "Pagination", "JPQL", "Native SQL"),
            SkillCategory.SECURITY, List.of("Spring Security", "JWT", "RBAC"),
            SkillCategory.DATABASE, List.of("PostgreSQL", "MySQL", "Redis", "Query Optimization", "Indexing"),
            SkillCategory.DEVOPS, List.of("Docker", "AWS EC2", "Flyway", "SLF4J", "Logback", "JUnit", "Swagger/OpenAPI", "Postman", "Maven", "Git", "Scheduling")
    );

    private static final LinkedHashMap<String, List<String>> ROLE_PATTERNS = new LinkedHashMap<>();

    static {
        ROLE_PATTERNS.put("Java Backend Developer", List.of("java backend developer", "java spring boot developer", "spring boot developer"));
        ROLE_PATTERNS.put("Backend Developer", List.of("backend developer", "backend engineer", "backend engineering role"));
        ROLE_PATTERNS.put("Software Engineer", List.of("software engineer", "software developer"));
        ROLE_PATTERNS.put("Full Stack Developer", List.of("full stack developer", "fullstack developer", "full stack engineer"));
    }

    @Override
    public String extractStructuredJson(String resumeText) {
        String safeText = Objects.requireNonNullElse(resumeText, "");
        SectionBundle sections = extractSections(safeText);
        Map<String, Integer> scores = scoreSkills(sections);
        List<ParsedSkill> skills = buildFlatSkills(scores);
        String role = extractRole(sections, scores);
        String confidence = calculateConfidence(role, skills.size());

        try {
            return objectMapper.writeValueAsString(new ExtractionPayload(role, skills, confidence));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize extracted resume payload", e);
        }
    }

    private SectionBundle extractSections(String rawText) {
        List<String> lines = Arrays.stream(rawText.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        String header = lines.stream().limit(8).collect(Collectors.joining("\n"));
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentSection = "fullText";
        sections.put(currentSection, new ArrayList<>());

        for (String line : lines) {
            String matchedSection = detectSection(normalize(line));
            if (matchedSection != null) {
                currentSection = matchedSection;
                sections.putIfAbsent(currentSection, new ArrayList<>());
                continue;
            }
            sections.computeIfAbsent(currentSection, ignored -> new ArrayList<>()).add(line);
            sections.get("fullText").add(line);
        }

        return new SectionBundle(
                header,
                join(sections.get("summary")),
                join(sections.get("technicalSkills")),
                join(sections.get("projectExperience")),
                join(sections.get("fullText"))
        );
    }

    private String detectSection(String normalizedLine) {
        if (normalizedLine.contains("technical skills") || normalizedLine.equals("skills")) return "technicalSkills";
        if (normalizedLine.contains("professional summary") || normalizedLine.equals("summary") || normalizedLine.contains("profile summary")) return "summary";
        if (normalizedLine.contains("project experience") || normalizedLine.contains("projects")) return "projectExperience";
        if (normalizedLine.contains("professional experience") || normalizedLine.contains("work experience") || normalizedLine.equals("experience")) return "projectExperience";
        return null;
    }

    private Map<String, Integer> scoreSkills(SectionBundle sections) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scoreSection(scores, sections.technicalSkills(), 5);
        scoreSection(scores, sections.projectExperience(), 3);
        scoreSection(scores, sections.summary(), 2);
        scoreSection(scores, sections.header(), 2);
        scoreSection(scores, sections.fullText(), 1);
        return scores;
    }

    private void scoreSection(Map<String, Integer> scores, String text, int weight) {
        String normalized = normalize(text);
        if (normalized.isBlank()) return;

        for (SkillDefinition skill : SKILL_DEFINITIONS) {
            if (skill.aliases().stream().anyMatch(alias -> containsAlias(normalized, alias))) {
                scores.merge(skill.canonicalName(), weight, Integer::sum);
            }
        }
    }

    private List<ParsedSkill> buildFlatSkills(Map<String, Integer> scores) {
        Map<SkillCategory, List<String>> grouped = new EnumMap<>(SkillCategory.class);
        int total = 0;
        for (SkillCategory category : List.of(SkillCategory.BACKEND, SkillCategory.SECURITY, SkillCategory.DATABASE, SkillCategory.DEVOPS)) {
            List<String> categorySkills = new ArrayList<>();
            for (String skill : CATEGORY_PRIORITY.getOrDefault(category, List.of())) {
                if (scores.getOrDefault(skill, 0) <= 0 || total >= MAX_TOTAL_SKILLS) continue;
                categorySkills.add(skill);
                total++;
            }
            if (!categorySkills.isEmpty()) grouped.put(category, categorySkills);
        }

        List<ParsedSkill> flattened = new ArrayList<>();
        for (SkillCategory category : List.of(SkillCategory.BACKEND, SkillCategory.SECURITY, SkillCategory.DATABASE, SkillCategory.DEVOPS)) {
            for (String skill : grouped.getOrDefault(category, List.of())) {
                flattened.add(new ParsedSkill(skill, category.value));
            }
        }
        return flattened;
    }

    private String extractRole(SectionBundle sections, Map<String, Integer> skillScores) {
        for (String source : List.of(sections.header(), sections.summary(), sections.projectExperience(), sections.fullText())) {
            String role = extractExplicitRole(source);
            if (role != null) return role;
        }

        Set<String> backendSignals = Set.of("Java", "Spring Boot", "REST APIs", "JPA", "Hibernate");
        long matches = backendSignals.stream().filter(skill -> skillScores.getOrDefault(skill, 0) > 0).count();
        if (matches >= 3 && skillScores.getOrDefault("Java", 0) > 0) return "Java Backend Developer";
        if (matches >= 2) return "Backend Developer";
        if (skillScores.getOrDefault("Spring Boot", 0) > 0 && skillScores.getOrDefault("REST APIs", 0) > 0) return "Backend Developer";
        return "Software Engineer";
    }

    private String extractExplicitRole(String text) {
        String normalized = normalize(text);
        for (Map.Entry<String, List<String>> entry : ROLE_PATTERNS.entrySet()) {
            if (entry.getValue().stream().anyMatch(alias -> containsAlias(normalized, alias))) {
                if ("Backend Developer".equals(entry.getKey()) && containsAlias(normalized, "java")) {
                    return "Java Backend Developer";
                }
                return entry.getKey();
            }
        }

        Matcher matcher = Pattern.compile("\\b(java\\s+backend\\s+developer|backend\\s+developer|backend\\s+engineer|software\\s+engineer|full\\s+stack\\s+developer)\\b")
                .matcher(normalized);
        if (!matcher.find()) return null;
        return switch (matcher.group(1)) {
            case "java backend developer" -> "Java Backend Developer";
            case "backend developer", "backend engineer" -> "Backend Developer";
            case "full stack developer" -> "Full Stack Developer";
            default -> "Software Engineer";
        };
    }

    private String calculateConfidence(String role, int skillCount) {
        if (role != null && skillCount >= 8) return "HIGH";
        if (skillCount >= 4) return "MEDIUM";
        return "LOW";
    }

    private boolean containsAlias(String normalizedText, String alias) {
        String candidate = " " + normalize(alias) + " ";
        return (" " + normalizedText + " ").contains(candidate);
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.@/\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String join(List<String> lines) {
        return lines == null ? "" : String.join("\n", lines);
    }
}
