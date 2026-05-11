package com.learningai.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    // ── Per-user bucket store (in-memory for MVP) ─────────────────────────
    // For production replace with Redis-backed buckets
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // ── Limits ────────────────────────────────────────────────────────────

    // AI endpoints — expensive, rate-limited strictly
    // 20 requests per minute per user
    public Bucket resolveAiBucket(String userId) {
        return buckets.computeIfAbsent(
                "ai:" + userId, k -> buildBucket(10, Duration.ofMinutes(1)));
    }

    // Scraping endpoints — very expensive
    // 5 requests per minute per user
    public Bucket resolveScrapeBucket(String userId) {
        return buckets.computeIfAbsent(
                "scrape:" + userId, k -> buildBucket(5, Duration.ofMinutes(1)));
    }

    // Quiz endpoints — moderate
    // 60 requests per minute per user
    public Bucket resolveQuizBucket(String userId) {
        return buckets.computeIfAbsent(
                "quiz:" + userId, k -> buildBucket(60, Duration.ofMinutes(1)));
    }

    // General API — generous
    // 200 requests per minute per user
    public Bucket resolveGeneralBucket(String userId) {
        return buckets.computeIfAbsent(
                "general:" + userId, k -> buildBucket(200, Duration.ofMinutes(1)));
    }

    // Daily AI limit — 200 AI calls per user per day
    public Bucket resolveDailyAiBucket(String userId) {
        return buckets.computeIfAbsent(
                "daily-ai:" + userId,
                k -> buildBucket(200, Duration.ofHours(24)));
    }

    private Bucket buildBucket(int capacity, Duration duration) {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.greedy(capacity, duration));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}