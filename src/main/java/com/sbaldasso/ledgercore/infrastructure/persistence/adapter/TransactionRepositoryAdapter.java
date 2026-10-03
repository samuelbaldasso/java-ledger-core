package com.sbaldasso.ledgercore.infrastructure.persistence.adapter;

import com.sbaldasso.ledgercore.domain.model.Transaction;
import com.sbaldasso.ledgercore.domain.repository.TransactionRepository;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.TransactionJpaEntity;
import com.sbaldasso.ledgercore.infrastructure.persistence.mapper.TransactionEntityMapper;
import com.sbaldasso.ledgercore.infrastructure.persistence.repository.SpringDataTransactionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final SpringDataTransactionRepository springDataTransactionRepository;

    public TransactionRepositoryAdapter(SpringDataTransactionRepository springDataTransactionRepository) {
        this.springDataTransactionRepository = springDataTransactionRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = TransactionEntityMapper.toEntity(transaction);
        TransactionJpaEntity saved = springDataTransactionRepository.save(entity);
        return TransactionEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return springDataTransactionRepository.findById(id)
                .map(TransactionEntityMapper::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return springDataTransactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(TransactionEntityMapper::toDomain);
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId) {
        return springDataTransactionRepository.findByAccountId(accountId).stream()
                .map(TransactionEntityMapper::toDomain)
                .toList();
    }
}
