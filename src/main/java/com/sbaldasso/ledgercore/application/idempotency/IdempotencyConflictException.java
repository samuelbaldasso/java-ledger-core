package com.sbaldasso.ledgercore.application.idempotency;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("Idempotency key has already been used with different transfer data");
    }
}
