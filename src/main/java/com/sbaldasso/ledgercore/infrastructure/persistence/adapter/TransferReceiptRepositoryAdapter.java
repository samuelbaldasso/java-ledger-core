package com.sbaldasso.ledgercore.infrastructure.persistence.adapter;

import com.sbaldasso.ledgercore.application.idempotency.TransferReceipt;
import com.sbaldasso.ledgercore.application.idempotency.TransferReceiptRepository;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.TransferReceiptJpaEntity;
import com.sbaldasso.ledgercore.infrastructure.persistence.mapper.TransferReceiptEntityMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TransferReceiptRepositoryAdapter implements TransferReceiptRepository {
    private final EntityManager entityManager;

    public TransferReceiptRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<TransferReceipt> findByKey(String key) {
        return Optional.ofNullable(entityManager.find(TransferReceiptJpaEntity.class, key))
                .map(TransferReceiptEntityMapper::toReceipt);
    }

    @Override
    public void insert(TransferReceipt receipt) {
        // Flush ledger inserts first, including the transaction referenced by the receipt.
        entityManager.flush();
        // persist is insert-only: merge could overwrite a concurrent winner.
        entityManager.persist(TransferReceiptEntityMapper.toEntity(receipt));
        entityManager.flush();
    }
}
