package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.AuthResponseBuilder;
import com.zbib.hiresync.dto.builder.UserBuilder;
import com.zbib.hiresync.dto.request.AuthRequest;
import com.zbib.hiresync.dto.request.SignupRequest;
import com.zbib.hiresync.dto.response.AuthResponse;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.auth.*;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.security.JwtTokenProvider;


import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Service for authentication operations.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LogManager.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserBuilder userBuilder;
    private final AuthResponseBuilder authResponseBuilder;
    private final JwtTokenProvider tokenProvider;


    @LoggableService(message = "User logging in with email: ${request.email}")
    public AuthResponse login(AuthRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            return authResponseBuilder.buildLoginResponse(user, authentication);

        } catch (BadCredentialsException e) {
            logger.warn("Invalid credentials for user: {}", request.getEmail());
            throw new InvalidCredentialsException();
        } catch (UserNotFoundException e) {
            logger.warn("Login attempt for non-existent user: {}", request.getEmail());
            throw new InvalidCredentialsException(); // Don't reveal that the user doesn't exist
        } catch (Exception e) {
            logger.error("Error during login for user: {}, error: {}", request.getEmail(), e.getMessage(), e);
            throw new UserAuthenticationException("Authentication failed: " + e.getMessage());
        }
    }

    @LoggableService(message = "User signing up with email: ${request.email}")
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAuthenticationException("Email is already in use: " + request.getEmail());
        }

        // First create and save the user
        User user = userBuilder.buildUser(request);
        User savedUser = userRepository.save(user);

        // Create authentication token directly (skip password verification since we just created the user)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            savedUser.getEmail(),
            null, // No need to include the password here
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return authResponseBuilder.buildSignupResponse(savedUser, authentication);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for token"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                tokenProvider.getAuthorities(refreshToken));

        return authResponseBuilder.buildRefreshResponse(user, authentication);
    }
    
    /**
     * Logout a user by clearing the security context.
     * 
     * In a stateless JWT system, we can't invalidate tokens on the server side.
     * The client is responsible for discarding the token.
     * 
     * @param token the JWT token (not used in stateless implementation)
     */
    @LoggableService(message = "User logging out")
    public void logout(String token) {
        // Get current user for logging purposes
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";
        logger.info("User logging out: {}", username);
        
        // In a stateless JWT system, we only clear the security context
        // The client is responsible for discarding the token
        SecurityContextHolder.clearContext();
    }


}