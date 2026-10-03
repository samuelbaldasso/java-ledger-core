package com.sbaldasso.ledgercore.application.service;

import com.sbaldasso.ledgercore.application.usecase.OpenAccountCommand;
import com.sbaldasso.ledgercore.application.usecase.OpenAccountUseCase;
import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.AccountNumberAlreadyExistsException;
import com.sbaldasso.ledgercore.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class OpenAccountService implements OpenAccountUseCase {

    private final AccountRepository accountRepository;

    public OpenAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public Account execute(OpenAccountCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        if (accountRepository.findByAccountNumber(command.accountNumber()).isPresent()) {
            throw new AccountNumberAlreadyExistsException(command.accountNumber());
        }

        Account newAccount = Account.openAccount(
                command.accountNumber(),
                command.ownerName(),
                command.currency()
        );

        return accountRepository.save(newAccount);
    }
}
