package com.example.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseRegistryTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void shouldSendEventToRegisteredEmitter() {
    SseRegistry registry = new SseRegistry();
    SseEmitter emitter = new SseEmitter(5_000L);

    registry.register("auction-1", emitter);

    BidEvent bidEvent =
        new BidEvent(
            "bid.accepted",
            "bid-1",
            "auction-1",
            "bidder-3",
            new BigDecimal("80"),
            "ACTIVE",
            Instant.now());

    Map<String, Object> data = objectMapper.convertValue(bidEvent, Map.class);

    registry.onSseNotificationEvent(
        new SseNotificationEvent("auction-1", "bid.accepted", data));
  }

  @Test
  void shouldNotSendToDifferentAuction() {
    SseRegistry registry = new SseRegistry();
    SseEmitter emitter = new SseEmitter(1_000L);

    registry.register("auction-1", emitter);

    BidEvent bidEvent =
        new BidEvent(
            "bid.accepted",
            "bid-1",
            "auction-2",
            "bidder-3",
            new BigDecimal("80"),
            "ACTIVE",
            Instant.now());

    Map<String, Object> data = objectMapper.convertValue(bidEvent, Map.class);

    registry.onSseNotificationEvent(
        new SseNotificationEvent("auction-2", "bid.accepted", data));
  }

  @Test
  void shouldHandleNoEmitters() {
    SseRegistry registry = new SseRegistry();

    Map<String, Object> data = Map.of("key", "value");

    registry.onSseNotificationEvent(
        new SseNotificationEvent("auction-1", "bid.accepted", data));
  }
}
