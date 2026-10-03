package com.sbaldasso.ledgercore.domain.model;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID accountId) {
        super(String.format("Account not found with id: %s", accountId));
    }
}
