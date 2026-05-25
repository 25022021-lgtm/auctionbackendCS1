package com.auction.items.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request DTO for publishing a new auction item. */
public record PublishItemRequest(
    @NotBlank String title,
    @NotNull String description,
    @NotNull Long endTime,
    @Positive @NotNull Double startingPrice,
    @Positive @NotNull Double buyItNowPrice,
    @Positive @NotNull Double bidIncrement) {}