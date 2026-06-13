package com.example.auctionservice;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnProperty(prefix = "auction-service.sqs", name = "enabled", havingValue = "false", matchIfMissing = true)
public class AuctionEndScheduler {

  private static final Logger log = LoggerFactory.getLogger(AuctionEndScheduler.class);

  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private final AuctionRepository repo;
  private final Optional<AuctionEventPublisher> eventPublisher;

  public AuctionEndScheduler(AuctionRepository repo, Optional<AuctionEventPublisher> eventPublisher) {
    this.repo = repo;
    this.eventPublisher = eventPublisher;
  }

  public void scheduleEnd(String auctionId, Instant endsAt) {
    long delayMs = endsAt.toEpochMilli() - System.currentTimeMillis();
    if (delayMs <= 0) {
      log.info("Auction {} end time already passed — ending immediately", auctionId);
      executeEnd(auctionId);
      return;
    }
    log.info("Scheduled auction {} to end at {} (delay {}ms)", auctionId, endsAt, delayMs);
    executor.schedule(() -> executeEnd(auctionId), delayMs, TimeUnit.MILLISECONDS);
  }

  @PostConstruct
  void recoverOverdue() {
    repo.findByStatus(AuctionStatus.ACTIVE).stream()
        .filter(a -> a.endsAt() != null)
        .filter(a -> a.endsAt().isBefore(Instant.now()))
        .forEach(a -> {
          log.info("Recovering overdue auction {} — transitioning to ENDED", a.id());
          executeEnd(a.id());
        });
  }

  private void executeEnd(String auctionId) {
    Auction auction = repo.findById(auctionId).orElse(null);
    if (auction == null || auction.status() != AuctionStatus.ACTIVE) {
      return;
    }
    Auction ended = auction.withStatus(AuctionStatus.ENDED);
    repo.save(ended);
    eventPublisher.ifPresent(p -> p.auctionEnded(ended));
    log.info("Auction {} ended via scheduler", auctionId);
  }
}
