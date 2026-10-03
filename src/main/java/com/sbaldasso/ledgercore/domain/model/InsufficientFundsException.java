package com.sbaldasso.ledgercore.domain.model;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    private final UUID accountId;
    private final Money requested;
    private final Money currentBalance;

    public InsufficientFundsException(UUID accountId, Money requested, Money currentBalance) {
        super(String.format("Insufficient funds for account %s: requested %s %s, current balance %s %s",
                accountId,
                requested.amount(), requested.currency().getCurrencyCode(),
                currentBalance.amount(), currentBalance.currency().getCurrencyCode()));
        this.accountId = accountId;
        this.requested = requested;
        this.currentBalance = currentBalance;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Money getRequested() {
        return requested;
    }

    public Money getCurrentBalance() {
        return currentBalance;
    }
}
