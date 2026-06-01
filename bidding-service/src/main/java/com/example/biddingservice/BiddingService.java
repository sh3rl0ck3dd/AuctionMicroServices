package com.example.biddingservice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BiddingService {

  private final AuctionClient auctionClient;
  private final Map<String, List<Bid>> bidsByAuctionId = new ConcurrentHashMap<>();
  private final Map<String, ReentrantLock> lockByAuctionId = new ConcurrentHashMap<>();

  public BiddingService(AuctionClient auctionClient) {
    this.auctionClient = auctionClient;
  }

  public Bid placeBid(String auctionId, PlaceBidRequest request) {
    ReentrantLock lock = lockByAuctionId.computeIfAbsent(auctionId, k -> new ReentrantLock());
    lock.lock();
    try {
      AuctionSummary auction = auctionClient.getAuction(auctionId);

      if (!"ACTIVE".equals(auction.status())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Auction is not active: " + auctionId);
      }

      List<Bid> bids = bidsByAuctionId.computeIfAbsent(auctionId, k -> new ArrayList<>());

      BigDecimal currentHighest = bids.stream()
          .map(Bid::amount)
          .max(BigDecimal::compareTo)
          .orElse(auction.startingPrice());

      if (request.amount().compareTo(currentHighest) <= 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Bid amount must be greater than current highest bid: " + currentHighest);
      }

      Bid previousHighest = bids.stream()
          .filter(b -> b.status() == BidStatus.ACTIVE)
          .reduce((a, b) -> a.amount().compareTo(b.amount()) > 0 ? a : b)
          .orElse(null);

      if (previousHighest != null) {
        Bid outbid =
            new Bid(
                previousHighest.id(),
                previousHighest.auctionId(),
                previousHighest.bidderId(),
                previousHighest.amount(),
                BidStatus.OUTBID,
                previousHighest.createdAt());
        bids.replaceAll(b -> b.id().equals(outbid.id()) ? outbid : b);
      }

      String bidId = UUID.randomUUID().toString();
      Bid bid =
          new Bid(
              bidId,
              auctionId,
              request.bidderId(),
              request.amount(),
              BidStatus.ACTIVE,
              Instant.now());

      bids.add(bid);
      return bid;
    } finally {
      lock.unlock();
    }
  }

  public List<Bid> getBidsForAuction(String auctionId) {
    return bidsByAuctionId.getOrDefault(auctionId, List.of());
  }
}
