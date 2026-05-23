package com.auction.users.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositRequest(@Positive @NotNull Double amount) {
}
