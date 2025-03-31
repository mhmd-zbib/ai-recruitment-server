package com.zbib.hiresync.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class OpenApiConfig {

  @Value("${spring.application.name}")
  private String applicationName;

  private final Environment environment;

  public OpenApiConfig(Environment environment) {
    this.environment = environment;
  }

  @Bean
  public OpenAPI openAPI() {
    // Basic configuration for all environments
    OpenAPI openAPI =
        new OpenAPI()
            .components(
                new Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .in(SecurityScheme.In.HEADER)
                            .name("Authorization")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));

    // Determine which profile is active
    boolean isProdProfile = false;
    for (String profile : environment.getActiveProfiles()) {
      if ("prod".equals(profile)) {
        isProdProfile = true;
        break;
      }
    }

    // Set title and description based on active profile
    if (isProdProfile) {
      openAPI.info(
          new Info()
              .title(applicationName + " - Production")
              .version("1.0.0")
              .description("Production API with authentication required for most endpoints")
              .contact(
                  new Contact()
                      .name("HireSync Team")
                      .email("info@hiresync.com")
                      .url("https://hiresync.com"))
              .license(
                  new License().name("MIT License").url("https://opensource.org/licenses/MIT")));
    } else {
      openAPI.info(
          new Info()
              .title(applicationName + " - Development")
              .version("1.0.0")
              .description("Development API Documentation with all endpoints")
              .contact(
                  new Contact()
                      .name("HireSync Team")
                      .email("info@hiresync.com")
                      .url("https://hiresync.com"))
              .license(
                  new License().name("MIT License").url("https://opensource.org/licenses/MIT")));
    }

    return openAPI;
  }
}
