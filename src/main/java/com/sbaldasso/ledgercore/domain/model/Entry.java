package com.sbaldasso.ledgercore.domain.model;

import java.util.Objects;
import java.util.UUID;

public record Entry(UUID id, UUID accountId, EntryType type, Money amount) {

    public Entry {
        Objects.requireNonNull(id, "Entry id cannot be null");
        Objects.requireNonNull(accountId, "AccountId cannot be null");
        Objects.requireNonNull(type, "EntryType cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Entry amount must be positive");
        }
    }

    public static Entry debit(UUID accountId, Money amount) {
        return new Entry(UUID.randomUUID(), accountId, EntryType.DEBIT, amount);
    }

    public static Entry credit(UUID accountId, Money amount) {
        return new Entry(UUID.randomUUID(), accountId, EntryType.CREDIT, amount);
    }

    public static Entry of(UUID id, UUID accountId, EntryType type, Money amount) {
        return new Entry(id, accountId, type, amount);
    }
}
