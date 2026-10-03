package com.sbaldasso.ledgercore.infrastructure.web.dto;

import com.sbaldasso.ledgercore.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountStatementResponse(
        UUID accountId,
        List<StatementEntryResponse> entries
) {
    public record StatementEntryResponse(
            UUID transactionId,
            String description,
            String type,
            BigDecimal amount,
            String currency,
            Instant createdAt
    ) {
    }

    public static AccountStatementResponse from(UUID accountId, List<Transaction> transactions) {
        List<StatementEntryResponse> entries = transactions.stream()
                .flatMap(tx -> tx.getEntries().stream()
                        .filter(e -> e.accountId().equals(accountId))
                        .map(e -> new StatementEntryResponse(
                                tx.getId(),
                                tx.getDescription(),
                                e.type().name(),
                                e.amount().amount(),
                                e.amount().currency().getCurrencyCode(),
                                tx.getCreatedAt()
                        )))
                .toList();

        return new AccountStatementResponse(accountId, entries);
    }
}
