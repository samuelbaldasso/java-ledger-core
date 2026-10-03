package com.sbaldasso.ledgercore.application.service;

import com.sbaldasso.ledgercore.application.usecase.GetAccountUseCase;
import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.AccountNotFoundException;
import com.sbaldasso.ledgercore.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class GetAccountService implements GetAccountUseCase {

    private final AccountRepository accountRepository;

    public GetAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Account execute(UUID accountId) {
        Objects.requireNonNull(accountId, "AccountId cannot be null");
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
