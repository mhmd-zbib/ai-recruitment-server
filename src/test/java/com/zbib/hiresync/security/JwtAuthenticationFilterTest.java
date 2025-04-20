package com.zbib.hiresync.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid-jwt-token";
    private static final String INVALID_TOKEN = "invalid-jwt-token";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should set authentication when valid token is provided")
    void shouldSetAuthenticationWhenValidTokenIsProvided() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTH_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getAuthentication(VALID_TOKEN)).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(securityContext).setAuthentication(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when token is invalid")
    void shouldNotSetAuthenticationWhenTokenIsInvalid() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTH_HEADER)).thenReturn(BEARER_PREFIX + INVALID_TOKEN);
        when(tokenProvider.validateToken(INVALID_TOKEN)).thenReturn(false);
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when no token is provided")
    void shouldNotSetAuthenticationWhenNoTokenIsProvided() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTH_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when token format is invalid")
    void shouldNotSetAuthenticationWhenTokenFormatIsInvalid() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTH_HEADER)).thenReturn("InvalidFormat " + INVALID_TOKEN);
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear security context when token validation throws exception")
    void shouldClearSecurityContextWhenTokenValidationThrowsException() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTH_HEADER)).thenReturn(BEARER_PREFIX + INVALID_TOKEN);
        when(tokenProvider.validateToken(INVALID_TOKEN)).thenThrow(new RuntimeException("Token validation error"));
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear security context when authentication extraction throws exception")
    void shouldClearSecurityContextWhenAuthenticationExtractionThrowsException() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTH_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getAuthentication(VALID_TOKEN)).thenThrow(new RuntimeException("Authentication error"));
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should always call the filter chain regardless of authentication result")
    void shouldAlwaysCallFilterChainRegardlessOfAuthenticationResult() throws ServletException, IOException {
        // Arrange - no token scenario
        when(request.getHeader(AUTH_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);

        // Arrange again - valid token scenario
        reset(filterChain);
        when(request.getHeader(AUTH_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getAuthentication(VALID_TOKEN)).thenReturn(authentication);

        // Act again
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert again
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle empty bearer token")
    void shouldHandleEmptyBearerToken() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTH_HEADER)).thenReturn(BEARER_PREFIX); // Empty token after "Bearer "
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }
} 