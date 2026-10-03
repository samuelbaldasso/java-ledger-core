package com.sbaldasso.ledgercore.infrastructure.persistence.repository;

import com.sbaldasso.ledgercore.infrastructure.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    Optional<TransactionJpaEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT DISTINCT t FROM TransactionJpaEntity t JOIN t.entries e WHERE e.accountId = :accountId ORDER BY t.createdAt DESC")
    List<TransactionJpaEntity> findByAccountId(@Param("accountId") UUID accountId);
}
