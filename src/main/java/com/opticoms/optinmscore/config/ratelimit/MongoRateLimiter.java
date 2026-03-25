package com.opticoms.optinmscore.config.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Distributed rate limiter backed by MongoDB.
 * Uses atomic {@code findAndModify} for consistency across multiple pods.
 *
 * <p>Activated when {@code app.rate-limit.store=mongodb}.</p>
 *
 * <p>Bucket lifecycle: expired documents are auto-removed by MongoDB's
 * TTL index on the {@code resetAt} field.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.store", havingValue = "mongodb")
public class MongoRateLimiter implements RateLimiter {

    private final MongoTemplate mongoTemplate;

    @Override
    public ConsumeResult tryConsume(String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        long resetAtMs = now + windowSeconds * 1000L;

        // Step 1: Atomically increment count on an active (non-expired) bucket
        RateLimitBucket bucket = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(key).and("resetAt").gt(new java.util.Date(now))),
                new Update().inc("count", 1),
                FindAndModifyOptions.options().returnNew(true),
                RateLimitBucket.class
        );

        if (bucket != null) {
            return toResult(bucket, maxRequests);
        }

        // Step 2: No active bucket — expired or absent. Remove stale and create fresh.
        mongoTemplate.remove(
                Query.query(Criteria.where("_id").is(key)),
                RateLimitBucket.class
        );

        try {
            bucket = new RateLimitBucket(key, 1, maxRequests, resetAtMs);
            mongoTemplate.insert(bucket);
            return new ConsumeResult(true, maxRequests - 1, resetAtMs / 1000, maxRequests);
        } catch (DuplicateKeyException e) {
            // Another thread created the bucket between remove and insert — retry increment
            bucket = mongoTemplate.findAndModify(
                    Query.query(Criteria.where("_id").is(key).and("resetAt").gt(new java.util.Date(now))),
                    new Update().inc("count", 1),
                    FindAndModifyOptions.options().returnNew(true),
                    RateLimitBucket.class
            );
            if (bucket != null) {
                return toResult(bucket, maxRequests);
            }
            // Extremely unlikely — fail open
            log.warn("Rate limiter race condition for key={}, allowing request", key);
            return new ConsumeResult(true, maxRequests - 1, resetAtMs / 1000, maxRequests);
        }
    }

    private ConsumeResult toResult(RateLimitBucket bucket, int maxRequests) {
        int remaining = bucket.getLimit() - bucket.getCount();
        long resetEpochSecond = bucket.getResetAt().getTime() / 1000;
        if (remaining < 0) {
            return new ConsumeResult(false, 0, resetEpochSecond, maxRequests);
        }
        return new ConsumeResult(true, remaining, resetEpochSecond, maxRequests);
    }
}
