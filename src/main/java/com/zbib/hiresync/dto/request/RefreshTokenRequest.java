package com.zbib.hiresync.dto.request;

import com.zbib.hiresync.logging.SensitiveData;
import com.zbib.hiresync.logging.SensitiveData.SensitiveType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    
    @NotBlank(message = "Refresh token is required")
    @SensitiveData(type = SensitiveType.CREDENTIALS)
    private String refreshToken;
    
    @NotBlank(message = "Session ID is required")
    private String sessionId;
} 