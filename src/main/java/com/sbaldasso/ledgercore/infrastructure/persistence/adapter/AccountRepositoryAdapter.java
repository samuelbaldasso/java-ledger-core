package com.sbaldasso.ledgercore.infrastructure.persistence.adapter;

import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.repository.AccountRepository;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.AccountJpaEntity;
import com.sbaldasso.ledgercore.infrastructure.persistence.mapper.AccountEntityMapper;
import com.sbaldasso.ledgercore.infrastructure.persistence.repository.SpringDataAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AccountRepositoryAdapter implements AccountRepository {

    private final SpringDataAccountRepository springDataAccountRepository;

    public AccountRepositoryAdapter(SpringDataAccountRepository springDataAccountRepository) {
        this.springDataAccountRepository = springDataAccountRepository;
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = AccountEntityMapper.toEntity(account);
        AccountJpaEntity saved = springDataAccountRepository.save(entity);
        return AccountEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return springDataAccountRepository.findById(id)
                .map(AccountEntityMapper::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(UUID id) {
        return springDataAccountRepository.findByIdForUpdate(id)
                .map(AccountEntityMapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return springDataAccountRepository.findByAccountNumber(accountNumber)
                .map(AccountEntityMapper::toDomain);
    }
}
