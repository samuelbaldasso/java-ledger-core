package com.sbaldasso.ledgercore.domain.model;

import java.util.UUID;

public class UnbalancedTransactionException extends RuntimeException {

    private final UUID transactionId;
    private final Money totalDebits;
    private final Money totalCredits;

    public UnbalancedTransactionException(UUID transactionId, Money totalDebits, Money totalCredits) {
        super(String.format("Unbalanced transaction %s: total debits (%s %s) does not match total credits (%s %s)",
                transactionId,
                totalDebits.amount(), totalDebits.currency().getCurrencyCode(),
                totalCredits.amount(), totalCredits.currency().getCurrencyCode()));
        this.transactionId = transactionId;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public Money getTotalDebits() {
        return totalDebits;
    }

    public Money getTotalCredits() {
        return totalCredits;
    }
}
