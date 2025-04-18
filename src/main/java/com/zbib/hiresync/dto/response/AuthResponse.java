package com.zbib.hiresync.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    
    private String userId;
    private String email;
    private String displayName;
    private String role;
    private boolean success;
    
    // Token data
    private String sessionId;
    private String accessToken;
    private String refreshToken;
    
    // Expiration info in seconds
    private long expiresIn;
    private String tokenType;
} 