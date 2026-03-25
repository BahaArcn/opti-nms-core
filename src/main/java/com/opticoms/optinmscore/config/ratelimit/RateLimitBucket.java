package com.opticoms.optinmscore.config.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * MongoDB document for distributed rate-limit buckets.
 * TTL index on {@code resetAt} auto-removes expired buckets.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rate_limit_buckets")
public class RateLimitBucket {

    @Id
    private String id;

    private int count;

    private int limit;

    /** Epoch millis when this window expires. TTL index cleans up 60s after expiry. */
    @Indexed(expireAfterSeconds = 60)
    private Date resetAt;

    public RateLimitBucket(String id, int count, int limit, long resetAtMillis) {
        this.id = id;
        this.count = count;
        this.limit = limit;
        this.resetAt = new Date(resetAtMillis);
    }
}
