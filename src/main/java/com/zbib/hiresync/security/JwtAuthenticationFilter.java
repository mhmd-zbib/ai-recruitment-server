package com.zbib.hiresync.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Filter that authenticates users via JWT tokens
 */
@Component
@Order(10)
public class JwtAuthenticationFilter implements Filter {
    private static final Logger log = LogManager.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();
        
        try {
            authenticateRequest(httpRequest);
            log.debug("Authentication filter processed request: {}", requestUri);
        } catch (Exception e) {
            log.error("Authentication error for {}: {}", requestUri, e.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        chain.doFilter(request, response);
    }
    
    private void authenticateRequest(HttpServletRequest request) {
        String jwt = resolveToken(request);
        
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Authentication auth = tokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("User '{}' authenticated for URI: {}", 
                auth.getName(), request.getRequestURI());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}