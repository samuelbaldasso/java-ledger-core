package com.sbaldasso.ledgercore.application.usecase;

import com.sbaldasso.ledgercore.domain.model.Transaction;

import java.util.List;
import java.util.UUID;

public interface GetStatementUseCase {

    List<Transaction> execute(UUID accountId);
}
