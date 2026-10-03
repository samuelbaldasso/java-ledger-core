package com.sbaldasso.ledgercore.infrastructure.persistence.mapper;

import com.sbaldasso.ledgercore.domain.model.EntryType;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.domain.model.Transaction;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.EntryJpaEntity;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.TransactionJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEntityMapperTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Test
    @DisplayName("Should map domain Transaction to TransactionJpaEntity with idempotency key")
    void shouldMapDomainToEntity() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Money amount = Money.of("100.00", BRL);
        String idempotencyKey = "key-abc-123";

        Transaction transaction = Transaction.createTransfer(sourceId, targetId, amount, "Test transfer", idempotencyKey);

        TransactionJpaEntity entity = TransactionEntityMapper.toEntity(transaction);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(transaction.getId());
        assertThat(entity.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(entity.getDescription()).isEqualTo("Test transfer");
        assertThat(entity.getCreatedAt()).isEqualTo(transaction.getCreatedAt());
        assertThat(entity.getEntries()).hasSize(2);

        EntryJpaEntity debitEntry = entity.getEntries().get(0);
        assertThat(debitEntry.getAccountId()).isEqualTo(sourceId);
        assertThat(debitEntry.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(debitEntry.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(debitEntry.getCurrency()).isEqualTo("BRL");
        assertThat(debitEntry.getTransaction()).isSameAs(entity);

        EntryJpaEntity creditEntry = entity.getEntries().get(1);
        assertThat(creditEntry.getAccountId()).isEqualTo(targetId);
        assertThat(creditEntry.getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(creditEntry.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(creditEntry.getCurrency()).isEqualTo("BRL");
        assertThat(creditEntry.getTransaction()).isSameAs(entity);
    }

    @Test
    @DisplayName("Should map TransactionJpaEntity to domain Transaction with idempotency key")
    void shouldMapEntityToDomain() {
        UUID txId = UUID.randomUUID();
        UUID acc1 = UUID.randomUUID();
        UUID acc2 = UUID.randomUUID();
        Instant now = Instant.now();
        String idempotencyKey = "key-xyz-789";

        TransactionJpaEntity entity = new TransactionJpaEntity(txId, idempotencyKey, "Deposit", now);
        entity.addEntry(new EntryJpaEntity(UUID.randomUUID(), entity, acc1, EntryType.DEBIT, new BigDecimal("50.00"), "BRL"));
        entity.addEntry(new EntryJpaEntity(UUID.randomUUID(), entity, acc2, EntryType.CREDIT, new BigDecimal("50.00"), "BRL"));

        Transaction domain = TransactionEntityMapper.toDomain(entity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(txId);
        assertThat(domain.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(domain.getDescription()).isEqualTo("Deposit");
        assertThat(domain.getCreatedAt()).isEqualTo(now);
        assertThat(domain.getEntries()).hasSize(2);
        assertThat(domain.getEntries().get(0).accountId()).isEqualTo(acc1);
        assertThat(domain.getEntries().get(1).accountId()).isEqualTo(acc2);
    }
}
