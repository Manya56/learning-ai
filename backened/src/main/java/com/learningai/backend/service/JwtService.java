package com.learningai.backend.service;

import com.learningai.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiry-ms}")
    private long expiryMs;

    @Value("${jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("role", user.getRole().name());
        claims.put("type", "access");
        return buildToken(claims, user.getEmail(), expiryMs);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");
        return buildToken(claims, user.getEmail(), refreshExpiryMs);
    }

    private String buildToken(Map<String, Object> claims,
                               String subject,
                               long expiry) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String userId = extractClaim(token,
                claims -> claims.get("userId", String.class));
        return UUID.fromString(userId);
    }

    public String extractTokenType(String token) {
        return extractClaim(token,
                claims -> claims.get("type", String.class));
    }

    public boolean isTokenValid(String token, User user) {
        try {
            String email = extractEmail(token);
            String type  = extractTokenType(token);
            return email.equals(user.getEmail())
                    && "access".equals(type)
                    && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token, User user) {
        try {
            String email = extractEmail(token);
            String type  = extractTokenType(token);
            return email.equals(user.getEmail())
                    && "refresh".equals(type)
                    && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token,
                                Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    public long getExpiryMs() {
        return expiryMs;
    }
}