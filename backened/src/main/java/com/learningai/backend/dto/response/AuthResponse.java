package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {

    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;
}