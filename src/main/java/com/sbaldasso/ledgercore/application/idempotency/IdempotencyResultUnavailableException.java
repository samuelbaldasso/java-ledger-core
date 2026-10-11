package com.sbaldasso.ledgercore.application.idempotency;

public class IdempotencyResultUnavailableException extends RuntimeException {
    public IdempotencyResultUnavailableException() {
        super("This key belongs to a historical transaction without a stored response; the transfer was not repeated");
    }
}
