package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.request.AuthRequest;
import com.zbib.hiresync.dto.request.LogoutRequest;
import com.zbib.hiresync.dto.request.RefreshTokenRequest;
import com.zbib.hiresync.dto.request.SignupRequest;
import com.zbib.hiresync.dto.response.AuthResponse;
import com.zbib.hiresync.dto.response.MessageResponse;
import com.zbib.hiresync.entity.UserSession;
import com.zbib.hiresync.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password", description = "Authenticates a user and returns a JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/signup")
    @Operation(summary = "Create a new user account", description = "Registers a new user and returns a JWT token")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshRequest) {
        return ResponseEntity.ok(authService.refreshToken(refreshRequest));
    }
    
    @PostMapping("/logout")
    @Operation(summary = "Logout from current device", description = "Invalidates the current session")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest logoutRequest) {
        return ResponseEntity.ok(authService.logout(logoutRequest));
    }
    
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout from all devices", description = "Invalidates all active sessions for the current user")
    public ResponseEntity<MessageResponse> logoutAllDevices() {
        return ResponseEntity.ok(authService.logoutAllDevices());
    }
    
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get active sessions", description = "Lists all active sessions for the current user")
    public ResponseEntity<List<UserSession>> getSessions() {
        return ResponseEntity.ok(authService.getUserSessions());
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke a specific session", description = "Invalidates a specific session by ID")
    public ResponseEntity<MessageResponse> revokeSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(authService.revokeSession(sessionId));
    }
} 