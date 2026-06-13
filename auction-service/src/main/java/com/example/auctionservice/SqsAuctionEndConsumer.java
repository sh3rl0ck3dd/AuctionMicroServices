package com.example.auctionservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "auction-service.sqs", name = "enabled", havingValue = "true")
public class SqsAuctionEndConsumer {

  private static final Logger log = LoggerFactory.getLogger(SqsAuctionEndConsumer.class);

  private final SqsClient sqsClient;
  private final String queueUrl;
  private final AuctionRepository repo;
  private final Optional<AuctionEventPublisher> eventPublisher;
  private final ObjectMapper objectMapper;
  private final Duration extensionDuration;
  private final Duration bidWindow;

  public SqsAuctionEndConsumer(
      SqsClient sqsClient,
      String queueUrl,
      AuctionRepository repo,
      Optional<AuctionEventPublisher> eventPublisher,
      @Value("${auction-service.extension.minutes:5}") int extensionMinutes,
      @Value("${auction-service.bid-window.minutes:5}") int bidWindowMinutes) {
    this.sqsClient = sqsClient;
    this.queueUrl = queueUrl;
    this.repo = repo;
    this.eventPublisher = eventPublisher;
    this.objectMapper = new ObjectMapper();
    this.extensionDuration = Duration.ofMinutes(extensionMinutes);
    this.bidWindow = Duration.ofMinutes(bidWindowMinutes);
  }

  @Scheduled(fixedDelay = 1_000)
  public void poll() {
    ReceiveMessageRequest request = ReceiveMessageRequest.builder()
        .queueUrl(queueUrl)
        .maxNumberOfMessages(10)
        .waitTimeSeconds(20)
        .build();

    List<Message> messages = sqsClient.receiveMessage(request).messages();
    for (Message message : messages) {
      processMessage(message);
    }
  }

  private void processMessage(Message message) {
    AucEndMessage aucEndMessage;
    try {
      aucEndMessage = objectMapper.readValue(message.body(), AucEndMessage.class);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse SQS message body: {}", message.body(), e);
      deleteMessage(message);
      return;
    }

    String auctionId = aucEndMessage.auctionId();
    Instant endsAt = aucEndMessage.endsAt();

    Auction auction = repo.findById(auctionId).orElse(null);
    if (auction == null || auction.status() != AuctionStatus.ACTIVE) {
      deleteMessage(message);
      return;
    }

    Instant now = Instant.now();

    if (now.isBefore(endsAt)) {
      long remainingSeconds = Duration.between(now, endsAt).getSeconds() + 1;
      sqsClient.changeMessageVisibility(r -> r
          .queueUrl(queueUrl)
          .receiptHandle(message.receiptHandle())
          .visibilityTimeout((int) Math.min(remainingSeconds, 43_200)));
      return;
    }

    Instant lastBid = auction.lastBidTime();
    if (lastBid != null && lastBid.plus(bidWindow).isAfter(now)) {
      Instant newEndsAt = now.plus(extensionDuration);
      Auction extended = auction.withEndsAt(newEndsAt);
      repo.save(extended);
      log.info("Extended auction {} end time to {}", auctionId, newEndsAt);
      sendScheduledEnd(auctionId, newEndsAt);
      deleteMessage(message);
      return;
    }

    Auction ended = auction.withStatus(AuctionStatus.ENDED);
    repo.save(ended);
    eventPublisher.ifPresent(p -> p.auctionEnded(ended));
    log.info("Auction {} ended via SQS consumer", auctionId);
    deleteMessage(message);
  }

  private void sendScheduledEnd(String auctionId, Instant endsAt) {
    AucEndMessage msg = new AucEndMessage(auctionId, endsAt);
    try {
      String body = objectMapper.writeValueAsString(msg);
      long delaySeconds = Math.max(0, Duration.between(Instant.now(), endsAt).getSeconds());
      sqsClient.sendMessage(SendMessageRequest.builder()
          .queueUrl(queueUrl)
          .messageBody(body)
          .delaySeconds((int) Math.min(delaySeconds, 900))
          .build());
      log.info("Sent SQS end message for auction {} with delay {}s", auctionId, Math.min(delaySeconds, 900));
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize SQS message for auction {}", auctionId, e);
    }
  }

  public void scheduleEnd(String auctionId, Instant endsAt) {
    log.info("Scheduling auction {} end via SQS at {}", auctionId, endsAt);
    sendScheduledEnd(auctionId, endsAt);
  }

  private void deleteMessage(Message message) {
    sqsClient.deleteMessage(DeleteMessageRequest.builder()
        .queueUrl(queueUrl)
        .receiptHandle(message.receiptHandle())
        .build());
  }

  private record AucEndMessage(String auctionId, Instant endsAt) {}
}
