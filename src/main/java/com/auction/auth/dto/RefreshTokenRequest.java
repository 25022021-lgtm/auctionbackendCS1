package com.auction.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Request DTO for refreshing an authentication token. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
