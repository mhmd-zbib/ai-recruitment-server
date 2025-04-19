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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    private AuthRequest authRequest;
    private SignupRequest signupRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private LogoutRequest logoutRequest;
    private AuthResponse authResponse;
    private List<UserSession> userSessions;

    @BeforeEach
    void setUp() {
        // Reset the mocked service for each test
        when(authService.toString()).thenCallRealMethod(); // Just to verify it's a mock
        
        // Setup auth request
        authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password");

        // Setup signup request
        signupRequest = new SignupRequest();
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password");
        signupRequest.setFirstName("Test");
        signupRequest.setLastName("User");

        // Setup refresh token request
        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh-token");
        refreshTokenRequest.setSessionId("session-id");

        // Setup logout request
        logoutRequest = new LogoutRequest();
        logoutRequest.setSessionId("session-id");

        // Setup auth response
        authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID().toString())
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .sessionId("session-id")
                .build();

        // Setup user sessions
        UserSession session1 = new UserSession();
        session1.setSessionId("session-id-1");
        session1.setDeviceInfo("Device 1");
        session1.setIpAddress("127.0.0.1");
        session1.setCreatedAt(java.time.Instant.now());
        session1.setLastUsedAt(java.time.Instant.now());
        session1.setRevoked(false);
        session1.setTokenHash("token-hash-1");

        UserSession session2 = new UserSession();
        session2.setSessionId("session-id-2");
        session2.setDeviceInfo("Device 2");
        session2.setIpAddress("192.168.1.1");
        session2.setCreatedAt(java.time.Instant.now());
        session2.setLastUsedAt(java.time.Instant.now());
        session2.setRevoked(false);
        session2.setTokenHash("token-hash-2");

        userSessions = Arrays.asList(session1, session2);
    }

    @Test
    void shouldReturnAuthResponseOnLogin() throws Exception {
        // Arrange
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(authResponse.getEmail())))
                .andExpect(jsonPath("$.access_token", is(authResponse.getAccessToken())))
                .andExpect(jsonPath("$.refresh_token", is(authResponse.getRefreshToken())))
                .andExpect(jsonPath("$.session_id", is(authResponse.getSessionId())));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new UserAuthenticationException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnAuthResponseOnSignup() throws Exception {
        // Arrange
        when(authService.signup(any(SignupRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is(authResponse.getEmail())))
                .andExpect(jsonPath("$.access_token", is(authResponse.getAccessToken())))
                .andExpect(jsonPath("$.refresh_token", is(authResponse.getRefreshToken())))
                .andExpect(jsonPath("$.session_id", is(authResponse.getSessionId())));
    }

    @Test
    void shouldReturnUnauthorizedForExistingUser() throws Exception {
        // Arrange
        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new UserAuthenticationException("Email is already in use"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNewAuthResponseOnRefresh() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(authResponse.getEmail())))
                .andExpect(jsonPath("$.access_token", is(authResponse.getAccessToken())))
                .andExpect(jsonPath("$.refresh_token", is(authResponse.getRefreshToken())))
                .andExpect(jsonPath("$.session_id", is(authResponse.getSessionId())));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidToken() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidTokenException("Invalid refresh token"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnSuccessMessageOnLogout() throws Exception {
        // Arrange
        when(authService.logout(any(LogoutRequest.class)))
                .thenReturn(new MessageResponse("Logged out successfully"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logged out successfully")));
    }

    @Test
    void shouldReturnNotFoundForInvalidSession() throws Exception {
        // Arrange
        when(authService.logout(any(LogoutRequest.class)))
                .thenThrow(new ResourceNotFoundException("Session not found"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldReturnSuccessMessageOnLogoutAllDevices() throws Exception {
        // Arrange
        when(authService.logoutAllDevices())
                .thenReturn(new MessageResponse("Logged out from all devices successfully"));

        // Act & Assert
        mockMvc.perform(post("/v1/auth/logout-all")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logged out from all devices successfully")));
    }

    @Test
    @WithMockUser
    void shouldReturnListOfActiveSessions() throws Exception {
        // Arrange
        when(authService.getUserSessions()).thenReturn(userSessions);

        // Act & Assert
        mockMvc.perform(get("/v1/auth/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sessionId", is("session-id-1")))
                .andExpect(jsonPath("$[1].sessionId", is("session-id-2")));
    }

    @Test
    @WithMockUser
    void shouldReturnSuccessMessageOnRevokeSession() throws Exception {
        // Arrange
        when(authService.revokeSession(anyString()))
                .thenReturn(new MessageResponse("Session revoked successfully"));

        // Act & Assert
        mockMvc.perform(delete("/v1/auth/sessions/session-id-1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Session revoked successfully")));
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundForInvalidSessionRevoke() throws Exception {
        // Arrange
        when(authService.revokeSession(anyString()))
                .thenThrow(new ResourceNotFoundException("Session not found"));

        // Act & Assert
        mockMvc.perform(delete("/v1/auth/sessions/invalid-session-id")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
} 