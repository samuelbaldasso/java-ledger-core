package com.sbaldasso.ledgercore.application.usecase;

import com.sbaldasso.ledgercore.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

public record TransferResult(
        UUID transactionId,
        UUID sourceAccountId,
        UUID targetAccountId,
        Money transferredAmount,
        Money sourceNewBalance,
        Money targetNewBalance,
        Instant executedAt
) {
}
