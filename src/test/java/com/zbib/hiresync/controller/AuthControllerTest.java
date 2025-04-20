package com.zbib.hiresync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbib.hiresync.dto.request.AuthRequest;
import com.zbib.hiresync.dto.request.LogoutRequest;
import com.zbib.hiresync.dto.request.RefreshTokenRequest;
import com.zbib.hiresync.dto.request.SignupRequest;
import com.zbib.hiresync.dto.response.AuthResponse;
import com.zbib.hiresync.dto.response.MessageResponse;
import com.zbib.hiresync.entity.UserSession;
import com.zbib.hiresync.exception.InvalidTokenException;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.UserAuthenticationException;
import com.zbib.hiresync.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private AuthRequest authRequest;
    private SignupRequest signupRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private LogoutRequest logoutRequest;
    private AuthResponse authResponse;
    private MessageResponse messageResponse;
    private String testEmail = "test@example.com";
    private String testPassword = "password123";
    private String testUserId = UUID.randomUUID().toString();
    private String testSessionId = UUID.randomUUID().toString();
    private String testAccessToken = "test.access.token";
    private String testRefreshToken = "test.refresh.token";

    @BeforeEach
    void setUp() {
        // Set up request objects
        authRequest = AuthRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        signupRequest = SignupRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email(testEmail)
                .password(testPassword)
                .build();

        refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken(testRefreshToken)
                .sessionId(testSessionId)
                .build();

        logoutRequest = LogoutRequest.builder()
                .sessionId(testSessionId)
                .build();

        // Set up response objects
        authResponse = AuthResponse.builder()
                .userId(testUserId)
                .email(testEmail)
                .firstName("Test")
                .lastName("User")
                .displayName("Test User")
                .role("ROLE_USER")
                .success(true)
                .sessionId(testSessionId)
                .accessToken(testAccessToken)
                .refreshToken(testRefreshToken)
                .expiresIn(3600)
                .tokenType("Bearer")
                .build();

        messageResponse = new MessageResponse("Operation successful");
    }

    @Test
    @DisplayName("Login should return auth response with tokens when credentials are valid")
    void loginShouldReturnAuthResponseWhenCredentialsAreValid() throws Exception {
        // Arrange
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(testEmail)))
                .andExpect(jsonPath("$.access_token", is(testAccessToken)))
                .andExpect(jsonPath("$.refresh_token", is(testRefreshToken)))
                .andExpect(jsonPath("$.session_id", is(testSessionId)))
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("Login should return unauthorized when credentials are invalid")
    void loginShouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        // Arrange
        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new UserAuthenticationException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid credentials")));
    }

    @Test
    @DisplayName("Signup should return auth response with tokens when signup is successful")
    void signupShouldReturnAuthResponseWhenSignupIsSuccessful() throws Exception {
        // Arrange
        when(authService.signup(any(SignupRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is(testEmail)))
                .andExpect(jsonPath("$.access_token", is(testAccessToken)))
                .andExpect(jsonPath("$.refresh_token", is(testRefreshToken)))
                .andExpect(jsonPath("$.session_id", is(testSessionId)))
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("Signup should return unauthorized when email is already in use")
    void signupShouldReturnUnauthorizedWhenEmailIsAlreadyInUse() throws Exception {
        // Arrange
        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new UserAuthenticationException("Email is already in use"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Email is already in use")));
    }

    @Test
    @DisplayName("Refresh token should return new auth response when refresh token is valid")
    void refreshTokenShouldReturnNewAuthResponseWhenRefreshTokenIsValid() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(testEmail)))
                .andExpect(jsonPath("$.access_token", is(testAccessToken)))
                .andExpect(jsonPath("$.refresh_token", is(testRefreshToken)))
                .andExpect(jsonPath("$.session_id", is(testSessionId)))
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("Refresh token should return unauthorized when refresh token is invalid")
    void refreshTokenShouldReturnUnauthorizedWhenRefreshTokenIsInvalid() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidTokenException("Invalid refresh token"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid refresh token")));
    }

    @Test
    @DisplayName("Logout should return success message when logout is successful")
    void logoutShouldReturnSuccessMessageWhenLogoutIsSuccessful() throws Exception {
        // Arrange
        MessageResponse response = new MessageResponse("Logged out successfully");
        when(authService.logout(any(LogoutRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logged out successfully")));
    }

    @Test
    @DisplayName("Logout should return not found when session is not found")
    void logoutShouldReturnNotFoundWhenSessionIsNotFound() throws Exception {
        // Arrange
        when(authService.logout(any(LogoutRequest.class)))
                .thenThrow(new ResourceNotFoundException("Session not found"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Session not found")));
    }

    @Test
    @DisplayName("Request validation should reject invalid login request")
    void requestValidationShouldRejectInvalidLoginRequest() throws Exception {
        // Arrange
        AuthRequest invalidRequest = AuthRequest.builder()
                .email("not-an-email")
                .password("")  // Empty password
                .build();

        // Act & Assert
        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Request validation should reject invalid signup request")
    void requestValidationShouldRejectInvalidSignupRequest() throws Exception {
        // Arrange
        SignupRequest invalidRequest = SignupRequest.builder()
                .firstName("")  // Empty first name
                .lastName("")   // Empty last name
                .email("not-an-email")
                .password("123") // Too short password
                .build();

        // Act & Assert
        mockMvc.perform(post("/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Request validation should reject invalid refresh token request")
    void requestValidationShouldRejectInvalidRefreshTokenRequest() throws Exception {
        // Arrange
        RefreshTokenRequest invalidRequest = RefreshTokenRequest.builder()
                .refreshToken("")  // Empty refresh token
                .sessionId("")     // Empty session ID
                .build();

        // Act & Assert
        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Request validation should reject invalid logout request")
    void requestValidationShouldRejectInvalidLogoutRequest() throws Exception {
        // Arrange
        LogoutRequest invalidRequest = LogoutRequest.builder()
                .sessionId("")  // Empty session ID
                .build();

        // Act & Assert
        mockMvc.perform(post("/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
    }
} 