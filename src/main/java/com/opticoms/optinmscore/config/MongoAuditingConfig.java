package com.opticoms.optinmscore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * MongoDB Auditing Configuration.
 *
 * Enables automatic population of audit fields:
 * - @CreatedDate (already works without this config)
 * - @LastModifiedDate (already works without this config)
 * - @CreatedBy (requires this config)
 * - @LastModifiedBy (requires this config)
 *
 * This config provides a AuditorAware bean that tells Spring Data MongoDB
 * who the current user is.
 *
 * @author Opticoms Team
 * @version 0.1.0
 */
@Configuration
@EnableMongoAuditing
public class MongoAuditingConfig {

    /**
     * Supplies the current principal name for {@code @CreatedBy} / {@code @LastModifiedBy}.
     * Uses Spring Security; falls back to {@code "system"} when unauthenticated or anonymous.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            String username = authentication.getName();

            if ("anonymousUser".equals(username)) {
                return Optional.of("system");
            }

            return Optional.of(username);
        };
    }
}