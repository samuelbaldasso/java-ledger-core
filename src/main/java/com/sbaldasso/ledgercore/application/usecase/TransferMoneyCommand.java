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
        Objects.requireNonNull(description, "Description cannot be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key cannot be blank");
        }
    }
}
