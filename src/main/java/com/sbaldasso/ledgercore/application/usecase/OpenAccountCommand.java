package com.sbaldasso.ledgercore.application.usecase;

import java.util.Currency;
import java.util.Objects;

public record OpenAccountCommand(
        String accountNumber,
        String ownerName,
        Currency currency
) {
    public OpenAccountCommand {
        Objects.requireNonNull(accountNumber, "Account number cannot be null");
        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be blank");
        }
        Objects.requireNonNull(ownerName, "Owner name cannot be null");
        if (ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name cannot be blank");
        }
        Objects.requireNonNull(currency, "Currency cannot be null");
    }
}
