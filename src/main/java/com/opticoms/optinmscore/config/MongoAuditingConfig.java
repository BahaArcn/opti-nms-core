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
@EnableMongoAuditing // ✅ MongoDB auditing'i aktif et
public class MongoAuditingConfig {

    /**
     * AuditorAware bean: Mevcut kullanıcıyı Spring Data MongoDB'ye söyler.
     *
     * Spring Security context'inden current user'ı alır.
     * Eğer user yoksa (anonymous request) "system" döner.
     *
     * @return AuditorAware implementation
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // Spring Security context'inden current user'ı al
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                // User login olmamışsa "system" yaz
                return Optional.of("system");
            }

            // Authenticated user'ın username'ini döndür
            String username = authentication.getName();

            // "anonymousUser" Spring Security'nin default anonymous user'ı
            if ("anonymousUser".equals(username)) {
                return Optional.of("system");
            }

            return Optional.of(username);
        };
    }
}