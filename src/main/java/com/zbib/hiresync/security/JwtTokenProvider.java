package com.zbib.hiresync.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger log = LogManager.getLogger(JwtTokenProvider.class);
    private static final String AUTHORITIES_KEY = "auth";

    private final SecretKey secretKey;
    private final long tokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;
    private final String issuer;
    private final String audience;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long tokenValidityInMilliseconds,
            @Value("${jwt.refresh-expiration}") long refreshTokenValidityInMilliseconds,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("Secret key is less than 256 bits. Consider using a stronger key.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);
        String tokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .setId(tokenId)
                .setIssuedAt(now)
                .setExpiration(validity)
                .setIssuer(issuer)
                .setAudience(audience)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Authentication authentication) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);
        String tokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .setSubject(authentication.getName())
                .setId(tokenId)
                .setIssuedAt(now)
                .setExpiration(validity)
                .setIssuer(issuer)
                .setAudience(audience)
                .signWith(secretKey)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        try {
            Claims claims = extractClaims(token);

            Collection<? extends GrantedAuthority> authorities = Arrays
                    .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                    .filter(auth -> !auth.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            User principal = new User(claims.getSubject(), "", authorities);

            return new UsernamePasswordAuthenticationToken(principal, token, authorities);
        } catch (Exception e) {
            log.error("Failed to get authentication from token: {}", e.getMessage());
            throw e;
        }
    }

    public String getEmailFromToken(String token) {
        return extractClaims(token).getSubject();
    }

    public Collection<GrantedAuthority> getAuthorities(String token) {
        Claims claims = extractClaims(token);
        
        if (claims.get(AUTHORITIES_KEY) != null) {
            return Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                    .filter(auth -> !auth.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        
        // Default authority if none specified in token
        return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to extract claims from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Calculates SHA-256 hash of the token for stateless validation.
     * This allows validating tokens without requiring a database lookup.
     * 
     * @param token The JWT token to hash
     * @return Base64 encoded SHA-256 hash of the token
     */
    public String calculateTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to calculate token hash: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifies if a token hash matches the provided token.
     * 
     * @param token The JWT token to verify
     * @param storedHash The previously stored hash to compare against
     * @return true if the calculated hash matches the stored hash
     */
    public boolean verifyTokenHash(String token, String storedHash) {
        String calculatedHash = calculateTokenHash(token);
        return calculatedHash != null && calculatedHash.equals(storedHash);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Validate token is not expired
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                log.error("JWT token is expired");
                return false;
            }

            // Validate issuer
            if (issuer != null && !issuer.equals(claims.getIssuer())) {
                log.error("JWT issuer is invalid");
                return false;
            }

            // Validate audience
            if (audience != null && !audience.equals(claims.getAudience())) {
                log.error("JWT audience is invalid");
                return false;
            }

            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    public long getTokenValidityInMilliseconds() {
        return tokenValidityInMilliseconds;
    }
}