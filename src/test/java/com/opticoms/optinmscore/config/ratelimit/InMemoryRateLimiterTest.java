package com.opticoms.optinmscore.config.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private InMemoryRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new InMemoryRateLimiter();
    }

    @Test
    void firstRequest_isAllowed() {
        var result = limiter.tryConsume("key1", 5, 60);
        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isEqualTo(4);
        assertThat(result.limit()).isEqualTo(5);
    }

    @Test
    void requestsWithinLimit_allAllowed() {
        for (int i = 0; i < 5; i++) {
            var result = limiter.tryConsume("key2", 5, 60);
            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(4 - i);
        }
    }

    @Test
    void exceedingLimit_isRejected() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("key3", 5, 60);
        }

        var result = limiter.tryConsume("key3", 5, 60);
        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isZero();
    }

    @Test
    void differentKeys_haveIndependentBuckets() {
        for (int i = 0; i < 3; i++) {
            limiter.tryConsume("keyA", 3, 60);
        }
        assertThat(limiter.tryConsume("keyA", 3, 60).allowed()).isFalse();

        var resultB = limiter.tryConsume("keyB", 3, 60);
        assertThat(resultB.allowed()).isTrue();
        assertThat(resultB.remaining()).isEqualTo(2);
    }

    @Test
    void resetEpochSecond_isInFuture() {
        var result = limiter.tryConsume("key4", 10, 120);
        long now = System.currentTimeMillis() / 1000;
        assertThat(result.resetEpochSecond()).isGreaterThan(now);
        assertThat(result.resetEpochSecond()).isLessThanOrEqualTo(now + 121);
    }

    @Test
    void cleanup_removesExpiredBuckets() {
        limiter.tryConsume("expired-key", 5, 0);
        assertThat(limiter.bucketCount()).isEqualTo(1);

        limiter.cleanup();
        assertThat(limiter.bucketCount()).isZero();
    }

    @Test
    void cleanup_keepsActiveBuckets() {
        limiter.tryConsume("active-key", 5, 3600);
        assertThat(limiter.bucketCount()).isEqualTo(1);

        limiter.cleanup();
        assertThat(limiter.bucketCount()).isEqualTo(1);
    }

    @Test
    void expiredBucket_getsNewWindow() throws InterruptedException {
        limiter.tryConsume("short-key", 1, 1);
        assertThat(limiter.tryConsume("short-key", 1, 1).allowed()).isFalse();

        Thread.sleep(1100);

        var result = limiter.tryConsume("short-key", 1, 1);
        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isZero();
    }

    @Test
    void singleRequestLimit_allowsExactlyOne() {
        assertThat(limiter.tryConsume("once", 1, 60).allowed()).isTrue();
        assertThat(limiter.tryConsume("once", 1, 60).allowed()).isFalse();
    }

    @Test
    void rejectedRequest_doesNotDrainBelowZero() {
        limiter.tryConsume("drain", 1, 60);
        for (int i = 0; i < 10; i++) {
            var r = limiter.tryConsume("drain", 1, 60);
            assertThat(r.allowed()).isFalse();
            assertThat(r.remaining()).isZero();
        }
    }
}
