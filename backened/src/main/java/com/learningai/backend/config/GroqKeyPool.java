package com.learningai.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GroqKeyPool — round-robin load balancer across multiple Groq API keys.
 *
 * FIX: @Value("${groq.api-keys}") cannot bind a YAML list to List<String>.
 * Solution: use @ConfigurationProperties(prefix = "groq") — Spring Boot
 * automatically maps groq.api-keys → setApiKeys(List<String>).
 *
 * Also add @EnableConfigurationProperties(GroqKeyPool.class) to your
 * main application class, OR add @ConfigurationPropertiesScan to it.
 * Alternatively the @Component annotation here is enough in most setups.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "groq")
public class GroqKeyPool {

    // Spring maps groq.api-keys → this via setApiKeys()
    private List<String> apiKeys = new ArrayList<>();

    private final List<KeyEntry> pool    = new ArrayList<>();
    private final AtomicInteger  counter = new AtomicInteger(0);

    private static final long COOLDOWN_SECONDS = 60L;

    // ── Called by Spring after all properties are bound ───────────────────

    public void setApiKeys(List<String> keys) {
        this.apiKeys = keys;
        pool.clear();

        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException(
                    "No Groq API keys configured. " +
                    "Add at least one key under groq.api-keys in application.yaml");
        }

        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                pool.add(new KeyEntry(key.trim()));
            }
        }

        if (pool.isEmpty()) {
            throw new IllegalStateException("All groq.api-keys entries are blank.");
        }

        log.info("GroqKeyPool ready — {} key(s) loaded", pool.size());
    }

    public List<String> getApiKeys() {
        return apiKeys;
    }

    // ── Pick next key ─────────────────────────────────────────────────────

    public String nextKey() {
        if (pool.isEmpty()) {
            throw new IllegalStateException("GroqKeyPool has no keys");
        }

        int size  = pool.size();
        int start = Math.abs(counter.getAndIncrement() % size);

        for (int i = 0; i < size; i++) {
            KeyEntry entry = pool.get((start + i) % size);
            if (!entry.isCooledDown()) {
                return entry.key;
            }
        }

        // All cooled — return least-recently-cooled
        log.warn("All Groq keys cooled down — using least-cooled key");
        return pool.stream()
                .min((a, b) -> Long.compare(a.cooldownUntil.get(), b.cooldownUntil.get()))
                .map(e -> e.key)
                .orElse(pool.get(0).key);
    }

    // ── Mark a key as rate-limited ────────────────────────────────────────

    public void markRateLimited(String key) {
        pool.stream().filter(e -> e.key.equals(key)).findFirst().ifPresent(entry -> {
            entry.cooldownUntil.set(Instant.now().getEpochSecond() + COOLDOWN_SECONDS);
            log.warn("Groq key rate-limited — cooling {}s", COOLDOWN_SECONDS);
        });
    }

    public void markError(String key, int statusCode) {
        log.warn("Groq key error HTTP {} — no cooldown applied", statusCode);
    }

    public int poolSize() { return pool.size(); }

    public long activeKeysCount() {
        return pool.stream().filter(e -> !e.isCooledDown()).count();
    }

    // ── Inner ─────────────────────────────────────────────────────────────

    private static class KeyEntry {
        final String     key;
        final AtomicLong cooldownUntil = new AtomicLong(0L);

        KeyEntry(String key) { this.key = key; }

        boolean isCooledDown() {
            return Instant.now().getEpochSecond() < cooldownUntil.get();
        }
    }
}