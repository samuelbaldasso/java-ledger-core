package com.sbaldasso.ledgercore.domain.model;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

public class Account {

    private final UUID id;
    private final String accountNumber;
    private String ownerName;
    private Money balance;
    private AccountStatus status;
    private final Instant createdAt;

    private Account(UUID id, String accountNumber, String ownerName, Money balance, AccountStatus status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Account id cannot be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.ownerName = Objects.requireNonNull(ownerName, "Owner name cannot be null");
        this.balance = Objects.requireNonNull(balance, "Balance cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
    }

    public static Account openAccount(String accountNumber, String ownerName, Currency currency) {
        Objects.requireNonNull(currency, "Currency cannot be null");
        return new Account(
                UUID.randomUUID(),
                accountNumber,
                ownerName,
                Money.zero(currency),
                AccountStatus.ACTIVE,
                Instant.now()
        );
    }

    /**
     * Factory method for reconstituting an existing account from persistence.
     */
    public static Account reconstitute(UUID id, String accountNumber, String ownerName, Money balance, AccountStatus status, Instant createdAt) {
        return new Account(id, accountNumber, ownerName, balance, status, createdAt);
    }

    public void debit(Money amount) {
        requireActive();
        Objects.requireNonNull(amount, "Debit amount cannot be null");

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }

        if (this.balance.isLessThan(amount)) {
            throw new InsufficientFundsException(this.id, amount, this.balance);
        }

        this.balance = this.balance.subtract(amount);
    }

    public void credit(Money amount) {
        requireActive();
        Objects.requireNonNull(amount, "Credit amount cannot be null");

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }

        this.balance = this.balance.add(amount);
    }

    public void close() {
        requireActive();
        if (!this.balance.isZero()) {
            throw new IllegalStateException(String.format("Cannot close account %s with remaining balance %s", id, balance.amount()));
        }
        this.status = AccountStatus.CLOSED;
    }

    public void changeOwnerName(String newOwnerName) {
        Objects.requireNonNull(newOwnerName, "Owner name cannot be null");
        if (newOwnerName.isBlank()) {
            throw new IllegalArgumentException("Owner name cannot be blank");
        }
        this.ownerName = newOwnerName;
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(id);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Money getBalance() {
        return balance;
    }

    public Currency getCurrency() {
        return balance.currency();
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
