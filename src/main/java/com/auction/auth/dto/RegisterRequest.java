package com.auction.auth.dto;

import com.auction.common.annotations.NoSpace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Request DTO for user registration. */
public record RegisterRequest(
    @NotEmpty(message = "Username must not be empty")
        @NotNull(message = "Username must not be null")
        @NoSpace(message = "Username can't have space")
        String username,
    @NotNull(message = "Display name must not be null")
        @NotBlank(message = "Display name can't be blank")
        String displayName,
    @NotEmpty(message = "Password must not be empty")
        @NotNull(message = "Password must not be null")
        @NoSpace(message = "Password can't have space")
        String password) {}
