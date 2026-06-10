package com.example.auctionservice;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record StartAuctionRequest(@NotNull Instant endsAt) {}
