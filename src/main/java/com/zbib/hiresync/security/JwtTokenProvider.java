package com.zbib.hiresync.security;

import com.zbib.hiresync.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

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
import java.util.Arrays;
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
        } catch (ExpiredJwtException e) {
            throw AuthException.tokenExpired();
        } catch (Exception e) {
            throw AuthException.tokenExpired();
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

        return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw AuthException.tokenExpired();
        } catch (Exception e) {
            throw AuthException.tokenExpired();
        }
    }

    public long getTokenValidityInMilliseconds() {
        return tokenValidityInMilliseconds;
    }

    public boolean validateToken(String token) {
        if (token == null) {
            return false;
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }
}