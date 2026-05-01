package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.service.resume.RuleBasedResumeExtractionClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeParsingServiceImplTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResumeParsingServiceImpl service = new ResumeParsingServiceImpl(
            redisTemplate,
            objectMapper,
            mock(com.Reffr_Backend.module.user.repository.UserRepository.class),
            mock(com.Reffr_Backend.module.user.infrastructure.FileStorageService.class),
            new RuleBasedResumeExtractionClient(objectMapper)
    );

    @Test
    void parsesBackendResumeIntoFlatCanonicalSkills() {
        String resumeText = """
                ADITYA KUMAR
                Java Backend Developer | Spring Boot | REST APIs

                PROFESSIONAL SUMMARY
                Java Backend Developer with internship experience building RESTful APIs using Spring Boot, Spring Data JPA,
                PostgreSQL, Redis, JWT authentication, Spring Security and AWS EC2 deployment.

                TECHNICAL SKILLS
                Languages: Java 17, SQL
                Backend: Spring Boot, Spring MVC, Spring Data JPA, Hibernate, REST APIs, Pageable
                Security: JWT Authentication, Spring Security, Role-Based Access Control (RBAC)
                Database: PostgreSQL, MySQL, Redis, Indexing
                DevOps: Docker, AWS EC2, Flyway, SLF4J, Logback
                """;

        UserDto.ParsedResumeResponse parsed = service.parseResumeText(resumeText);

        assertEquals(2, parsed.getVersion());
        assertEquals(UserDto.ParsedResumeResponse.ParsingStatus.READY, parsed.getStatus());
        assertEquals("Java Backend Developer", parsed.getRole());
        assertEquals(UserDto.ParsedResumeResponse.ConfidenceLevel.HIGH, parsed.getConfidence());
        assertIterableEquals(
                java.util.List.of("Java", "Spring Boot", "Spring MVC", "JPA", "Hibernate", "REST APIs", "Pagination"),
                parsed.getSkills().stream().filter(skill -> "backend".equals(skill.getCategory())).map(UserDto.ParsedResumeResponse.ParsedSkill::getName).toList()
        );
        assertIterableEquals(
                java.util.List.of("Spring Security", "JWT", "RBAC"),
                parsed.getSkills().stream().filter(skill -> "security".equals(skill.getCategory())).map(UserDto.ParsedResumeResponse.ParsedSkill::getName).toList()
        );
        assertIterableEquals(
                java.util.List.of("PostgreSQL", "MySQL", "Redis", "Indexing"),
                parsed.getSkills().stream().filter(skill -> "database".equals(skill.getCategory())).map(UserDto.ParsedResumeResponse.ParsedSkill::getName).toList()
        );
    }

    @Test
    void migratesLegacyFlatSkillsArrayFromCache() {
        UUID userId = UUID.randomUUID();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("parsed_resume:" + userId)).thenReturn("""
                {
                  "status":"READY",
                  "skills":["java","spring boot","jwt","postgresql","docker"],
                  "role":"Java Backend Developer",
                  "confidence":"HIGH",
                  "resumeKey":"resume-key"
                }
                """);

        UserDto.ParsedResumeResponse response = service.getParsedData(userId);

        assertEquals(2, response.getVersion());
        assertEquals("Java Backend Developer", response.getRole());
        assertIterableEquals(
                java.util.List.of("Java", "Spring Boot", "JWT", "PostgreSQL", "Docker"),
                response.getSkills().stream().map(UserDto.ParsedResumeResponse.ParsedSkill::getName).toList()
        );
        verify(valueOperations).set(eq("parsed_resume:" + userId), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }
}
