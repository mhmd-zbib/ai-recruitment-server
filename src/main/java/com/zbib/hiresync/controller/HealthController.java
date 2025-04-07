package com.zbib.hiresync.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for health check endpoints. Provides system status information. */
@RestController
@RequestMapping("/health")
public class HealthController {

  /**
   * Performs a health check of the application.
   *
   * @return health status information with HTTP status 200 (OK)
   */
  @GetMapping
  public ResponseEntity<Map<String, Object>> healthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("timestamp", LocalDateTime.now().toString());
    response.put("service", "HireSync API");

    String activeProfile = System.getProperty("spring.profiles.active", "default");
    response.put("environment", activeProfile);

    return ResponseEntity.ok(response);
  }
}
