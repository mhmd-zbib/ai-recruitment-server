package com.zbib.hiresync.logging;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that populates the ThreadContext with request information 
 * and user context for structured logging.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingContextFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_CORRELATION_ID = "X-Correlation-ID";
    private static final String ANONYMOUS_USER = "anonymous";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            populateLoggingContext(request);
            addCorrelationIdToResponse(response);
            filterChain.doFilter(request, response);
        } finally {
            ThreadContext.clearAll();
        }
    }

    private void populateLoggingContext(HttpServletRequest request) {
        captureRequestIdentifiers(request);
        captureRequestMetadata(request);
        captureClientInformation(request);
        captureSessionInformation(request);
        captureUserContext();
    }

    private void captureRequestIdentifiers(HttpServletRequest request) {
        captureCorrelationId(request);
        ThreadContext.put(ContextKeys.REQUEST_ID, UUID.randomUUID().toString());
    }

    private void captureRequestMetadata(HttpServletRequest request) {
        ThreadContext.put(ContextKeys.REQUEST_PATH, request.getRequestURI());
        ThreadContext.put(ContextKeys.REQUEST_METHOD, request.getMethod());
    }

    private void captureCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(X_CORRELATION_ID);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = generateCorrelationId();
        }
        ThreadContext.put(ContextKeys.CORRELATION_ID, correlationId);
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private void captureClientInformation(HttpServletRequest request) {
        ThreadContext.put(ContextKeys.SOURCE_IP, resolveClientIpAddress(request));
        captureUserAgent(request);
    }

    private String resolveClientIpAddress(HttpServletRequest request) {
        String forwardedHeader = request.getHeader(X_FORWARDED_FOR);
        return forwardedHeader != null ? extractFirstIpFromHeader(forwardedHeader) : request.getRemoteAddr();
    }

    private String extractFirstIpFromHeader(String forwardedHeader) {
        String[] addresses = forwardedHeader.split(",");
        return addresses.length > 0 ? addresses[0].trim() : "";
    }

    private void captureUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && !userAgent.isEmpty()) {
            ThreadContext.put(ContextKeys.USER_AGENT, userAgent);
        }
    }

    private void captureSessionInformation(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            ThreadContext.put(ContextKeys.SESSION_ID, request.getSession().getId());
        }
    }

    private void captureUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            captureAuthenticatedUserContext(authentication);
        } else {
            ThreadContext.put(ContextKeys.USER_ID, ANONYMOUS_USER);
        }
    }

    private void captureAuthenticatedUserContext(Authentication authentication) {
        captureUserId(authentication);
        captureUserRoles(authentication);
    }

    private void captureUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            ThreadContext.put(ContextKeys.USER_ID, ((UserDetails) principal).getUsername());
        } else {
            ThreadContext.put(ContextKeys.USER_ID, principal.toString());
        }
    }

    private void captureUserRoles(Authentication authentication) {
        List<String> roles = authentication.getAuthorities() != null ?
            authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList()) :
            Collections.emptyList();
        
        ThreadContext.put(ContextKeys.ROLES, String.join(",", roles));
    }

    private void addCorrelationIdToResponse(HttpServletResponse response) {
        String correlationId = ThreadContext.get(ContextKeys.CORRELATION_ID);
        if (correlationId != null) {
            response.addHeader(X_CORRELATION_ID, correlationId);
        }
    }
} 