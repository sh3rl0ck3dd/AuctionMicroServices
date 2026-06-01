package com.example.biddingservice;

import java.math.BigDecimal;
import java.time.Instant;

public record BidResponse(
    String id,
    String auctionId,
    String bidderId,
    BigDecimal amount,
    BidStatus status,
    Instant createdAt) {}
