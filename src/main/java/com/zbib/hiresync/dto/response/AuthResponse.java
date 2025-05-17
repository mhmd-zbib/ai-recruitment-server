package com.zbib.hiresync.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String id;

    private String email;

    private String firstName;

    private String lastName;

    private String displayName;

    private String role;

    private boolean success;

    private String accessToken;

    private String refreshToken;

    private long expiresIn;

    private String tokenType;
}