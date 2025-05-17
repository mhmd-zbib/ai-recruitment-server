package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.builder.AuthResponseBuilder;
import com.zbib.hiresync.dto.builder.UserBuilder;
import com.zbib.hiresync.dto.request.AuthRequest;
import com.zbib.hiresync.dto.request.SignupRequest;
import com.zbib.hiresync.dto.response.AuthResponse;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.AuthException;
import com.zbib.hiresync.repository.UserRepository;
import com.zbib.hiresync.security.JwtTokenProvider;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserBuilder userBuilder;
    private final AuthResponseBuilder authResponseBuilder;
    private final JwtTokenProvider tokenProvider;


    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> AuthException.userNotFound(request.getEmail()));
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authResponseBuilder.buildLoginResponse(user, authentication);
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AuthException.emailAlreadyExists(request.getEmail());
        }

        User user = userBuilder.buildUser(request);
        User savedUser = userRepository.save(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser.getEmail(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return authResponseBuilder.buildSignupResponse(savedUser, authentication);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw AuthException.invalidToken(refreshToken);
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AuthException.userNotFound(email));

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                tokenProvider.getAuthorities(refreshToken));

        return authResponseBuilder.buildRefreshResponse(user, authentication);
    }

    public void logout(String token) {
        SecurityContextHolder.clearContext();
    }
}