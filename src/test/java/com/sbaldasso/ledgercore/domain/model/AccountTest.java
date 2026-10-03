package com.sbaldasso.ledgercore.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    @Nested
    @DisplayName("Account Opening and Lifecycle")
    class OpeningAndLifecycle {

        @Test
        @DisplayName("Should open account with active status and zero balance")
        void shouldOpenAccountWithZeroBalance() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);

            assertThat(account.getId()).isNotNull();
            assertThat(account.getAccountNumber()).isEqualTo("12345-6");
            assertThat(account.getOwnerName()).isEqualTo("Samuel");
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(account.getBalance()).isEqualTo(Money.zero(BRL));
            assertThat(account.getCurrency()).isEqualTo(BRL);
            assertThat(account.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should close account successfully when balance is zero")
        void shouldCloseAccountWhenBalanceIsZero() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);

            account.close();

            assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        }

        @Test
        @DisplayName("Should reject closing account when balance is not zero")
        void shouldRejectClosingAccountWithRemainingBalance() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);
            account.credit(Money.of("50.00", BRL));

            assertThatThrownBy(account::close)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot close account");
        }

        @Test
        @DisplayName("Should reconstitute existing account from persistence state")
        void shouldReconstituteAccount() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            Money balance = Money.of("250.75", BRL);

            Account account = Account.reconstitute(id, "999-0", "John Doe", balance, AccountStatus.ACTIVE, now);

            assertThat(account.getId()).isEqualTo(id);
            assertThat(account.getBalance()).isEqualTo(balance);
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Credit Operations")
    class CreditOperations {

        @Test
        @DisplayName("Should increase balance on credit")
        void shouldIncreaseBalanceOnCredit() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);

            account.credit(Money.of("150.00", BRL));

            assertThat(account.getBalance()).isEqualTo(Money.of("150.00", BRL));
        }

        @Test
        @DisplayName("Should reject non-positive credit amount")
        void shouldRejectNonPositiveCredit() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);

            assertThatThrownBy(() -> account.credit(Money.zero(BRL)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Credit amount must be positive");

            assertThatThrownBy(() -> account.credit(Money.of("-10.00", BRL)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Credit amount must be positive");
        }

        @Test
        @DisplayName("Should reject credit with mismatched currency")
        void shouldRejectCreditWithMismatchedCurrency() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);

            assertThatThrownBy(() -> account.credit(Money.of("100.00", USD)))
                    .isInstanceOf(CurrencyMismatchException.class);
        }

        @Test
        @DisplayName("Should reject credit on closed account")
        void shouldRejectCreditOnClosedAccount() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);
            account.close();

            assertThatThrownBy(() -> account.credit(Money.of("100.00", BRL)))
                    .isInstanceOf(AccountNotActiveException.class);
        }
    }

    @Nested
    @DisplayName("Debit Operations")
    class DebitOperations {

        @Test
        @DisplayName("Should decrease balance on debit")
        void shouldDecreaseBalanceOnDebit() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);
            account.credit(Money.of("100.00", BRL));

            account.debit(Money.of("40.00", BRL));

            assertThat(account.getBalance()).isEqualTo(Money.of("60.00", BRL));
        }

        @Test
        @DisplayName("Should throw InsufficientFundsException when debit exceeds balance")
        void shouldThrowInsufficientFundsWhenDebitExceedsBalance() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);
            account.credit(Money.of("50.00", BRL));

            assertThatThrownBy(() -> account.debit(Money.of("50.01", BRL)))
                    .isInstanceOf(InsufficientFundsException.class)
                    .satisfies(ex -> {
                        InsufficientFundsException ife = (InsufficientFundsException) ex;
                        assertThat(ife.getAccountId()).isEqualTo(account.getId());
                        assertThat(ife.getRequested()).isEqualTo(Money.of("50.01", BRL));
                        assertThat(ife.getCurrentBalance()).isEqualTo(Money.of("50.00", BRL));
                    });
        }

        @Test
        @DisplayName("Should reject non-positive debit amount")
        void shouldRejectNonPositiveDebit() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);

            assertThatThrownBy(() -> account.debit(Money.zero(BRL)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Debit amount must be positive");
        }

        @Test
        @DisplayName("Should reject debit with mismatched currency")
        void shouldRejectDebitWithMismatchedCurrency() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);
            account.credit(Money.of("100.00", BRL));

            assertThatThrownBy(() -> account.debit(Money.of("20.00", USD)))
                    .isInstanceOf(CurrencyMismatchException.class);
        }

        @Test
        @DisplayName("Should reject debit on closed account")
        void shouldRejectDebitOnClosedAccount() {
            Account account = Account.openAccount("12345-6", "Samuel", BRL);
            account.close();

            assertThatThrownBy(() -> account.debit(Money.of("10.00", BRL)))
                    .isInstanceOf(AccountNotActiveException.class);
        }
    }
}
