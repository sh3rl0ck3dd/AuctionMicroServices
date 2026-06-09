package com.example.biddingservice;

import java.math.BigDecimal;
import java.time.Instant;

public record BidEvent(
    String eventType,
    String bidId,
    String auctionId,
    String bidderId,
    BigDecimal amount,
    BidStatus status,
    Instant timestamp) {}
