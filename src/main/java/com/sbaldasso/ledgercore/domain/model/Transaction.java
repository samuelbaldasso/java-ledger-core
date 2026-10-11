package com.sbaldasso.ledgercore.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Transaction {

    private final UUID id;
    private final String idempotencyKey;
    private final String description;
    private final List<Entry> entries;
    private final Instant createdAt;

    private Transaction(UUID id, String idempotencyKey, String description, List<Entry> entries, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Transaction id cannot be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "IdempotencyKey cannot be null");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("IdempotencyKey cannot be blank");
        }
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        Objects.requireNonNull(entries, "Entries cannot be null");
        if (entries.size() < 2) {
            throw new IllegalArgumentException("Transaction must contain at least 2 entries for double-entry bookkeeping");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");

        validateDoubleEntryBalance(id, entries);

        this.entries = List.copyOf(entries);
    }

    private static void validateDoubleEntryBalance(UUID transactionId, List<Entry> entries) {
        Currency commonCurrency = entries.get(0).amount().currency();
        Money totalDebits = Money.zero(commonCurrency);
        Money totalCredits = Money.zero(commonCurrency);

        for (Entry entry : entries) {
            if (!entry.amount().currency().equals(commonCurrency)) {
                throw new CurrencyMismatchException(commonCurrency, entry.amount().currency());
            }

            if (entry.type() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(entry.amount());
            } else {
                totalCredits = totalCredits.add(entry.amount());
            }
        }

        if (totalDebits.isZero() || totalCredits.isZero() || !totalDebits.equals(totalCredits)) {
            throw new UnbalancedTransactionException(transactionId, totalDebits, totalCredits);
        }
    }

    public static Transaction createTransfer(UUID sourceAccountId, UUID targetAccountId, Money amount, String description, String idempotencyKey) {
        Objects.requireNonNull(sourceAccountId, "Source account id cannot be null");
        Objects.requireNonNull(targetAccountId, "Target account id cannot be null");
        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("Source and target accounts must be different");
        }
        Objects.requireNonNull(amount, "Amount cannot be null");

        List<Entry> entries = List.of(
                Entry.debit(sourceAccountId, amount),
                Entry.credit(targetAccountId, amount)
        );

        // Match PostgreSQL precision so the first response and persisted replay have the same timestamp.
        return new Transaction(UUID.randomUUID(), idempotencyKey, description, entries, Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    public static Transaction createTransfer(UUID sourceAccountId, UUID targetAccountId, Money amount, String description) {
        return createTransfer(sourceAccountId, targetAccountId, amount, description, UUID.randomUUID().toString());
    }

    public static Transaction of(UUID id, String idempotencyKey, String description, List<Entry> entries, Instant createdAt) {
        return new Transaction(id, idempotencyKey, description, entries, createdAt);
    }

    public static Builder builder(String description) {
        return new Builder(description);
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDescription() {
        return description;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String idempotencyKey = UUID.randomUUID().toString();
        private final String description;
        private final List<Entry> entries = new ArrayList<>();
        private Instant createdAt = Instant.now();

        private Builder(String description) {
            this.description = description;
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder debit(UUID accountId, Money amount) {
            this.entries.add(Entry.debit(accountId, amount));
            return this;
        }

        public Builder credit(UUID accountId, Money amount) {
            this.entries.add(Entry.credit(accountId, amount));
            return this;
        }

        public Builder addEntry(Entry entry) {
            this.entries.add(entry);
            return this;
        }

        public Transaction build() {
            return new Transaction(id, idempotencyKey, description, entries, createdAt);
        }
    }
}
