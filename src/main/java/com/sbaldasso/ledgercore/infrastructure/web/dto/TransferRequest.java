package com.sbaldasso.ledgercore.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "sourceAccountId is required")
        UUID sourceAccountId,

        @NotNull(message = "targetAccountId is required")
        UUID targetAccountId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be strictly positive")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        String currency,

        @NotBlank(message = "description is required")
        String description
) {
}
