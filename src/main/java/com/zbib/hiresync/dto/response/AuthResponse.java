package com.zbib.hiresync.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zbib.hiresync.logging.SensitiveData;
import com.zbib.hiresync.logging.SensitiveData.SensitiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("email")
    private String email;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("access_token")
    @SensitiveData(type = SensitiveType.CREDENTIALS)
    private String accessToken;
    
    @JsonProperty("refresh_token")
    @SensitiveData(type = SensitiveType.CREDENTIALS)
    private String refreshToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("session_id")
    private String sessionId;
} 