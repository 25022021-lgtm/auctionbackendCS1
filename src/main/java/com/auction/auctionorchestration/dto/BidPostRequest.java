package com.auction.auctionorchestration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request DTO for placing a bid on an auction item. */
public record BidPostRequest(
    @NotNull Long itemId, @Positive @NotNull Double bidAmount) {}