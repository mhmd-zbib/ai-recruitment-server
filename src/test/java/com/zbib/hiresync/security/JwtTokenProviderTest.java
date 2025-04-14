package com.zbib.hiresync.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private String secret;
    private long tokenValidity;
    private long refreshTokenValidity;
    private String issuer;
    private String audience;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Setup test configuration
        secret = "testSecretKeyThatIsAtLeast256BitsLongForHmacSha256Algorithm";
        tokenValidity = 3600000; // 1 hour
        refreshTokenValidity = 86400000; // 1 day
        issuer = "test-issuer";
        audience = "test-audience";

        // Create the token provider with test configuration
        tokenProvider = new JwtTokenProvider(secret, tokenValidity, refreshTokenValidity, issuer, audience);

        // Setup test authentication
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        User principal = new User("test@example.com", "password", authorities);
        authentication = new UsernamePasswordAuthenticationToken(principal, "password", authorities);
    }

    @Test
    void shouldGenerateValidToken() {
        // Act
        String token = tokenProvider.createToken(authentication);

        // Assert
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        // Verify token claims
        Claims claims = tokenProvider.extractClaims(token);
        assertEquals("test@example.com", claims.getSubject());
        assertEquals(issuer, claims.getIssuer());
        assertEquals(audience, claims.getAudience());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getId());
        assertTrue(claims.getExpiration().after(new Date()));
        assertEquals("ROLE_USER", claims.get("auth"));
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        // Act
        String refreshToken = tokenProvider.createRefreshToken(authentication);

        // Assert
        assertNotNull(refreshToken);
        assertTrue(tokenProvider.validateToken(refreshToken));

        // Verify token claims
        Claims claims = tokenProvider.extractClaims(refreshToken);
        assertEquals("test@example.com", claims.getSubject());
        assertEquals(issuer, claims.getIssuer());
        assertEquals(audience, claims.getAudience());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getId());
        assertTrue(claims.getExpiration().after(new Date()));
        // Refresh token should not contain authorities
        assertNull(claims.get("auth"));
    }

    @Test
    void shouldReturnValidAuthentication() {
        // Arrange
        String token = tokenProvider.createToken(authentication);

        // Act
        Authentication result = tokenProvider.getAuthentication(token);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getName());
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
                .contains("ROLE_USER"));
    }

    @Test
    void shouldReturnCorrectEmail() {
        // Arrange
        String token = tokenProvider.createToken(authentication);

        // Act
        String email = tokenProvider.getEmailFromToken(token);

        // Assert
        assertEquals("test@example.com", email);
    }

    @Test
    void shouldReturnCorrectAuthorities() {
        // Arrange
        String token = tokenProvider.createToken(authentication);

        // Act
        Collection<GrantedAuthority> authorities = tokenProvider.getAuthorities(token);

        // Assert
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
    }

    @Test
    void shouldReturnDefaultRoleWhenNoAuthoritiesInToken() {
        // Arrange
        String refreshToken = tokenProvider.createRefreshToken(authentication);

        // Act
        Collection<GrantedAuthority> authorities = tokenProvider.getAuthorities(refreshToken);

        // Assert
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
    }

    @Test
    void shouldReturnFalseForInvalidSignature() {
        // Arrange
        String token = tokenProvider.createToken(authentication);
        // Corrupt the token by changing a character
        token = token.substring(0, token.length() - 5) + "12345";

        // Act & Assert
        assertFalse(tokenProvider.validateToken(token));
    }

    @Test
    void shouldReturnFalseForExpiredToken() {
        // Arrange
        // Create a token provider with very short validity
        JwtTokenProvider shortTokenProvider = new JwtTokenProvider(
                secret, 1, refreshTokenValidity, issuer, audience); // 1ms validity

        String token = shortTokenProvider.createToken(authentication);

        // Wait for the token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            fail("Thread interrupted during sleep");
        }

        // Act & Assert
        assertFalse(shortTokenProvider.validateToken(token));
    }

    @Test
    void shouldReturnFalseForInvalidIssuer() {
        // Arrange
        String token = tokenProvider.createToken(authentication);
        
        JwtTokenProvider differentIssuerProvider = new JwtTokenProvider(
                secret, tokenValidity, refreshTokenValidity, "different-issuer", audience);

        // Act & Assert
        assertFalse(differentIssuerProvider.validateToken(token));
    }

    @Test
    void shouldReturnFalseForInvalidAudience() {
        // Arrange
        String token = tokenProvider.createToken(authentication);
        
        JwtTokenProvider differentAudienceProvider = new JwtTokenProvider(
                secret, tokenValidity, refreshTokenValidity, issuer, "different-audience");

        // Act & Assert
        assertFalse(differentAudienceProvider.validateToken(token));
    }

    @Test
    void shouldReturnConfiguredTokenValidity() {
        // Act
        long result = tokenProvider.getTokenValidityInMilliseconds();

        // Assert
        assertEquals(tokenValidity, result);
    }
} 