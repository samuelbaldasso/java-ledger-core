package com.sbaldasso.ledgercore.domain.model;

import java.util.UUID;

public class AccountNotActiveException extends RuntimeException {
    private final UUID id;

    public UUID getId() {
        return id;
    }

    public AccountNotActiveException(UUID id) {
        super(String.format("Account with %s not active. Try again.", id));
        this.id = id;
    }
}
