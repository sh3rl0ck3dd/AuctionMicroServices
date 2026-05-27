package com.example.auctionservice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAuctionRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String sellerId,
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal startingPrice) {}
