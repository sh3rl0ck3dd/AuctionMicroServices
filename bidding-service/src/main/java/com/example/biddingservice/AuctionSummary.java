package com.example.biddingservice;

import java.math.BigDecimal;

public record AuctionSummary(String id, String status, BigDecimal startingPrice) {}
