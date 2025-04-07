package com.zbib.hiresync.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.AuthenticationException;

@Configuration
public class JwtConfig {
    private static final Logger LOGGER = LogManager.getLogger(JwtConfig.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    @Bean
    public Key jwtSigningKey() {
        LOGGER.debug("Initializing JWT signing key");
        try {
            if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
                throw new IllegalStateException("JWT_SECRET environment variable is not set");
            }
            return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.error("Failed to initialize JWT signing key", e);
            throw new IllegalStateException("JWT signing key could not be initialized", e);
        }
    }

    public String getJwtSecret() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            LOGGER.error("JWT secret is not configured");
            throw new IllegalStateException("JWT secret is not configured");
        }
        return jwtSecret;
    }

    public int getJwtExpiration() {
        return jwtExpiration;
    }
} 