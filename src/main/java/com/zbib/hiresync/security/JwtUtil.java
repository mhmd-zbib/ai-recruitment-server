package com.zbib.hiresync.security;

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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtil {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

  @Value("${JWT_EXPIRATION:86400000}")
  private Long tokenValidity;
  
  @Value("${security.jwt.issuer:hiresync}")
  private String issuer;
  
  @Value("${security.jwt.audience:hiresync-app}")
  private String audience;
  
  private final Key signingKey;

  public JwtUtil(Key jwtSigningKey) {
    this.signingKey = jwtSigningKey;
    logger.info("JwtUtil initialized with signing key");
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    try {
      return Jwts.parserBuilder()
          .setSigningKey(signingKey)
          .build()
          .parseClaimsJws(token)
          .getBody();
    } catch (JwtException e) {
      logger.debug("Failed to parse JWT token", e);
      throw e;
    }
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();

    if (userDetails instanceof UserDetailsImpl) {
      claims.put("userId", ((UserDetailsImpl) userDetails).getId());
    }

    return createToken(claims, userDetails.getUsername());
  }

  private String createToken(Map<String, Object> claims, String subject) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + tokenValidity);
    
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(now)
        .setExpiration(validity)
        .setIssuer(issuer)
        .setAudience(audience)
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public Boolean validateToken(String token, UserDetails userDetails) {
    try {
      final String username = extractUsername(token);
      return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    } catch (JwtException e) {
      logger.debug("Invalid JWT token", e);
      return false;
    }
  }
}
