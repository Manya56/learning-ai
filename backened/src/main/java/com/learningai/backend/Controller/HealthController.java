package com.learningai.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learningai.backend.dto.response.ApiResponse;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Service health endpoints")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @RequestMapping(value = "/health", method = { RequestMethod.GET, RequestMethod.HEAD })
    @Operation(summary = "Full health check with DB and Redis status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        String dbStatus = checkDatabase();
        String redisStatus = checkRedis();

        boolean healthy = "UP".equals(dbStatus) && "UP".equals(redisStatus);

        Map<String, Object> health = Map.of(
                "status", healthy ? "UP" : "DEGRADED",
                "database", dbStatus,
                "redis", redisStatus,
                "timestamp", Instant.now().toString(),
                "version", "1.0.0");

        return ResponseEntity.ok(ApiResponse.ok("Health check complete", health));
    }

    private String checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkRedis() {
        try {
            redisTemplate.opsForValue().set("health:ping", "pong");
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
