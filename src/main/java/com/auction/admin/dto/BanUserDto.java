package com.auction.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record BanUserDto(@NotBlank String username, @NotBlank String reasons) {
}
