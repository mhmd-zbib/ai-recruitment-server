package com.zbib.hiresync.config;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration class for JWT (JSON Web Token) settings. */
@Configuration
public class JwtConfig {
  private static final Logger LOGGER = LogManager.getLogger(JwtConfig.class);

  /** JWT signing key from application properties. */
  @Value("${jwt.secret}")
  private String jwtSecret;

  /** JWT expiration time in milliseconds from application properties. */
  @Value("${jwt.expiration}")
  private long jwtExpiration;

  /**
   * Creates a signing key for JWT tokens.
   *
   * @return the JWT signing key
   * @throws IllegalStateException if JWT secret is not configured
   */
  @Bean
  public Key jwtSigningKey() {
    LOGGER.debug("Initializing JWT signing key");

    if (jwtSecret == null || jwtSecret.isBlank()) {
      LOGGER.error("JWT secret not configured");
      throw new IllegalStateException("JWT_SECRET environment variable is not set");
    }

    try {
      return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      LOGGER.error("Invalid JWT key material", e);
      throw new IllegalStateException(
          "JWT signing key could not be initialized: invalid key material", e);
    }
  }

  /**
   * Gets the JWT secret key.
   *
   * @return the JWT secret key
   * @throws IllegalStateException if JWT secret is not configured
   */
  public String getJwtSecret() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      LOGGER.error("JWT secret is not configured");
      throw new IllegalStateException("JWT secret is not configured");
    }
    return jwtSecret;
  }

  /**
   * Gets the JWT expiration time in milliseconds.
   *
   * @return the JWT expiration time
   */
  public long getJwtExpiration() {
    return jwtExpiration;
  }
}
