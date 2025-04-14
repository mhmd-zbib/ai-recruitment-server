package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.AuthResponseBuilder;
import com.zbib.hiresync.dto.builder.UserBuilder;
import com.zbib.hiresync.dto.request.AuthRequest;
import com.zbib.hiresync.dto.request.LogoutRequest;
import com.zbib.hiresync.dto.request.RefreshTokenRequest;
import com.zbib.hiresync.dto.request.SignupRequest;
import com.zbib.hiresync.dto.response.AuthResponse;
import com.zbib.hiresync.dto.response.MessageResponse;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.entity.UserSession;
import com.zbib.hiresync.exception.InvalidTokenException;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.UserAuthenticationException;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.repository.UserSessionRepository;
import com.zbib.hiresync.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private UserBuilder userBuilder;

    @Mock
    private AuthResponseBuilder authResponseBuilder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private AuthRequest authRequest;
    private SignupRequest signupRequest;
    private UserSession userSession;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setLocked(false);

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

        // Setup user session
        userSession = new UserSession();
        userSession.setSessionId(UUID.randomUUID().toString());
        userSession.setUser(testUser);
        userSession.setDeviceInfo("Test Device");
        userSession.setIpAddress("127.0.0.1");
        userSession.setCreatedAt(Instant.now());
        userSession.setLastUsedAt(Instant.now());
        userSession.setRevoked(false);
        userSession.setTokenHash("test-token-hash");

        // Setup auth response
        authResponse = AuthResponse.builder()
                .userId(testUser.getId())
                .email(testUser.getEmail())
                .firstName(testUser.getFirstName())
                .lastName(testUser.getLastName())
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .sessionId(userSession.getSessionId())
                .build();

        // Setup SecurityContext mock
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldLoginSuccessfully() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(request.getHeader("User-Agent")).thenReturn("Test Browser");
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(userSession);
        when(authResponseBuilder.buildLoginResponse(eq(testUser), eq(authentication), anyString()))
                .thenReturn(authResponse);

        // Act
        AuthResponse result = authService.login(authRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(securityContext).setAuthentication(authentication);
        verify(userSessionRepository).save(any(UserSession.class));
        verify(authResponseBuilder).buildLoginResponse(eq(testUser), eq(authentication), anyString());
    }

    @Test
    void shouldFailLoginWithInvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(UserAuthenticationException.class, () -> authService.login(authRequest));
        verify(securityContext, never()).setAuthentication(any());
    }

    @Test
    void shouldSignupSuccessfully() {
        // Arrange
        when(userRepository.existsByEmail(signupRequest.getEmail())).thenReturn(false);
        when(userBuilder.buildUser(signupRequest)).thenReturn(testUser);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(request.getHeader("User-Agent")).thenReturn("Test Browser");
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(userSession);
        when(authResponseBuilder.buildSignupResponse(eq(testUser), eq(authentication), anyString()))
                .thenReturn(authResponse);

        // Act
        AuthResponse result = authService.signup(signupRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).save(testUser);
        verify(securityContext).setAuthentication(authentication);
        verify(userSessionRepository).save(any(UserSession.class));
        verify(authResponseBuilder).buildSignupResponse(eq(testUser), eq(authentication), anyString());
    }

    @Test
    void shouldFailSignupWithExistingEmail() {
        // Arrange
        when(userRepository.existsByEmail(signupRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(UserAuthenticationException.class, () -> authService.signup(signupRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("valid-refresh-token");
        refreshRequest.setSessionId(userSession.getSessionId());

        when(tokenProvider.validateToken(refreshRequest.getRefreshToken())).thenReturn(true);
        when(tokenProvider.getEmailFromToken(refreshRequest.getRefreshToken())).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userSessionRepository.findBySessionIdAndUser(refreshRequest.getSessionId(), testUser))
                .thenReturn(Optional.of(userSession));
        when(tokenProvider.getAuthorities(refreshRequest.getRefreshToken()))
                .thenReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        when(authResponseBuilder.buildRefreshResponse(eq(testUser), any(Authentication.class), eq(refreshRequest.getSessionId())))
                .thenReturn(authResponse);

        // Act
        AuthResponse result = authService.refreshToken(refreshRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userSessionRepository).save(userSession);
        verify(authResponseBuilder).buildRefreshResponse(eq(testUser), any(Authentication.class), eq(refreshRequest.getSessionId()));
    }

    @Test
    void shouldFailRefreshWithInvalidToken() {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid-refresh-token");
        refreshRequest.setSessionId(userSession.getSessionId());

        when(tokenProvider.validateToken(refreshRequest.getRefreshToken())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(refreshRequest));
    }

    @Test
    void shouldFailRefreshWithRevokedSession() {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("valid-refresh-token");
        refreshRequest.setSessionId(userSession.getSessionId());

        userSession.setRevoked(true);

        when(tokenProvider.validateToken(refreshRequest.getRefreshToken())).thenReturn(true);
        when(tokenProvider.getEmailFromToken(refreshRequest.getRefreshToken())).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userSessionRepository.findBySessionIdAndUser(refreshRequest.getSessionId(), testUser))
                .thenReturn(Optional.of(userSession));

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(refreshRequest));
        verify(userSessionRepository).findByUserAndRevokedFalse(testUser);
    }

    @Test
    void shouldLogoutSuccessfully() {
        // Arrange
        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setSessionId(userSession.getSessionId());

        when(userSessionRepository.findBySessionId(logoutRequest.getSessionId())).thenReturn(Optional.of(userSession));

        // Act
        MessageResponse result = authService.logout(logoutRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Logged out successfully", result.getMessage());
        assertTrue(userSession.isRevoked());
        verify(userSessionRepository).save(userSession);
    }

    @Test
    void shouldFailLogoutWithInvalidSession() {
        // Arrange
        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setSessionId("invalid-session-id");

        when(userSessionRepository.findBySessionId(logoutRequest.getSessionId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> authService.logout(logoutRequest));
    }

    @Test
    void shouldLogoutAllDevicesSuccessfully() {
        // Arrange
        List<UserSession> activeSessions = Arrays.asList(userSession, createUserSession());
        
        when(authentication.getName()).thenReturn(testUser.getEmail());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userSessionRepository.findByUserAndRevokedFalse(testUser)).thenReturn(activeSessions);

        // Act
        MessageResponse result = authService.logoutAllDevices();

        // Assert
        assertNotNull(result);
        assertEquals("Logged out from all devices successfully", result.getMessage());
        assertTrue(activeSessions.stream().allMatch(UserSession::isRevoked));
        verify(userSessionRepository).saveAll(activeSessions);
    }

    @Test
    void shouldGetUserSessionsSuccessfully() {
        // Arrange
        List<UserSession> activeSessions = Arrays.asList(userSession, createUserSession());
        
        when(authentication.getName()).thenReturn(testUser.getEmail());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userSessionRepository.findByUserAndRevokedFalse(testUser)).thenReturn(activeSessions);

        // Act
        List<UserSession> result = authService.getUserSessions();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(activeSessions, result);
    }

    @Test
    void shouldRevokeSessionSuccessfully() {
        // Arrange
        when(authentication.getName()).thenReturn(testUser.getEmail());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userSessionRepository.findBySessionIdAndUser(userSession.getSessionId(), testUser))
                .thenReturn(Optional.of(userSession));

        // Act
        MessageResponse result = authService.revokeSession(userSession.getSessionId());

        // Assert
        assertNotNull(result);
        assertEquals("Session revoked successfully", result.getMessage());
        assertTrue(userSession.isRevoked());
        verify(userSessionRepository).save(userSession);
    }

    @Test
    void shouldFailRevokeWithInvalidSession() {
        // Arrange
        when(authentication.getName()).thenReturn(testUser.getEmail());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userSessionRepository.findBySessionIdAndUser("invalid-session-id", testUser))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> authService.revokeSession("invalid-session-id"));
    }

    private UserSession createUserSession() {
        UserSession session = new UserSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUser(testUser);
        session.setDeviceInfo("Another Device");
        session.setIpAddress("192.168.1.1");
        session.setCreatedAt(Instant.now());
        session.setLastUsedAt(Instant.now());
        session.setRevoked(false);
        session.setTokenHash("another-test-token-hash");
        return session;
    }
} 