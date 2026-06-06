package com.auction.users.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request DTO for depositing credit into a user's balance. */
public record DepositRequest(@Positive @NotNull Double amount) {}
