package com.opticoms.optinmscore.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class ProductionSafetyConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @PostConstruct
    public void validate() {
        if (allowedOrigins == null || allowedOrigins.isBlank() || "*".equals(allowedOrigins.trim())) {
            throw new IllegalStateException(
                    "Production profile requires explicit APP_CORS_ALLOWED_ORIGINS. " +
                    "Wildcard '*' is not allowed in production.");
        }
    }
}
