package com.zbib.hiresync.config;

import com.zbib.hiresync.security.JwtAuthenticationFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Security configuration class for the application. */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

  private final UserDetailsService userDetailsService;
  private final JwtAuthenticationFilter jwtRequestFilter;
  private final Environment environment;
  private final PasswordEncoder passwordEncoder;

  private static final String[] PUBLIC_ENDPOINTS = {
    "/api/auth/**",
    "/api/health/**",
    "/api/v3/api-docs/**",
    "/api/swagger-ui/**",
    "/api/actuator/health",
    "/api/actuator/info"
  };

  @Bean
  public HttpStatusEntryPoint authenticationEntryPoint() {
    return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    AccessDeniedHandlerImpl handler = new AccessDeniedHandlerImpl();
    handler.setErrorPage("/api/error/403");
    return handler;
  }

  /**
   * Configures the security filter chain.
   *
   * @param http the HttpSecurity to configure
   * @return the configured SecurityFilterChain
   */
  @SuppressWarnings("PMD.SignatureDeclareThrowsException") // Required by Spring Security
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    LOGGER.info("Configuring security filter chain");

    try {
      return http.csrf(AbstractHttpConfigurer::disable)
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .sessionManagement(
              session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(
              auth ->
                  auth.requestMatchers(PUBLIC_ENDPOINTS)
                      .permitAll()
                      .requestMatchers(HttpMethod.OPTIONS, "/**")
                      .permitAll()
                      .anyRequest()
                      .authenticated())
          .exceptionHandling(
              exception ->
                  exception
                      .authenticationEntryPoint(authenticationEntryPoint())
                      .accessDeniedHandler(accessDeniedHandler()))
          .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
          .build();
    } catch (SecurityException | IllegalArgumentException | IllegalStateException e) {
      LOGGER.error("Failed to configure security filter chain due to security violation", e);
      throw new SecurityConfigException("Failed to configure security filter chain", e);
    } catch (UnsupportedOperationException e) {
      LOGGER.error("Operation not supported in security configuration", e);
      throw new SecurityConfigException("Operation not supported in security configuration", e);
    } catch (javax.naming.ConfigurationException e) {
      LOGGER.error("Configuration error in security setup", e);
      throw new SecurityConfigException("Configuration error in security setup", e);
    } catch (javax.security.auth.login.LoginException e) {
      LOGGER.error("Authentication configuration error", e);
      throw new SecurityConfigException("Authentication configuration error", e);
    }
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    String[] allowedOrigins =
        environment.getProperty("security.cors.allowed-origins", String[].class);
    if (allowedOrigins == null || allowedOrigins.length == 0) {
      // Default for local development only - should be overridden in production
      configuration.setAllowedOrigins(List.of("http://localhost:3000"));
      LOGGER.warn("No CORS allowed origins configured, using default development setting");
    } else {
      configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
    }

    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  /**
   * Creates an authentication manager bean.
   *
   * @param config the authentication configuration
   * @return the authentication manager
   * @throws java.io.IOException if I/O operations fail
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException") // Needed for Spring Security
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws java.io.IOException {
    try {
      return config.getAuthenticationManager();
    } catch (SecurityException | IllegalArgumentException | IllegalStateException e) {
      LOGGER.error("Failed to create authentication manager due to security violation", e);
      throw new SecurityConfigException("Failed to create authentication manager", e);
    } catch (UnsupportedOperationException e) {
      LOGGER.error("Operation not supported in authentication manager creation", e);
      throw new SecurityConfigException(
          "Operation not supported in authentication manager creation", e);
    } catch (java.io.IOException e) {
      LOGGER.error("I/O error during authentication manager creation", e);
      throw e;
    } catch (org.springframework.security.core.AuthenticationException e) {
      LOGGER.error("Authentication error during manager creation", e);
      throw new SecurityConfigException("Authentication error during manager creation", e);
    } catch (org.springframework.beans.factory.BeanCreationException e) {
      LOGGER.error("Bean creation error during authentication manager creation", e);
      throw new SecurityConfigException("Bean creation error", e);
    } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
      LOGGER.error("Required bean not found for authentication manager", e);
      throw new SecurityConfigException("Required bean not found", e);
    } catch (Exception e) {
      LOGGER.error("Unexpected error during authentication manager creation", e);
      throw new SecurityConfigException("Unexpected error in authentication manager creation", e);
    }
  }

  /** Custom runtime exception for security configuration errors. */
  public static class SecurityConfigException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SecurityConfigException(String message) {
      super(message);
    }

    public SecurityConfigException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
