package com.sbaldasso.ledgercore.infrastructure.persistence.mapper;

import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.AccountJpaEntity;

import java.util.Currency;

public final class AccountEntityMapper {

    private AccountEntityMapper() {
    }

    public static Account toDomain(AccountJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Money balance = Money.of(entity.getBalance(), Currency.getInstance(entity.getCurrency()));
        return Account.reconstitute(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getOwnerName(),
                balance,
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public static AccountJpaEntity toEntity(Account domain) {
        if (domain == null) {
            return null;
        }
        return new AccountJpaEntity(
                domain.getId(),
                domain.getAccountNumber(),
                domain.getOwnerName(),
                domain.getBalance().amount(),
                domain.getCurrency().getCurrencyCode(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
    }
}
