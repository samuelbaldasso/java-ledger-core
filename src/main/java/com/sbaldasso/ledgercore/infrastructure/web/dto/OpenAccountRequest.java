package com.sbaldasso.ledgercore.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpenAccountRequest(
        @NotBlank(message = "accountNumber is required")
        String accountNumber,

        @NotBlank(message = "ownerName is required")
        String ownerName,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        String currency
) {
}
