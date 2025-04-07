package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.AuthRequest;
import com.zbib.hiresync.dto.AuthResponse;
import com.zbib.hiresync.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for handling authentication operations. */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  /**
   * Authenticates a user with the provided credentials.
   *
   * @param request the authentication request
   * @return an authentication response with a JWT token
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
    return ResponseEntity.ok(authService.authenticate(request));
  }

  /**
   * Registers a new user with the provided information.
   *
   * @param request the registration request
   * @return an authentication response with a JWT token
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
    return ResponseEntity.ok(authService.register(request));
  }
}
