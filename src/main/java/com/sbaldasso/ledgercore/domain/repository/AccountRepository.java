package com.sbaldasso.ledgercore.domain.repository;

import com.sbaldasso.ledgercore.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    Optional<Account> findByIdForUpdate(UUID id);

    Optional<Account> findByAccountNumber(String accountNumber);
}
