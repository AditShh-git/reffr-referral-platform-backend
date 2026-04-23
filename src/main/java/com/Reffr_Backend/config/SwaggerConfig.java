package com.Reffr_Backend.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${reffr.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reffr API")
                        .description("""
                                Reffr — Referral-first hiring platform.

                                **Auth flow:**
                                1. GET /oauth2/authorization/github
                                2. Get access_token
                                3. Use Authorization: Bearer <token>
                                """)
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Reffr Team")
                                .url(frontendUrl)))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url(frontendUrl).description("Frontend")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT access token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}