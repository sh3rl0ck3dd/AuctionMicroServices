package com.example.auctionservice;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuctionService {

  private final Map<String, Auction> auctions = new ConcurrentHashMap<>();

  public Auction createAuction(CreateAuctionRequest request) {
    String auctionId = UUID.randomUUID().toString();
    Auction auction =
        new Auction(
            auctionId,
            request.title(),
            request.description(),
            request.sellerId(),
            request.startingPrice(),
            AuctionStatus.DRAFT,
            Instant.now());
    auctions.put(auctionId, auction);
    return auction;
  }

  public Auction getAuctionById(String auctionId) {
    Auction auction = auctions.get(auctionId);
    if (auction == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found");
    }
    return auction;
  }

  public List<Auction> listAuctions() {
    return auctions.values().stream()
        .sorted(Comparator.comparing(Auction::createdAt).reversed().thenComparing(Auction::id))
        .toList();
  }

  public Auction startAuction(String auctionId) {
    Auction auction = getAuctionById(auctionId);
    if (auction.status() != AuctionStatus.DRAFT) {
      throw invalidTransition("start", auction.status());
    }
    return updateAuctionStatus(auction, AuctionStatus.ACTIVE);
  }

  public Auction endAuction(String auctionId) {
    Auction auction = getAuctionById(auctionId);
    if (auction.status() != AuctionStatus.ACTIVE) {
      throw invalidTransition("end", auction.status());
    }
    return updateAuctionStatus(auction, AuctionStatus.ENDED);
  }

  public Auction cancelAuction(String auctionId) {
    Auction auction = getAuctionById(auctionId);
    if (auction.status() != AuctionStatus.DRAFT && auction.status() != AuctionStatus.ACTIVE) {
      throw invalidTransition("cancel", auction.status());
    }
    return updateAuctionStatus(auction, AuctionStatus.CANCELLED);
  }

  private Auction updateAuctionStatus(Auction auction, AuctionStatus targetStatus) {
    Auction updatedAuction =
        new Auction(
            auction.id(),
            auction.title(),
            auction.description(),
            auction.sellerId(),
            auction.startingPrice(),
            targetStatus,
            auction.createdAt());
    auctions.put(updatedAuction.id(), updatedAuction);
    return updatedAuction;
  }

  private ResponseStatusException invalidTransition(String action, AuctionStatus currentStatus) {
    return new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Cannot " + action + " auction in " + currentStatus + " state");
  }
}
