package com.zbib.hiresync.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Filter that establishes consistent logging context for all requests.
 * Populates MDC with request information, correlation IDs, and user details.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MdcContextFilter extends OncePerRequestFilter {
    
    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_REAL_IP = "X-Real-IP";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_SESSION_ID = "X-Session-ID";
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            populateMdcFromRequest(request);
            addResponseHeaders(response);
            filterChain.doFilter(request, response);
        } finally {
            MdcContext.clear();
        }
    }
    
    private void populateMdcFromRequest(HttpServletRequest request) {
        // Request tracking
        addCorrelationId(request);
        addRequestId();
        
        // Request metadata
        MdcContext.put(ContextKeys.REQUEST_PATH, request.getRequestURI());
        MdcContext.put(ContextKeys.REQUEST_METHOD, request.getMethod());
        
        // Client information
        MdcContext.put(ContextKeys.SOURCE_IP, extractClientIp(request));
        addHeaderIfPresent(request, HEADER_USER_AGENT, ContextKeys.USER_AGENT);
        
        // Session tracking
        String sessionId = extractSessionId(request);
        if (sessionId != null && !sessionId.isEmpty()) {
            MdcContext.put(ContextKeys.SESSION_ID, sessionId);
        }
        
        // User information
        addUserContext();
    }
    
    private void addCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(HEADER_CORRELATION_ID);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }
        MdcContext.put(ContextKeys.CORRELATION_ID, correlationId);
    }
    
    private void addRequestId() {
        MdcContext.put(ContextKeys.REQUEST_ID, UUID.randomUUID().toString());
    }
    
    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        
        String realIp = request.getHeader(HEADER_REAL_IP);
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private void addHeaderIfPresent(HttpServletRequest request, String headerName, String contextKey) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isEmpty()) {
            MdcContext.put(contextKey, value);
        }
    }
    
    private String extractSessionId(HttpServletRequest request) {
        String headerSessionId = request.getHeader(HEADER_SESSION_ID);
        if (headerSessionId != null && !headerSessionId.isEmpty()) {
            return headerSessionId;
        }
        
        return request.getSession(false) != null 
                ? request.getSession().getId() 
                : null;
    }
    
    private void addUserContext() {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return;
        }
        
        // Add user ID
        MdcContext.put(ContextKeys.USER_ID, auth.getName());
        
        // Add user roles if available
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        
        if (!roles.isEmpty()) {
            MdcContext.put(ContextKeys.ROLES, roles);
        }
    }
    
    private Authentication getAuthentication() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && 
                    !ANONYMOUS_USER.equals(auth.getPrincipal().toString())) {
                return auth;
            }
        } catch (Exception e) {
            // Silently handle security context exceptions
        }
        return null;
    }
    
    private void addResponseHeaders(HttpServletResponse response) {
        String correlationId = MdcContext.get(ContextKeys.CORRELATION_ID);
        if (correlationId != null && !correlationId.isEmpty()) {
            response.setHeader(HEADER_CORRELATION_ID, correlationId);
        }
    }
} 