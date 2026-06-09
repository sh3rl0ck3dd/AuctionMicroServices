package com.example.biddingservice;

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
public class BidEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(BidEventPublisher.class);
  private static final String TOPIC = "bid-events";

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  public BidEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  public void bidAccepted(Bid bid) {
    publish("bid.accepted", bid);
  }

  public void bidRejected(Bid bid) {
    publish("bid.rejected", bid);
  }

  private void publish(String eventType, Bid bid) {
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
        kafkaTemplate.send(TOPIC, key, payload);

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
