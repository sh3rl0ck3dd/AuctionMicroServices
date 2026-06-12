package com.example.notificationservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class NotificationSseController {

  private static final Logger log = LoggerFactory.getLogger(NotificationSseController.class);

  private final SseRegistry registry;

  public NotificationSseController(SseRegistry registry) {
    this.registry = registry;
  }

  @GetMapping("/api/notifications/auctions/{auctionId}/stream")
  public SseEmitter stream(@PathVariable String auctionId) {
    log.info("SSE stream requested for auction {}", auctionId);
    SseEmitter emitter = new SseEmitter(300_000L);
    registry.register(auctionId, emitter);
    return emitter;
  }
}
