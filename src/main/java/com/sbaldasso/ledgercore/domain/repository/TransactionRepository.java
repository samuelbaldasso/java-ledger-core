package com.sbaldasso.ledgercore.domain.repository;

import com.sbaldasso.ledgercore.domain.model.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID id);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByAccountId(UUID accountId);
}
