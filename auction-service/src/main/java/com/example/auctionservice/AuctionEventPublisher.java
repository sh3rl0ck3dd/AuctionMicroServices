package com.example.auctionservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(KafkaTemplate.class)
public class AuctionEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(AuctionEventPublisher.class);
  private static final String TOPIC = "auction-events";

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  public AuctionEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  public void auctionCreated(Auction auction) {
    publish("auction.created", auction);
  }

  public void auctionStarted(Auction auction) {
    publish("auction.started", auction);
  }

  public void auctionEnded(Auction auction) {
    publish("auction.ended", auction);
  }

  private void publish(String eventType, Auction auction) {
    AuctionEvent event =
        new AuctionEvent(
            eventType,
            auction.id(),
            auction.title(),
            auction.sellerId(),
            auction.startingPrice(),
            auction.currentPrice(),
            auction.status(),
            Instant.now());

    String key = auction.id();
    String payload;
    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize event {} for auction {}", eventType, auction.id(), e);
      return;
    }

    CompletableFuture<SendResult<String, String>> future =
        kafkaTemplate.send(TOPIC, key, payload);

    future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            log.error("Failed to publish event {} for auction {}", eventType, auction.id(), ex);
          } else {
            log.debug(
                "Published event {} for auction {} to partition {} offset {}",
                eventType,
                auction.id(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });
  }
}
