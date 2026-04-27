package com.learningai.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.entity.User;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper    objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip rate limiting for public endpoints
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get current user
        String userId = extractUserId();
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Resolve correct bucket based on endpoint
        Bucket bucket = resolveBucket(userId, path);

        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers to response
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;

            log.warn("Rate limit exceeded — user:{} path:{} wait:{}s",
                    userId, path, waitSeconds);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("X-Rate-Limit-Retry-After-Seconds",
                    String.valueOf(waitSeconds));

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "Rate limit exceeded. Try again in " +
                    waitSeconds + " seconds.",
                    "RATE_LIMIT_EXCEEDED");

            response.getWriter().write(
                    objectMapper.writeValueAsString(errorResponse));
        }
    }

    private Bucket resolveBucket(String userId, String path) {
        // AI endpoints — strict limit
        if (path.contains("/api/learn/") ||
            path.contains("/api/mentor/chat") ||
            path.contains("/api/practice/generate") ||
            path.contains("/api/practice/evaluate")) {

            // Also check daily limit
            Bucket dailyBucket = rateLimitConfig
                    .resolveDailyAiBucket(userId);
            if (!dailyBucket.tryConsume(1)) {
                // Daily limit hit — return an exhausted bucket
                return rateLimitConfig.resolveDailyAiBucket(userId);
            }
            return rateLimitConfig.resolveAiBucket(userId);
        }

        // Scraping endpoints — very strict
        if (path.contains("/api/admin/scrape")) {
            return rateLimitConfig.resolveScrapeBucket(userId);
        }

        // Quiz endpoints
        if (path.contains("/api/quiz/")) {
            return rateLimitConfig.resolveQuizBucket(userId);
        }

        // Everything else
        return rateLimitConfig.resolveGeneralBucket(userId);
    }

    private String extractUserId() {
        try {
            Object principal = SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            if (principal instanceof User user) {
                return user.getId().toString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/actuator");
    }
}