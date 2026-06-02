package com.learningai.backend.controller;

import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void logoutWithoutAuthenticatedUserReturnsSuccess() {
        ResponseEntity<ApiResponse<Void>> response = authController.logout(null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verifyNoInteractions(authService);
    }

    @Test
    void logoutWithAuthenticatedUserClearsRefreshToken() {
        User user = new User();
        user.setEmail("learner@example.com");

        ResponseEntity<ApiResponse<Void>> response = authController.logout(user);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(authService).logout("learner@example.com");
    }
}
