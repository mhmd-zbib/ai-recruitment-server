package com.zbib.hiresync.logging;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Default implementation of UserIdentifierProvider that extracts
 * user identity from Spring Security's SecurityContextHolder.
 */
@Component
public class SecurityContextUserIdentifierProvider implements UserIdentifierProvider {
    private static final String ANONYMOUS_USER = "anonymousUser";

    @Override
    public String getCurrentUserId() {
        Authentication authentication = getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
    
    @Override
    public String getUserContext() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        
        return roles.isEmpty() ? null : roles;
    }
    
    private Authentication getAuthentication() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                    !ANONYMOUS_USER.equals(authentication.getPrincipal().toString())) {
                return authentication;
            }
        } catch (Exception e) {
            // Silently handle exceptions in security context access
        }
        return null;
    }
} 