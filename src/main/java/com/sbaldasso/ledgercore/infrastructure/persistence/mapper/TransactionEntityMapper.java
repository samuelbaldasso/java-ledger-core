package com.sbaldasso.ledgercore.infrastructure.persistence.mapper;

import com.sbaldasso.ledgercore.domain.model.Entry;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.domain.model.Transaction;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.EntryJpaEntity;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.TransactionJpaEntity;

import java.util.Currency;
import java.util.List;

public final class TransactionEntityMapper {

    private TransactionEntityMapper() {
    }

    public static Transaction toDomain(TransactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        List<Entry> domainEntries = entity.getEntries().stream()
                .map(e -> Entry.of(
                        e.getId(),
                        e.getAccountId(),
                        e.getEntryType(),
                        Money.of(e.getAmount(), Currency.getInstance(e.getCurrency()))
                ))
                .toList();

        return Transaction.of(
                entity.getId(),
                entity.getIdempotencyKey(),
                entity.getDescription(),
                domainEntries,
                entity.getCreatedAt()
        );
    }

    public static TransactionJpaEntity toEntity(Transaction domain) {
        if (domain == null) {
            return null;
        }
        TransactionJpaEntity entity = new TransactionJpaEntity(
                domain.getId(),
                domain.getIdempotencyKey(),
                domain.getDescription(),
                domain.getCreatedAt()
        );

        for (Entry entry : domain.getEntries()) {
            EntryJpaEntity entryEntity = new EntryJpaEntity(
                    entry.id(),
                    entity,
                    entry.accountId(),
                    entry.type(),
                    entry.amount().amount(),
                    entry.amount().currency().getCurrencyCode()
            );
            entity.addEntry(entryEntity);
        }

        return entity;
    }
}
