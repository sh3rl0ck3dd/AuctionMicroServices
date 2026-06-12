package com.example.notificationservice;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  private static final Logger log = LoggerFactory.getLogger(HealthController.class);

  @GetMapping("/api/health")
  public Map<String, String> health() {
    log.debug("Health check requested");
    return Map.of("status", "UP", "service", "notification-service");
  }
}
