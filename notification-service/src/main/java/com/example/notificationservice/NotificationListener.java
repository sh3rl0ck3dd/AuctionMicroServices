package com.example.notificationservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

  private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  public NotificationListener(ApplicationEventPublisher eventPublisher) {
    this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    this.eventPublisher = eventPublisher;
  }

  @KafkaListener(topics = "auction-events", groupId = "notification-service")
  public void onAuctionEvent(String message) {
    try {
      AuctionEvent event = objectMapper.readValue(message, AuctionEvent.class);
      handleAuctionEvent(event);
      publishEvent(event.auctionId(), event.eventType(), event);
    } catch (JsonProcessingException e) {
      log.error("Failed to deserialize auction event: {}", message, e);
    }
  }

  @KafkaListener(topics = "bid-events", groupId = "notification-service")
  public void onBidEvent(String message) {
    try {
      BidEvent event = objectMapper.readValue(message, BidEvent.class);
      handleBidEvent(event);
      publishEvent(event.auctionId(), event.eventType(), event);
    } catch (JsonProcessingException e) {
      log.error("Failed to deserialize bid event: {}", message, e);
    }
  }

  void handleAuctionEvent(AuctionEvent event) {
    switch (event.eventType()) {
      case "auction.created" ->
          log.info(
              "Auction {} created by {} (starting price: {})",
              event.auctionId(),
              event.sellerId(),
              event.startingPrice());
      case "auction.started" ->
          log.info(
              "Auction {} started (starting price: {})",
              event.auctionId(),
              event.startingPrice());
      case "auction.ended" ->
          log.info(
              "Auction {} ended (final price: {})",
              event.auctionId(),
              event.currentPrice());
      default ->
          log.debug(
              "Unknown auction event type: {} for auction {}",
              event.eventType(),
              event.auctionId());
    }
  }

  void handleBidEvent(BidEvent event) {
    switch (event.eventType()) {
      case "bid.accepted" ->
          log.info(
              "Auction {} has a new highest bid: {} by {}",
              event.auctionId(),
              event.amount(),
              event.bidderId());
      case "bid.rejected" ->
          log.info(
              "Bid rejected on auction {}: amount {} is too low",
              event.auctionId(),
              event.amount());
      default ->
          log.debug(
              "Unknown bid event type: {} for bid {}",
              event.eventType(),
              event.bidId());
    }
  }

  private void publishEvent(String auctionId, String eventType, Object event) {
    Map<String, Object> data = objectMapper.convertValue(event, Map.class);
    eventPublisher.publishEvent(new SseNotificationEvent(auctionId, eventType, data));
  }
}
