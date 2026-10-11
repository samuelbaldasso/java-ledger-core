package com.sbaldasso.ledgercore.application.idempotency;

import java.util.Optional;

public interface TransferReceiptRepository {
    Optional<TransferReceipt> findByKey(String key);

    /** Inserts without overwriting a winner. Must participate in the ledger transaction. */
    void insert(TransferReceipt receipt);
}
