package com.zbib.hiresync.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** JWT authentication filter that processes incoming requests and validates JWT tokens. */
@Component
@RequiredArgsConstructor
@SuppressWarnings(
    "PMD.AvoidCatchingGenericException") // Needed for comprehensive security error handling
public final class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private final JwtUtil jwtUtil;
  private final UserDetailsService userDetailsService;

  /**
   * Processes the incoming request and validates the JWT token if present.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param filterChain the filter chain
   * @throws ServletException if a servlet error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doFilterInternal(
      @NonNull final HttpServletRequest request,
      @NonNull final HttpServletResponse response,
      @NonNull final FilterChain filterChain)
      throws ServletException, IOException {
    final String authHeader = request.getHeader("Authorization");

    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
      }

      final String jwt = authHeader.substring(7);
      if (jwt == null || jwt.isBlank()) {
        LOGGER.debug("Empty JWT token");
        filterChain.doFilter(request, response);
        return;
      }

      final String username = jwtUtil.extractUsername(jwt);
      if (username == null || username.isBlank()) {
        LOGGER.debug("No username in JWT token");
        filterChain.doFilter(request, response);
        return;
      }

      if (SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        if (userDetails != null && jwtUtil.validateToken(jwt, userDetails)) {
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
          LOGGER.debug("Authenticated user: {}", username);
        }
      }
    } catch (JwtException e) {
      LOGGER.debug("JWT token validation failed: {}", e.getMessage());
    } catch (UsernameNotFoundException e) {
      LOGGER.debug("User not found during authentication: {}", e.getMessage());
    } catch (BadCredentialsException e) {
      LOGGER.warn("Bad credentials during authentication: {}", e.getMessage());
    } catch (AuthenticationException e) {
      LOGGER.warn("Authentication failed: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      LOGGER.error("Invalid argument during authentication: {}", e.getMessage());
    } catch (SecurityException e) {
      LOGGER.error("Security violation during authentication: {}", e.getMessage());
    } catch (UnsupportedOperationException e) {
      LOGGER.error("Unsupported operation during authentication: {}", e.getMessage());
    } catch (IndexOutOfBoundsException e) {
      LOGGER.error("Index out of bounds during authentication: {}", e.getMessage());
    } catch (IllegalStateException e) {
      LOGGER.error("Illegal state during authentication: {}", e.getMessage());
    } catch (RuntimeException e) {
      // This handles any other runtime exceptions including NPE
      LOGGER.error("Unexpected error during authentication: {}", e.getMessage(), e);
    }

    filterChain.doFilter(request, response);
  }
}
