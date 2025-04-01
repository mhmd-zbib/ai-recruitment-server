package com.zbib.hiresync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "prod"})
public class DatabaseHealthIndicator implements HealthIndicator {

  private final JdbcTemplate jdbcTemplate;

  @Value("${spring.datasource.url:}")
  private String datasourceUrl;

  @Value("${spring.datasource.username:}")
  private String datasourceUsername;

  @PostConstruct
  public void init() {
    log.info("Database connection configuration:");
    log.info("URL: {}", maskConnectionString(datasourceUrl));
    log.info("Username: {}", datasourceUsername);
    log.info("Connection pool: HikariCP");
  }

  @Override
  public Health health() {
    try {
      int result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      if (result == 1) {
        return Health.up()
            .withDetail("database", extractDatabaseName(datasourceUrl))
            .withDetail("status", "Available")
            .build();
      } else {
        return Health.down()
            .withDetail("database", extractDatabaseName(datasourceUrl))
            .withDetail("status", "Unexpected response")
            .build();
      }
    } catch (Exception e) {
      return Health.down()
          .withDetail("database", extractDatabaseName(datasourceUrl))
          .withDetail("status", "Unavailable")
          .withDetail("error", e.getMessage())
          .build();
    }
  }

  private String extractDatabaseName(String url) {
    if (url == null || url.isEmpty()) {
      return "unknown";
    }

    try {
      // Extract the database name from JDBC URL
      // Format: jdbc:postgresql://host:port/dbname
      String[] parts = url.split("/");
      return parts[parts.length - 1].split("\\?")[0];
    } catch (Exception e) {
      return "unknown";
    }
  }

  private String maskConnectionString(String url) {
    if (url == null || url.isEmpty()) {
      return "unknown";
    }

    // Mask password if present in URL
    if (url.contains("@")) {
      int startIndex = url.indexOf("://") + 3;
      int endIndex = url.indexOf("@");
      String credentials = url.substring(startIndex, endIndex);

      if (credentials.contains(":")) {
        String username = credentials.split(":")[0];
        return url.replace(credentials, username + ":********");
      }
    }

    return url;
  }
}
