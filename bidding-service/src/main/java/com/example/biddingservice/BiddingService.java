package com.example.biddingservice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class BiddingService {

  private final Map<String, List<Bid>> bidsByAuctionId = new ConcurrentHashMap<>();

  public Bid placeBid(String auctionId, PlaceBidRequest request) {
    String bidId = UUID.randomUUID().toString();
    Bid bid =
        new Bid(
            bidId,
            auctionId,
            request.bidderId(),
            request.amount(),
            BidStatus.ACTIVE,
            Instant.now());

    bidsByAuctionId.computeIfAbsent(auctionId, k -> new ArrayList<>()).add(bid);
    return bid;
  }

  public List<Bid> getBidsForAuction(String auctionId) {
    return bidsByAuctionId.getOrDefault(auctionId, List.of());
  }
}
