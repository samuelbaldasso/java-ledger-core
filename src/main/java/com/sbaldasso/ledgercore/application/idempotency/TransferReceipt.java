package com.sbaldasso.ledgercore.application.idempotency;

import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;

import java.util.Objects;

/** The successful request and its original result, independent of current account state. */
public record TransferReceipt(TransferMoneyCommand request, TransferResult result) {
    public TransferReceipt {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(result, "Result cannot be null");
    }

    public TransferResult replay(TransferMoneyCommand retry) {
        if (!request.equals(retry)) {
            throw new IdempotencyConflictException();
        }
        return result;
    }
}
