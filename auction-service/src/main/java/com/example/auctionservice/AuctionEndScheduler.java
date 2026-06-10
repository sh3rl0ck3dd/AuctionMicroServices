package com.example.auctionservice;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@DependsOn("auctionStore")
public class AuctionEndScheduler {

  private static final Logger log = LoggerFactory.getLogger(AuctionEndScheduler.class);

  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private final AuctionStore store;
  private final Optional<AuctionEventPublisher> eventPublisher;

  public AuctionEndScheduler(AuctionStore store, Optional<AuctionEventPublisher> eventPublisher) {
    this.store = store;
    this.eventPublisher = eventPublisher;
  }

  public void scheduleEnd(String auctionId, Instant endsAt) {
    long delayMs = endsAt.toEpochMilli() - System.currentTimeMillis();
    if (delayMs <= 0) {
      executeEnd(auctionId);
      return;
    }
    executor.schedule(() -> executeEnd(auctionId), delayMs, TimeUnit.MILLISECONDS);
  }

  @PostConstruct
  void recoverOverdue() {
    store.values().stream()
        .filter(a -> a.status() == AuctionStatus.ACTIVE && a.endsAt() != null)
        .filter(a -> a.endsAt().isBefore(Instant.now()))
        .forEach(a -> {
          log.info("Recovering overdue auction {} — transitioning to ENDED", a.id());
          executeEnd(a.id());
        });
  }

  private void executeEnd(String auctionId) {
    Auction auction = store.get(auctionId);
    if (auction == null || auction.status() != AuctionStatus.ACTIVE) {
      return;
    }
    Auction ended =
        new Auction(
            auction.id(),
            auction.title(),
            auction.description(),
            auction.sellerId(),
            auction.startingPrice(),
            auction.currentPrice(),
            AuctionStatus.ENDED,
            auction.createdAt(),
            auction.endsAt());
    store.put(auctionId, ended);
    eventPublisher.ifPresent(p -> p.auctionEnded(ended));
  }
}
