package com.zbib.hiresync.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that authenticates users via JWT tokens
 */
@Component
@Order(10)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LogManager.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String jwt = resolveToken(request);

        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Authentication auth = tokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("User '{}' authenticated for URI: {}",
                    auth.getName(), request.getRequestURI());
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Check if the token length is sufficient before extracting
            if (bearerToken.length() > 7) {
                return bearerToken.substring(7);
            } else {
                log.warn("Authorization header has 'Bearer ' prefix but no token");
                return null;
            }
        }
        return null;
    }
}