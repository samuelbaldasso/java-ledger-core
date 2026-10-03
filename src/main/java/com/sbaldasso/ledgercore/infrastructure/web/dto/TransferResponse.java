package com.sbaldasso.ledgercore.infrastructure.web.dto;

import com.sbaldasso.ledgercore.application.usecase.TransferResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal transferredAmount,
        String currency,
        BigDecimal sourceNewBalance,
        BigDecimal targetNewBalance,
        Instant executedAt
) {
    public static TransferResponse from(TransferResult result) {
        return new TransferResponse(
                result.transactionId(),
                result.sourceAccountId(),
                result.targetAccountId(),
                result.transferredAmount().amount(),
                result.transferredAmount().currency().getCurrencyCode(),
                result.sourceNewBalance().amount(),
                result.targetNewBalance().amount(),
                result.executedAt()
        );
    }
}
