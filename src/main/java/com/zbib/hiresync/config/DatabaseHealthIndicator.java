package com.zbib.hiresync.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "prod"})
public class DatabaseHealthIndicator implements HealthIndicator {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseHealthIndicator.class);

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
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      return Health.up()
          .withDetail("database", extractDatabaseName(datasourceUrl))
          .withDetail("status", "Available")
          .build();
    } catch (CannotGetJdbcConnectionException e) {
      LOGGER.error("Database connection failed", e);
      return Health.down()
          .withException(e)
          .withDetail("error", "Database connection failed")
          .build();
    } catch (DataAccessException e) {
      LOGGER.error("Database access error", e);
      return Health.down()
          .withException(e)
          .withDetail("error", "Database access error")
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
