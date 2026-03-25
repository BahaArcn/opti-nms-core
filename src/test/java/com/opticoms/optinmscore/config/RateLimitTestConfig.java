package com.opticoms.optinmscore.config;

import com.opticoms.optinmscore.config.ratelimit.InMemoryRateLimiter;
import com.opticoms.optinmscore.config.ratelimit.RateLimitProperties;
import com.opticoms.optinmscore.config.ratelimit.RateLimiter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Provides real rate limit beans with rate limiting disabled.
 * Imported by security integration tests to satisfy SecurityConfiguration's
 * constructor dependencies without using @MockBean (which can cause context
 * cache pollution across test classes).
 */
@TestConfiguration
public class RateLimitTestConfig {

    @Bean
    public RateLimitProperties rateLimitProperties() {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(false);
        return props;
    }

    @Bean
    public RateLimiter rateLimiter() {
        return new InMemoryRateLimiter();
    }
}
