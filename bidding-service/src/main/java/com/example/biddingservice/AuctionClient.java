package com.example.biddingservice;

import java.math.BigDecimal;

public interface AuctionClient {

  AuctionSummary getAuction(String auctionId);

  void updateHighestBid(String auctionId, String bidId, String bidderId, BigDecimal amount);
}
