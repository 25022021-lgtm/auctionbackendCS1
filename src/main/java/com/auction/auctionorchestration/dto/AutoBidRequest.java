    package com.auction.auctionorchestration.dto;

    import jakarta.validation.constraints.NotNull;

    public record AutoBidRequest(@NotNull Long itemId, @NotNull Double maxBidLimit) {}
