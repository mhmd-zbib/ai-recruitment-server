package com.zbib.hiresync.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/** Utility class for JWT token operations. */
@Component
public final class JwtUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);

  private final Key signingKey;

  @Value("${jwt.expiration}")
  private long jwtExpiration;

  @Value("${jwt.refresh-expiration}")
  private long refreshExpiration;

  /**
   * Creates a new JwtUtil instance.
   *
   * @param jwtSigningKey the JWT signing key
   */
  public JwtUtil(final byte[] jwtSigningKey) {
    // Create a copy of the signing key to prevent external modification
    byte[] keyBytes = new byte[jwtSigningKey.length];
    System.arraycopy(jwtSigningKey, 0, keyBytes, 0, jwtSigningKey.length);
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Extracts the username from a JWT token.
   *
   * @param token the JWT token
   * @return the username
   */
  public String extractUsername(final String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Extracts the expiration date from a JWT token.
   *
   * @param token the JWT token
   * @return the expiration date
   */
  public Date extractExpiration(final String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Extracts a claim from a JWT token.
   *
   * @param token the JWT token
   * @param claimsResolver the claims resolver function
   * @return the extracted claim
   */
  public <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(final String token) {
    return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
  }

  private boolean isTokenExpired(final String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * Generates a JWT token for a user.
   *
   * @param userDetails the user details
   * @return the generated JWT token
   */
  public String generateToken(final UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    return generateToken(claims, userDetails.getUsername());
  }

  private String generateToken(final Map<String, Object> claims, final String subject) {
    return buildToken(claims, subject, jwtExpiration);
  }

  private String buildToken(
      final Map<String, Object> claims, final String subject, final long expiration) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Validates a JWT token.
   *
   * @param token the JWT token
   * @param userDetails the user details
   * @return true if the token is valid, false otherwise
   */
  public boolean validateToken(final String token, final UserDetails userDetails) {
    try {
      final String username = extractUsername(token);
      return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    } catch (SignatureException e) {
      LOGGER.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      LOGGER.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      LOGGER.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      LOGGER.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      LOGGER.error("JWT claims string is empty: {}", e.getMessage());
    }
    return false;
  }
}
