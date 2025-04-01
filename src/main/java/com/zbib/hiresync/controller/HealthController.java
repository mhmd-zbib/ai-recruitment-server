package com.zbib.hiresync.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

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
