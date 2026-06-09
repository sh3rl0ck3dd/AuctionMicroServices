package com.example.notificationservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class NotificationSseController {

  private final SseRegistry registry;

  public NotificationSseController(SseRegistry registry) {
    this.registry = registry;
  }

  @GetMapping("/api/notifications/auctions/{auctionId}/stream")
  public SseEmitter stream(@PathVariable String auctionId) {
    SseEmitter emitter = new SseEmitter(300_000L);
    registry.register(auctionId, emitter);
    return emitter;
  }
}
