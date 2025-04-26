package com.zbib.hiresync.dto.builder;

import com.zbib.hiresync.dto.response.AuthResponse;
import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthResponseBuilder {
    
    private final JwtTokenProvider tokenProvider;
    
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken, Long expiresIn, String sessionId) {
        return AuthResponse.builder()
            .id(user.getId().toString())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(expiresIn)
            .sessionId(sessionId)
            .build();
    }

    public AuthResponse buildLoginResponse(User user, Authentication authentication, String sessionId) {
        String accessToken = tokenProvider.createToken(authentication);
        String refreshToken = tokenProvider.createRefreshToken(authentication);
        
        return buildAuthResponse(
            user, 
            accessToken, 
            refreshToken, 
            tokenProvider.getTokenValidityInMilliseconds(),
            sessionId
        );
    }
    
    public AuthResponse buildSignupResponse(User savedUser, Authentication authentication, String sessionId) {
        String accessToken = tokenProvider.createToken(authentication);
        String refreshToken = tokenProvider.createRefreshToken(authentication);
        
        return buildAuthResponse(
            savedUser, 
            accessToken, 
            refreshToken, 
            tokenProvider.getTokenValidityInMilliseconds(),
            sessionId
        );
    }
    
    public AuthResponse buildRefreshResponse(User user, Authentication authentication, String sessionId) {
        String accessToken = tokenProvider.createToken(authentication);
        String refreshToken = tokenProvider.createRefreshToken(authentication);
        
        return buildAuthResponse(
            user, 
            accessToken, 
            refreshToken, 
            tokenProvider.getTokenValidityInMilliseconds(),
            sessionId
        );
    }
} 