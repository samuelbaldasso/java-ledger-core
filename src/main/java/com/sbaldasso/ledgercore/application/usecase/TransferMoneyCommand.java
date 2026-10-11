package com.sbaldasso.ledgercore.application.usecase;

import com.sbaldasso.ledgercore.domain.model.Money;

import java.util.Objects;
import java.util.UUID;

public record TransferMoneyCommand(
        UUID sourceAccountId,
        UUID targetAccountId,
        Money amount,
        String description,
        String idempotencyKey
) {
    public TransferMoneyCommand {
        Objects.requireNonNull(sourceAccountId, "Source account id cannot be null");
        Objects.requireNonNull(targetAccountId, "Target account id cannot be null");
        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("Source and target accounts must be different");
        }
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        Objects.requireNonNull(description, "Description cannot be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (description.length() > 255) {
            throw new IllegalArgumentException("Description cannot exceed 255 characters");
        }
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key cannot be blank");
        }
        if (idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency key cannot exceed 128 characters");
        }
    }
}
