package com.example.biddingservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class BidEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(BidEventPublisher.class);
  private static final String TOPIC = "bid-events";

  private final Optional<KafkaTemplate<String, String>> kafkaTemplate;
  private final ObjectMapper objectMapper;

  public BidEventPublisher(Optional<KafkaTemplate<String, String>> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @PostConstruct
  void init() {
    if (kafkaTemplate.isPresent()) {
      log.info("BidEventPublisher initialized — Kafka events will be published");
    } else {
      log.warn("BidEventPublisher initialized without KafkaTemplate — events will not be published");
    }
  }

  public void bidAccepted(Bid bid) {
    publish("bid.accepted", bid);
  }

  public void bidRejected(Bid bid) {
    publish("bid.rejected", bid);
  }

  private void publish(String eventType, Bid bid) {
    if (kafkaTemplate.isEmpty()) {
      log.warn("Skipping event {} for bid {} — no KafkaTemplate available", eventType, bid.id());
      return;
    }

    BidEvent event =
        new BidEvent(
            eventType,
            bid.id(),
            bid.auctionId(),
            bid.bidderId(),
            bid.amount(),
            bid.status(),
            Instant.now());

    String key = bid.auctionId();
    String payload;
    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize event {} for bid {}", eventType, bid.id(), e);
      return;
    }

    CompletableFuture<SendResult<String, String>> future =
        kafkaTemplate.get().send(TOPIC, key, payload);

    future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            log.error("Failed to publish event {} for bid {}", eventType, bid.id(), ex);
          } else {
            log.debug(
                "Published event {} for bid {} to partition {} offset {}",
                eventType,
                bid.id(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });
  }
}
