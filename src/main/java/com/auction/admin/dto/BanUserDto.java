package com.auction.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record BanUserDto(@NotBlank String usernamer, @NotBlank String reasons) {
}
