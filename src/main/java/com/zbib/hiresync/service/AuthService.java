package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.AuthResponseBuilder;
import com.zbib.hiresync.dto.builder.UserBuilder;
import com.zbib.hiresync.dto.request.AuthRequest;
import com.zbib.hiresync.dto.request.RefreshTokenRequest;
import com.zbib.hiresync.dto.request.SignupRequest;
import com.zbib.hiresync.dto.request.LogoutRequest;
import com.zbib.hiresync.dto.response.AuthResponse;
import com.zbib.hiresync.dto.response.MessageResponse;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.entity.UserSession;
import com.zbib.hiresync.exception.InvalidTokenException;
import com.zbib.hiresync.exception.ResourceNotFoundException;
import com.zbib.hiresync.exception.UserAuthenticationException;
import com.zbib.hiresync.logging.LogLevel;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.repository.UserSessionRepository;
import com.zbib.hiresync.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LogManager.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserBuilder userBuilder;
    private final AuthResponseBuilder authResponseBuilder;
    private final JwtTokenProvider tokenProvider;
    private final HttpServletRequest request;

    @LoggableService(message = "Login attempt for user: ${email}", level = LogLevel.INFO)
    public AuthResponse login(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

            String deviceInfo = extractDeviceInfo();
            String ipAddress = extractIpAddress();

            // Create and store session
            UserSession session = createUserSession(user, deviceInfo, ipAddress);

            return authResponseBuilder.buildLoginResponse(user, authentication, session.getSessionId());

        } catch (BadCredentialsException e) {
            logger.warn("Login attempt failed for user: {}", request.getEmail());
            throw new UserAuthenticationException("Invalid email or password");
        }
    }

    @Transactional
    @LoggableService(message = "User signup request for: ${request.email}", level = LogLevel.INFO)
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAuthenticationException("Email is already in use: " + request.getEmail());
        }

        User user = userBuilder.buildUser(request);
        User savedUser = userRepository.save(user);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String deviceInfo = extractDeviceInfo();
            String ipAddress = extractIpAddress();

            // Create and store session
            UserSession session = createUserSession(savedUser, deviceInfo, ipAddress);

            return authResponseBuilder.buildSignupResponse(savedUser, authentication, session.getSessionId());

        } catch (Exception e) {
            logger.error("Error during signup authentication: {}", e.getMessage());
            throw new UserAuthenticationException("Registration successful but unable to authenticate");
        }
    }

    @Transactional
    @LoggableService(message = "Token refresh request for session: ${refreshRequest.sessionId}", level = LogLevel.INFO)
    public AuthResponse refreshToken(RefreshTokenRequest refreshRequest) {
        String refreshToken = refreshRequest.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        String sessionId = refreshRequest.getSessionId();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for token"));

        UserSession session = userSessionRepository.findBySessionIdAndUser(sessionId, user)
                .orElseThrow(() -> new InvalidTokenException("Invalid session"));

        // Check if session is revoked
        if (session.isRevoked()) {
            revokeAllUserSessions(user);
            throw new InvalidTokenException("Session has been revoked");
        }

        // Validate token hash if available
        if (session.getTokenHash() != null && !session.getTokenHash().isEmpty()) {
            boolean validHash = tokenProvider.verifyTokenHash(refreshToken, session.getTokenHash());
            if (!validHash) {
                session.setRevoked(true);
                userSessionRepository.save(session);
                throw new InvalidTokenException("Invalid token hash");
            }
        }

        // Update session last used time
        session.setLastUsedAt(Instant.now());
        userSessionRepository.save(session);

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                tokenProvider.getAuthorities(refreshToken));

        return authResponseBuilder.buildRefreshResponse(user, authentication, sessionId);
    }

    @Transactional
    @LoggableService(message = "Processing logout request for session: ${logoutRequest.sessionId}", level = LogLevel.INFO)
    public MessageResponse logout(LogoutRequest logoutRequest) {
        String sessionId = logoutRequest.getSessionId();

        UserSession session = userSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setRevoked(true);
        userSessionRepository.save(session);

        return new MessageResponse("Logged out successfully");
    }

    @Transactional
    @LoggableService(message = "Processing logout from all devices request for user: ${authentication.name}", level = LogLevel.INFO)
    public MessageResponse logoutAllDevices() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        revokeAllUserSessions(user);

        return new MessageResponse("Logged out from all devices successfully");
    }

    @LoggableService(message = "Retrieving active sessions for user: ${authentication.name}", level = LogLevel.INFO)
    public List<UserSession> getUserSessions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userSessionRepository.findByUserAndRevokedFalse(user);
    }

    @Transactional
    @LoggableService(message = "Revoking session: ${sessionId} for user: ${authentication.name}", level = LogLevel.INFO)
    public MessageResponse revokeSession(String sessionId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserSession session = userSessionRepository.findBySessionIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setRevoked(true);
        userSessionRepository.save(session);

        return new MessageResponse("Session revoked successfully");
    }

    private UserSession createUserSession(User user, String deviceInfo, String ipAddress) {
        String sessionId = UUID.randomUUID().toString();

        UserSession session = new UserSession();
        session.setSessionId(sessionId);
        session.setUser(user);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        session.setCreatedAt(Instant.now());
        session.setLastUsedAt(Instant.now());
        session.setRevoked(false);

        // Store token hash for stateless validation
        String tokenData = sessionId + user.getId() + user.getEmail() + System.currentTimeMillis();
        session.setTokenHash(tokenProvider.calculateTokenHash(tokenData));

        return userSessionRepository.save(session);
    }

    private void revokeAllUserSessions(User user) {
        List<UserSession> sessions = userSessionRepository.findByUserAndRevokedFalse(user);
        sessions.forEach(session -> session.setRevoked(true));
        userSessionRepository.saveAll(sessions);
    }

    private String extractDeviceInfo() {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown Device";
    }

    private String extractIpAddress() {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}