package com.example.auctionservice;

import java.math.BigDecimal;
import java.time.Instant;

public record Auction(
    String id,
    String title,
    String description,
    String sellerId,
    BigDecimal startingPrice,
    AuctionStatus status,
    Instant createdAt) {}
