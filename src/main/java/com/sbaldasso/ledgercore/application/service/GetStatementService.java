package com.sbaldasso.ledgercore.application.service;

import com.sbaldasso.ledgercore.application.usecase.GetStatementUseCase;
import com.sbaldasso.ledgercore.domain.model.AccountNotFoundException;
import com.sbaldasso.ledgercore.domain.model.Transaction;
import com.sbaldasso.ledgercore.domain.repository.AccountRepository;
import com.sbaldasso.ledgercore.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class GetStatementService implements GetStatementUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public GetStatementService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> execute(UUID accountId) {
        Objects.requireNonNull(accountId, "AccountId cannot be null");

        if (accountRepository.findById(accountId).isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }

        return transactionRepository.findByAccountId(accountId);
    }
}
