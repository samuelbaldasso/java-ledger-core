package com.sbaldasso.ledgercore.infrastructure.persistence.mapper;

import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.AccountStatus;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.AccountJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountEntityMapperTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Test
    @DisplayName("Should map domain Account to AccountJpaEntity")
    void shouldMapDomainToEntity() {
        Account account = Account.openAccount("123-45", "Samuel", BRL);
        account.credit(Money.of("50.00", BRL));

        AccountJpaEntity entity = AccountEntityMapper.toEntity(account);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(account.getId());
        assertThat(entity.getAccountNumber()).isEqualTo("123-45");
        assertThat(entity.getOwnerName()).isEqualTo("Samuel");
        assertThat(entity.getBalance()).isEqualTo(new BigDecimal("50.00"));
        assertThat(entity.getCurrency()).isEqualTo("BRL");
        assertThat(entity.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(entity.getCreatedAt()).isEqualTo(account.getCreatedAt());
    }

    @Test
    @DisplayName("Should map AccountJpaEntity to domain Account")
    void shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        AccountJpaEntity entity = new AccountJpaEntity(
                id,
                "987-65",
                "Alice",
                new BigDecimal("250.75"),
                "BRL",
                AccountStatus.ACTIVE,
                now
        );

        Account account = AccountEntityMapper.toDomain(entity);

        assertThat(account).isNotNull();
        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getAccountNumber()).isEqualTo("987-65");
        assertThat(account.getOwnerName()).isEqualTo("Alice");
        assertThat(account.getBalance()).isEqualTo(Money.of("250.75", BRL));
        assertThat(account.getCurrency()).isEqualTo(BRL);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getCreatedAt()).isEqualTo(now);
    }
}
