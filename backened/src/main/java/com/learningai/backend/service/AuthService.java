package com.learningai.backend.service;

import com.learningai.backend.dto.request.LoginRequest;
import com.learningai.backend.dto.request.RefreshTokenRequest;
import com.learningai.backend.dto.request.RegisterRequest;
import com.learningai.backend.dto.response.AuthResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService      jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.conflict("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // FIX: store hash of refresh token, not the raw token
        user.setRefreshToken(hashToken(refreshToken));
        userRepository.save(user);

        log.info("New user registered: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> AppException.unauthorized("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw AppException.unauthorized("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw AppException.forbidden("Account is disabled");
        }

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // FIX: store hash of refresh token
        user.setRefreshToken(hashToken(refreshToken));
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token     = request.getRefreshToken();
        String tokenHash = hashToken(token);

        // FIX: look up by hash, not raw token
        User user = userRepository.findByRefreshToken(tokenHash)
                .orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));

        if (!jwtService.isRefreshTokenValid(token, user)) {
            user.setRefreshToken(null);
            userRepository.save(user);
            throw AppException.unauthorized("Refresh token expired, please login again");
        }

        String newAccessToken  = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // FIX: store new hash
        user.setRefreshToken(hashToken(newRefreshToken));
        userRepository.save(user);

        log.info("Token refreshed for: {}", user.getEmail());
        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setRefreshToken(null);
            userRepository.save(user);
            log.info("User logged out: {}", email);
        });
    }

    // ─── Hash a token with SHA-256 ────────────────────────────────────────
    // Returns lowercase hex string — safe to store in DB

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to hash token: {}", e.getMessage());
            throw new RuntimeException("Token hashing failed");
        }
    }

    private AuthResponse buildAuthResponse(User user,
                                            String accessToken,
                                            String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)   // raw token returned to client only
                .accessTokenExpiresIn(jwtService.getExpiryMs())
                .build();
    }
}