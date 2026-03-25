package com.opticoms.optinmscore.config.ratelimit;

/**
 * Abstraction for rate limiting. Implementations may be JVM-local
 * ({@link InMemoryRateLimiter}) or distributed ({@link MongoRateLimiter}).
 */
public interface RateLimiter {

    /**
     * Tries to consume one token for the given key.
     *
     * @param key           bucket key (e.g. "auth:192.168.1.1" or "user:admin")
     * @param maxRequests   maximum requests allowed per window
     * @param windowSeconds window duration in seconds
     * @return result indicating whether the request is allowed
     */
    ConsumeResult tryConsume(String key, int maxRequests, int windowSeconds);

    record ConsumeResult(boolean allowed, int remaining, long resetEpochSecond, int limit) {}
}
