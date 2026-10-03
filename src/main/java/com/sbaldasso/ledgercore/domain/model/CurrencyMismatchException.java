package com.sbaldasso.ledgercore.domain.model;

import java.util.Currency;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(Currency expected, Currency actual) {
        super(String.format("Currency mismatch: expected %s, but got %s",
                expected != null ? expected.getCurrencyCode() : "null",
                actual != null ? actual.getCurrencyCode() : "null"));
    }
}
