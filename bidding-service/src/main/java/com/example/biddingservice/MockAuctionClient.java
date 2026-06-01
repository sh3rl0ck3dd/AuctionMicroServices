package com.example.biddingservice;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("mock")
public class MockAuctionClient implements AuctionClient {

  private static final BigDecimal DEFAULT_STARTING_PRICE = new BigDecimal("10");

  private static final Map<String, String> STATUS_OVERRIDES =
      Map.of("auction-ended", "ENDED");

  @Override
  public AuctionSummary getAuction(String auctionId) {
    if (auctionId == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found: " + auctionId);
    }

    String status = STATUS_OVERRIDES.getOrDefault(auctionId, "ACTIVE");
    return new AuctionSummary(auctionId, status, DEFAULT_STARTING_PRICE);
  }

  @Override
  public void updateHighestBid(String auctionId, String bidId, String bidderId,
      java.math.BigDecimal amount) {
  }
}
