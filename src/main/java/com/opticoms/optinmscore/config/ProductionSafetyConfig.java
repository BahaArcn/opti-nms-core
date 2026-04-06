package com.opticoms.optinmscore.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("prod")
public class ProductionSafetyConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @PostConstruct
    public void validate() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            throw new IllegalStateException(
                    "Production profile requires APP_CORS_ALLOWED_ORIGINS to be set. " +
                    "It cannot be blank.");
        }
        if ("*".equals(allowedOrigins.trim())) {
            log.warn("SECURITY: APP_CORS_ALLOWED_ORIGINS is set to wildcard '*'. " +
                     "This should be replaced with explicit frontend domain(s) before GA release.");
        }
    }
}
