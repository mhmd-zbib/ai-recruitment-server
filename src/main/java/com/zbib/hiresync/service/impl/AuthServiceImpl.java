package com.zbib.hiresync.service.impl;

import com.zbib.hiresync.dto.request.AuthRequest;
import com.zbib.hiresync.dto.request.SignupRequest;
import com.zbib.hiresync.dto.response.AuthResponse;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.security.JwtTokenProvider;
import com.zbib.hiresync.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String accessToken = tokenProvider.createToken(authentication);
        String refreshToken = tokenProvider.createRefreshToken(authentication);
        
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + request.getEmail()));
        
        return AuthResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getTokenValidityInMilliseconds())
            .build();
    }

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use: " + request.getEmail());
        }
        
        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .enabled(true)
            .locked(false)
            .build();
        
        User savedUser = userRepository.save(user);
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String accessToken = tokenProvider.createToken(authentication);
        String refreshToken = tokenProvider.createRefreshToken(authentication);
        
        return AuthResponse.builder()
            .userId(savedUser.getId())
            .email(savedUser.getEmail())
            .firstName(savedUser.getFirstName())
            .lastName(savedUser.getLastName())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getTokenValidityInMilliseconds())
            .build();
    }
} 