package com.zbib.hiresync.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
        tokenProvider = new JwtTokenProvider(
            secret, tokenValidity, refreshTokenValidity, issuer, audience);

        // Setup test authentication
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        User principal = new User("test@example.com", "password", authorities);
        authentication = new UsernamePasswordAuthenticationToken(principal, "password", authorities);
    }

    @Test
    @DisplayName("Create token should return a valid token")
    void createTokenShouldReturnValidToken() {
        // Act
        String token = tokenProvider.createToken(authentication);

        // Assert
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("test@example.com", tokenProvider.getEmailFromToken(token));
    }

    @Test
    @DisplayName("Create refresh token should return a valid refresh token")
    void createRefreshTokenShouldReturnValidToken() {
        // Act
        String refreshToken = tokenProvider.createRefreshToken(authentication);

        // Assert
        assertNotNull(refreshToken);
        assertTrue(tokenProvider.validateToken(refreshToken));
        assertEquals("test@example.com", tokenProvider.getEmailFromToken(refreshToken));
    }

    @Test
    @DisplayName("Get authentication from token should return valid authentication")
    void getAuthenticationFromTokenShouldReturnValidAuthentication() {
        // Arrange
        String token = tokenProvider.createToken(authentication);

        // Act
        Authentication result = tokenProvider.getAuthentication(token);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getName());
        assertTrue(result.isAuthenticated());
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("Get email from token should return correct email")
    void getEmailFromTokenShouldReturnCorrectEmail() {
        // Arrange
        String token = tokenProvider.createToken(authentication);

        // Act
        String email = tokenProvider.getEmailFromToken(token);

        // Assert
        assertEquals("test@example.com", email);
    }

    @Test
    @DisplayName("Get authorities from token should return correct authorities")
    void getAuthoritiesFromTokenShouldReturnCorrectAuthorities() {
        // Arrange
        String token = tokenProvider.createToken(authentication);

        // Act
        Collection<GrantedAuthority> authorities = tokenProvider.getAuthorities(token);

        // Assert
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("Validate token should return true for valid token")
    void validateTokenShouldReturnTrueForValidToken() {
        // Arrange
        String token = tokenProvider.createToken(authentication);

        // Act
        boolean valid = tokenProvider.validateToken(token);

        // Assert
        assertTrue(valid);
    }

    @Test
    @DisplayName("Validate token should return false for token with wrong signature")
    void validateTokenShouldReturnFalseForTokenWithWrongSignature() {
        // Arrange
        String token = tokenProvider.createToken(authentication);
        // Create another token provider with different key to simulate wrong signature
        JwtTokenProvider anotherProvider = new JwtTokenProvider(
            "differentSecretKeyThatIsAtLeast256BitsLongForHmacSha256", 
            tokenValidity, refreshTokenValidity, issuer, audience);
        String corruptedToken = anotherProvider.createToken(authentication);

        // Act
        boolean valid = tokenProvider.validateToken(corruptedToken);

        // Assert
        assertFalse(valid);
    }

    @Test
    @DisplayName("Validate token should return false for expired token")
    void validateTokenShouldReturnFalseForExpiredToken() {
        // Create a token provider with very short expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
            secret, 1, refreshTokenValidity, issuer, audience);
        
        // Create a token that will expire almost immediately
        String expiredToken = shortLivedProvider.createToken(authentication);
        
        // Wait briefly to ensure the token expires
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail("Test interrupted while waiting for token to expire");
        }
        
        // Act
        boolean valid = shortLivedProvider.validateToken(expiredToken);
        
        // Assert
        assertFalse(valid);
    }

    @Test
    @DisplayName("Validate token should return false for token with wrong issuer")
    void validateTokenShouldReturnFalseForTokenWithWrongIssuer() {
        // Create token provider with different issuer
        JwtTokenProvider differentIssuerProvider = new JwtTokenProvider(
            secret, tokenValidity, refreshTokenValidity, "different-issuer", audience);
        
        // Create token with different issuer
        String differentIssuerToken = differentIssuerProvider.createToken(authentication);
        
        // Validate using the original provider (with issuer "test-issuer")
        boolean valid = tokenProvider.validateToken(differentIssuerToken);
        
        // Assert
        assertFalse(valid);
    }

    @Test
    @DisplayName("Validate token should return false for token with wrong audience")
    void validateTokenShouldReturnFalseForTokenWithWrongAudience() {
        // Create token provider with different audience
        JwtTokenProvider differentAudienceProvider = new JwtTokenProvider(
            secret, tokenValidity, refreshTokenValidity, issuer, "different-audience");
        
        // Create token with different audience
        String differentAudienceToken = differentAudienceProvider.createToken(authentication);
        
        // Validate using the original provider (with audience "test-audience")
        boolean valid = tokenProvider.validateToken(differentAudienceToken);
        
        // Assert
        assertFalse(valid);
    }

    @Test
    @DisplayName("Calculate token hash should return consistent hash")
    void calculateTokenHashShouldReturnConsistentHash() {
        // Arrange
        String tokenData = "test-token-data";
        
        // Act
        String hash1 = tokenProvider.calculateTokenHash(tokenData);
        String hash2 = tokenProvider.calculateTokenHash(tokenData);
        
        // Assert
        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Verify token hash should return true for matching hash")
    void verifyTokenHashShouldReturnTrueForMatchingHash() {
        // Arrange
        String tokenData = "test-token-data";
        String hash = tokenProvider.calculateTokenHash(tokenData);
        
        // Act
        boolean verified = tokenProvider.verifyTokenHash(tokenData, hash);
        
        // Assert
        assertTrue(verified);
    }

    @Test
    @DisplayName("Verify token hash should return false for non-matching hash")
    void verifyTokenHashShouldReturnFalseForNonMatchingHash() {
        // Arrange
        String tokenData1 = "test-token-data-1";
        String tokenData2 = "test-token-data-2";
        String hash = tokenProvider.calculateTokenHash(tokenData1);
        
        // Act
        boolean verified = tokenProvider.verifyTokenHash(tokenData2, hash);
        
        // Assert
        assertFalse(verified);
    }

    @Test
    @DisplayName("Get token validity should return configured token validity")
    void getTokenValidityShouldReturnConfiguredTokenValidity() {
        // Act
        long result = tokenProvider.getTokenValidityInMilliseconds();
        
        // Assert
        assertEquals(tokenValidity, result);
    }
} 