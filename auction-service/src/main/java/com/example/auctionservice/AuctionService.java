package com.example.auctionservice;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuctionService {

  private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

  private final AuctionStore store;
  private final Optional<AuctionEventPublisher> eventPublisher;
  private final Optional<AuctionEndScheduler> scheduler;
  private final Optional<SqsAuctionEndConsumer> sqsConsumer;

  public AuctionService(
      AuctionStore store,
      Optional<AuctionEventPublisher> eventPublisher,
      Optional<AuctionEndScheduler> scheduler,
      Optional<SqsAuctionEndConsumer> sqsConsumer) {
    this.store = store;
    this.eventPublisher = eventPublisher;
    this.scheduler = scheduler;
    this.sqsConsumer = sqsConsumer;
  }

  public Auction createAuction(CreateAuctionRequest request) {
    String auctionId = UUID.randomUUID().toString();
    Auction auction =
        new Auction(
            auctionId,
            request.title(),
            request.description(),
            request.sellerId(),
            request.startingPrice(),
            request.startingPrice(),
            AuctionStatus.DRAFT,
            Instant.now(),
            null,
            null);
    store.put(auctionId, auction);
    eventPublisher.ifPresent(p -> p.auctionCreated(auction));
    log.info("Auction {} created: title='{}' seller={} startingPrice={}",
        auctionId, request.title(), request.sellerId(), request.startingPrice());
    return auction;
  }

  public Auction getAuctionById(String auctionId) {
    Auction auction = store.get(auctionId);
    if (auction == null) {
      log.warn("Auction {} not found", auctionId);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found");
    }
    return auction;
  }

  public List<Auction> listAuctions() {
    return store.values().stream()
        .sorted(Comparator.comparing(Auction::createdAt).reversed().thenComparing(Auction::id))
        .toList();
  }

  public Auction startAuction(String auctionId, Instant endsAt) {
    Auction auction = getAuctionById(auctionId);
    if (auction.status() != AuctionStatus.DRAFT) {
      log.warn("Cannot start auction {} in {} state", auctionId, auction.status());
      throw invalidTransition("start", auction.status());
    }
    if (endsAt != null && !endsAt.isAfter(Instant.now())) {
      log.warn("Cannot start auction {} — endsAt {} is not in the future", auctionId, endsAt);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be in the future");
    }
    Auction started =
        new Auction(
            auction.id(),
            auction.title(),
            auction.description(),
            auction.sellerId(),
            auction.startingPrice(),
            auction.currentPrice(),
            AuctionStatus.ACTIVE,
            auction.createdAt(),
            endsAt,
            null);
    store.put(auctionId, started);
    eventPublisher.ifPresent(p -> p.auctionStarted(started));
    if (endsAt != null) {
      if (sqsConsumer.isPresent()) {
        sqsConsumer.get().scheduleEnd(auctionId, endsAt);
      } else {
        scheduler.ifPresent(s -> s.scheduleEnd(auctionId, endsAt));
      }
    }
    log.info("Auction {} started — endsAt={}", auctionId, endsAt);
    return started;
  }

  public Auction endAuction(String auctionId) {
    Auction auction = getAuctionById(auctionId);
    if (auction.status() != AuctionStatus.ACTIVE) {
      log.warn("Cannot end auction {} in {} state", auctionId, auction.status());
      throw invalidTransition("end", auction.status());
    }
    log.info("Ending auction {}", auctionId);
    return updateAuctionStatus(auction, AuctionStatus.ENDED);
  }

  public Auction cancelAuction(String auctionId) {
    Auction auction = getAuctionById(auctionId);
    if (auction.status() != AuctionStatus.DRAFT && auction.status() != AuctionStatus.ACTIVE) {
      log.warn("Cannot cancel auction {} in {} state", auctionId, auction.status());
      throw invalidTransition("cancel", auction.status());
    }
    log.info("Cancelling auction {}", auctionId);
    return updateAuctionStatus(auction, AuctionStatus.CANCELLED);
  }

  public Auction updateHighestBid(String auctionId, HighestBidRequest request) {
    Auction auction = getAuctionById(auctionId);
    if (auction.status() != AuctionStatus.ACTIVE) {
      log.warn("Cannot update bid for auction {} in {} state", auctionId, auction.status());
      throw invalidTransition("update bid for", auction.status());
    }
    if (request.amount().compareTo(auction.currentPrice()) <= 0) {
      log.warn("Bid amount {} does not exceed current price {} for auction {}",
          request.amount(), auction.currentPrice(), auctionId);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Bid amount must exceed current price: " + auction.currentPrice());
    }
    Auction updated =
        new Auction(
            auction.id(),
            auction.title(),
            auction.description(),
            auction.sellerId(),
            auction.startingPrice(),
            request.amount(),
            auction.status(),
            auction.createdAt(),
            auction.endsAt(),
            Instant.now());
    store.put(updated.id(), updated);
    log.info("Highest bid updated for auction {}: amount={} bidder={}", auctionId, request.amount(), request.bidderId());
    return updated;
  }

  private Auction updateAuctionStatus(Auction auction, AuctionStatus targetStatus) {
    Auction updatedAuction =
        new Auction(
            auction.id(),
            auction.title(),
            auction.description(),
            auction.sellerId(),
            auction.startingPrice(),
            auction.currentPrice(),
            targetStatus,
            auction.createdAt(),
            auction.endsAt(),
            auction.lastBidTime());
    store.put(updatedAuction.id(), updatedAuction);
    log.info("Auction {} status: {} → {}", auction.id(), auction.status(), targetStatus);
    switch (targetStatus) {
      case ACTIVE -> eventPublisher.ifPresent(p -> p.auctionStarted(updatedAuction));
      case ENDED -> eventPublisher.ifPresent(p -> p.auctionEnded(updatedAuction));
    }
    return updatedAuction;
  }

  private ResponseStatusException invalidTransition(String action, AuctionStatus currentStatus) {
    return new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Cannot " + action + " auction in " + currentStatus + " state");
  }
}
