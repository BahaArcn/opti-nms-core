package com.opticoms.optinmscore.config.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window-counter rate limiter backed by ConcurrentHashMap.
 * Each key (IP or username) gets a {@link Bucket} that tracks
 * remaining tokens within a fixed time window.
 *
 * <p><strong>Limitation:</strong> at window boundaries, up to 2x the configured
 * limit can be consumed within a short burst. For auth endpoints the
 * configured limit should therefore be set conservatively.</p>
 *
 * <p><strong>Note:</strong> buckets are JVM-local; this implementation does not
 * work correctly across multiple application instances (K8s pods).
 * Set {@code app.rate-limit.store=mongodb} for horizontal scaling.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.rate-limit.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Tries to consume one token for the given key.
     *
     * @return a {@link ConsumeResult} indicating whether the request is allowed,
     *         remaining tokens, and the reset timestamp (epoch seconds).
     */
    @Override
    public ConsumeResult tryConsume(String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new Bucket(maxRequests, now + windowSeconds * 1000L);
            }
            return existing;
        });

        int remaining = bucket.tokens.decrementAndGet();
        long resetEpochSecond = bucket.resetAtMillis / 1000;

        if (remaining < 0) {
            bucket.tokens.incrementAndGet();
            return new ConsumeResult(false, 0, resetEpochSecond, maxRequests);
        }

        return new ConsumeResult(true, remaining, resetEpochSecond, maxRequests);
    }

    /** Evict expired buckets to prevent unbounded memory growth. */
    @Scheduled(fixedDelayString = "#{${app.rate-limit.cleanup-interval-minutes:5} * 60000}")
    public void cleanup() {
        long now = System.currentTimeMillis();
        int before = buckets.size();
        buckets.entrySet().removeIf(e -> e.getValue().isExpired(now));
        int removed = before - buckets.size();
        if (removed > 0) {
            log.debug("Rate limiter cleanup: evicted {} expired buckets", removed);
        }
    }

    /** Visible for testing. */
    int bucketCount() {
        return buckets.size();
    }

    /** Visible for testing. */
    void clear() {
        buckets.clear();
    }

    // ── Inner classes ───────────────────────────────────────────────────

    static class Bucket {
        final AtomicInteger tokens;
        final long resetAtMillis;

        Bucket(int maxTokens, long resetAtMillis) {
            this.tokens = new AtomicInteger(maxTokens);
            this.resetAtMillis = resetAtMillis;
        }

        boolean isExpired(long nowMillis) {
            return nowMillis >= resetAtMillis;
        }
    }

}
