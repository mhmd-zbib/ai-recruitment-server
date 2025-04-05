package com.zbib.hiresync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class JwtConfig {
    private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);

    @Value("${JWT_SECRET}")
    private String secretKeyValue;

    @Bean
    public Key jwtSigningKey() {
        logger.debug("Initializing JWT signing key");
        try {
            if (secretKeyValue == null || secretKeyValue.trim().isEmpty()) {
                throw new IllegalStateException("JWT_SECRET environment variable is not set");
            }
            return Keys.hmacShaKeyFor(secretKeyValue.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("Failed to initialize JWT signing key", e);
            throw new IllegalStateException("JWT signing key could not be initialized", e);
        }
    }
} 