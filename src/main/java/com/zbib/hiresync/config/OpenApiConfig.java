package com.zbib.hiresync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configuration class for OpenAPI/Swagger documentation.
 */
@Configuration
public class OpenApiConfig {

  @Value("${spring.application.name}")
  private String applicationName;

  private final Environment environment;

  public OpenApiConfig(Environment environment) {
    this.environment = environment;
  }

  /**
   * Creates and configures the OpenAPI documentation.
   *
   * @return the configured OpenAPI instance
   */
  @Bean
  public OpenAPI openAPI() {
    boolean isProdProfile = isActiveProfile("prod");
    boolean isDevProfile = isActiveProfile("dev");
    boolean isLocalProfile = isActiveProfile("local");

    OpenAPI openAPI = new OpenAPI();

    // Create base information for all environments
    Info info = createBaseInfo();

    // Apply profile-specific configurations
    if (isProdProfile) {
      // For prod: Documentation is not shown (controlled in application-prod.yaml)
      info.title(applicationName + " - Production")
          .description("Production API - Documentation disabled");
    } else if (isDevProfile) {
      // For dev: Documentation with auth required
      info.title(applicationName + " - Development")
          .description("Development API with authentication required");

      // Add security requirement for dev
      openAPI
          .components(createSecurityComponents())
          .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    } else if (isLocalProfile) {
      // For local: Documentation with no auth
      info.title(applicationName + " - Local Development")
          .description("Local development API with unrestricted documentation access");

      // No security requirements for local
    }

    return openAPI.info(info);
  }

  private boolean isActiveProfile(String profileName) {
    for (String profile : environment.getActiveProfiles()) {
      if (profileName.equals(profile)) {
        return true;
      }
    }
    return environment.getDefaultProfiles().length == 0 && "local".equals(profileName);
  }

  private Info createBaseInfo() {
    return new Info()
        .version("1.0.0")
        .contact(
            new Contact()
                .name("HireSync Team")
                .email("info@hiresync.com")
                .url("https://hiresync.com"))
        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT"));
  }

  private Components createSecurityComponents() {
    return new Components()
        .addSecuritySchemes(
            "bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization"));
  }
}
