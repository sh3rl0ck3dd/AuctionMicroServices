package com.example.auctionservice;

import java.math.BigDecimal;

public record HighestBidRequest(String bidId, String bidderId, BigDecimal amount) {}
