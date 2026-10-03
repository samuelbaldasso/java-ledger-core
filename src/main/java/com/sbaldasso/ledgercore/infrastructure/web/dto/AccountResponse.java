package com.sbaldasso.ledgercore.infrastructure.web.dto;

import com.sbaldasso.ledgercore.domain.model.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String ownerName,
        BigDecimal balance,
        String currency,
        String status,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getBalance().amount(),
                account.getCurrency().getCurrencyCode(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}
