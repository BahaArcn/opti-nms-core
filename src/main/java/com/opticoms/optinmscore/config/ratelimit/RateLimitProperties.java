package com.opticoms.optinmscore.config.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /** Backend store: "memory" (default, single-pod) or "mongodb" (distributed). */
    private String store = "memory";

    /** Max requests per window for general API endpoints. */
    private int requestsPerWindow = 100;

    /** Window duration in seconds for general API endpoints. */
    private int windowSeconds = 60;

    /** Max requests per window for auth endpoints (brute force protection). */
    private int authRequestsPerWindow = 5;

    /** Window duration in seconds for auth endpoints. */
    private int authWindowSeconds = 60;

    /** Interval in minutes for evicting expired buckets from memory. */
    private int cleanupIntervalMinutes = 5;
}
