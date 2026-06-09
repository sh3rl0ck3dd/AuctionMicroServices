package com.example.notificationservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationSseControllerTest {

  @LocalServerPort
  private int port;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private SseRegistry registry;

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void shouldPublishAuctionEventToRegistry() {
    SseEmitter emitter = new SseEmitter(5_000L);
    registry.register("auction-1", emitter);

    AuctionEvent auctionEvent =
        new AuctionEvent(
            "auction.started",
            "auction-1",
            "Test Auction",
            "seller-5",
            new BigDecimal("100"),
            new BigDecimal("100"),
            "ACTIVE",
            Instant.now());

    Map<String, Object> data = objectMapper.convertValue(auctionEvent, Map.class);
    eventPublisher.publishEvent(
        new SseNotificationEvent("auction-1", "auction.started", data));
  }

  @Test
  void shouldPublishBidEventToRegistry() {
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
    eventPublisher.publishEvent(
        new SseNotificationEvent("auction-1", "bid.accepted", data));
  }

  @Test
  void shouldNotSendToDifferentAuction() {
    SseEmitter emitter1 = new SseEmitter(5_000L);
    SseEmitter emitter2 = new SseEmitter(5_000L);
    registry.register("auction-1", emitter1);
    registry.register("auction-2", emitter2);

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
    eventPublisher.publishEvent(
        new SseNotificationEvent("auction-1", "bid.accepted", data));
  }
}
