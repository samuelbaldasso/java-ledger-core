package com.sbaldasso.ledgercore.domain.model;

public class AccountNumberAlreadyExistsException extends RuntimeException {

    public AccountNumberAlreadyExistsException(String accountNumber) {
        super(String.format("Account with number %s already exists", accountNumber));
    }
}
