package com.example.notificationservice;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationListenerTest {

  private NotificationListener listener;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    listener = new NotificationListener();
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @Test
  void shouldHandleAuctionCreated() throws JsonProcessingException {
    AuctionEvent event =
        new AuctionEvent(
            "auction.created",
            "auction-1",
            "Test Auction",
            "seller-5",
            new BigDecimal("100"),
            new BigDecimal("100"),
            "DRAFT",
            Instant.now());

    String json = objectMapper.writeValueAsString(event);

    assertThatCode(() -> listener.onAuctionEvent(json)).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleAuctionStarted() throws JsonProcessingException {
    AuctionEvent event =
        new AuctionEvent(
            "auction.started",
            "auction-1",
            "Test Auction",
            "seller-5",
            new BigDecimal("100"),
            new BigDecimal("100"),
            "ACTIVE",
            Instant.now());

    String json = objectMapper.writeValueAsString(event);

    assertThatCode(() -> listener.onAuctionEvent(json)).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleAuctionEnded() throws JsonProcessingException {
    AuctionEvent event =
        new AuctionEvent(
            "auction.ended",
            "auction-1",
            "Test Auction",
            "seller-5",
            new BigDecimal("100"),
            new BigDecimal("150"),
            "ENDED",
            Instant.now());

    String json = objectMapper.writeValueAsString(event);

    assertThatCode(() -> listener.onAuctionEvent(json)).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleBidAccepted() throws JsonProcessingException {
    BidEvent event =
        new BidEvent(
            "bid.accepted",
            "bid-1",
            "auction-1",
            "bidder-3",
            new BigDecimal("80"),
            "ACTIVE",
            Instant.now());

    String json = objectMapper.writeValueAsString(event);

    assertThatCode(() -> listener.onBidEvent(json)).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleBidRejected() throws JsonProcessingException {
    BidEvent event =
        new BidEvent(
            "bid.rejected",
            "bid-2",
            "auction-1",
            "bidder-4",
            new BigDecimal("50"),
            "OUTBID",
            Instant.now());

    String json = objectMapper.writeValueAsString(event);

    assertThatCode(() -> listener.onBidEvent(json)).doesNotThrowAnyException();
  }

  @Test
  void shouldNotThrowOnMalformedJson() {
    assertThatCode(() -> listener.onAuctionEvent("{not valid json"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldNotThrowOnUnknownEventType() throws JsonProcessingException {
    AuctionEvent event =
        new AuctionEvent(
            "auction.unknown",
            "auction-1",
            "Test",
            "seller-5",
            BigDecimal.ONE,
            BigDecimal.ONE,
            "DRAFT",
            Instant.now());

    String json = objectMapper.writeValueAsString(event);

    assertThatCode(() -> listener.onAuctionEvent(json)).doesNotThrowAnyException();
  }
}
