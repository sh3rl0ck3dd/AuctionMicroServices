package com.example.auctionservice;

import java.math.BigDecimal;
import java.time.Instant;

public record AuctionResponse(
    String id,
    String title,
    String description,
    String sellerId,
    BigDecimal startingPrice,
    BigDecimal currentPrice,
    AuctionStatus status,
    Instant endsAt,
    Instant lastBidTime) {}
