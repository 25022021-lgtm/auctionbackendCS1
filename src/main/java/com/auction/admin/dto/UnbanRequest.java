package com.auction.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record UnbanRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
