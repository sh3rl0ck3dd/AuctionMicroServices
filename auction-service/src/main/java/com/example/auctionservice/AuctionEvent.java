package com.example.auctionservice;

import java.math.BigDecimal;
import java.time.Instant;

public record AuctionEvent(
    String eventType,
    String auctionId,
    String title,
    String sellerId,
    BigDecimal startingPrice,
    BigDecimal currentPrice,
    AuctionStatus status,
    Instant timestamp) {}
