package com.example.biddingservice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PlaceBidRequest(
    @NotBlank String bidderId,
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount) {}
