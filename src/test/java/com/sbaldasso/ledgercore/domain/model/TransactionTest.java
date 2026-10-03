package com.sbaldasso.ledgercore.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    @Nested
    @DisplayName("Direct Transfer Factory")
    class DirectTransferFactory {

        @Test
        @DisplayName("Should create balanced 2-leg transfer transaction with idempotency key")
        void shouldCreateBalancedTransfer() {
            UUID sourceId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            Money amount = Money.of("150.00", BRL);
            String idempotencyKey = "tx-key-12345";

            Transaction tx = Transaction.createTransfer(sourceId, targetId, amount, "Pix transfer", idempotencyKey);

            assertThat(tx.getId()).isNotNull();
            assertThat(tx.getIdempotencyKey()).isEqualTo(idempotencyKey);
            assertThat(tx.getDescription()).isEqualTo("Pix transfer");
            assertThat(tx.getCreatedAt()).isNotNull();
            assertThat(tx.getEntries()).hasSize(2);

            Entry debitEntry = tx.getEntries().get(0);
            assertThat(debitEntry.accountId()).isEqualTo(sourceId);
            assertThat(debitEntry.type()).isEqualTo(EntryType.DEBIT);
            assertThat(debitEntry.amount()).isEqualTo(amount);

            Entry creditEntry = tx.getEntries().get(1);
            assertThat(creditEntry.accountId()).isEqualTo(targetId);
            assertThat(creditEntry.type()).isEqualTo(EntryType.CREDIT);
            assertThat(creditEntry.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("Should reject transfer between identical accounts")
        void shouldRejectTransferBetweenIdenticalAccounts() {
            UUID sameId = UUID.randomUUID();
            Money amount = Money.of("50.00", BRL);

            assertThatThrownBy(() -> Transaction.createTransfer(sameId, sameId, amount, "Transfer"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source and target accounts must be different");
        }

        @Test
        @DisplayName("Should reject blank description")
        void shouldRejectBlankDescription() {
            UUID sourceId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            assertThatThrownBy(() -> Transaction.createTransfer(sourceId, targetId, Money.of("10.00", BRL), "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Description cannot be blank");
        }

        @Test
        @DisplayName("Should reject blank idempotency key")
        void shouldRejectBlankIdempotencyKey() {
            UUID sourceId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            assertThatThrownBy(() -> Transaction.createTransfer(sourceId, targetId, Money.of("10.00", BRL), "Transfer", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IdempotencyKey cannot be blank");
        }
    }

    @Nested
    @DisplayName("Double-Entry Invariants and Builder")
    class DoubleEntryInvariantsAndBuilder {

        @Test
        @DisplayName("Should create multi-leg balanced transaction with fee and custom idempotency key")
        void shouldCreateMultiLegBalancedTransaction() {
            UUID payer = UUID.randomUUID();
            UUID payee = UUID.randomUUID();
            UUID bankRevenue = UUID.randomUUID();

            Transaction tx = Transaction.builder("Payment with processing fee")
                    .idempotencyKey("payment-split-999")
                    .debit(payer, Money.of("102.50", BRL))
                    .credit(payee, Money.of("100.00", BRL))
                    .credit(bankRevenue, Money.of("2.50", BRL))
                    .build();

            assertThat(tx.getEntries()).hasSize(3);
            assertThat(tx.getIdempotencyKey()).isEqualTo("payment-split-999");
        }

        @Test
        @DisplayName("Should reject unbalanced transaction where debits != credits")
        void shouldRejectUnbalancedTransaction() {
            UUID payer = UUID.randomUUID();
            UUID payee = UUID.randomUUID();

            assertThatThrownBy(() -> Transaction.builder("Unbalanced transfer")
                    .debit(payer, Money.of("100.00", BRL))
                    .credit(payee, Money.of("90.00", BRL))
                    .build())
                    .isInstanceOf(UnbalancedTransactionException.class)
                    .satisfies(ex -> {
                        UnbalancedTransactionException ute = (UnbalancedTransactionException) ex;
                        assertThat(ute.getTotalDebits()).isEqualTo(Money.of("100.00", BRL));
                        assertThat(ute.getTotalCredits()).isEqualTo(Money.of("90.00", BRL));
                    });
        }

        @Test
        @DisplayName("Should reject transaction with less than 2 entries")
        void shouldRejectLessThanTwoEntries() {
            UUID payer = UUID.randomUUID();

            assertThatThrownBy(() -> Transaction.builder("Single leg")
                    .debit(payer, Money.of("50.00", BRL))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain at least 2 entries");
        }

        @Test
        @DisplayName("Should reject transaction with mixed currencies")
        void shouldRejectMixedCurrencies() {
            UUID payer = UUID.randomUUID();
            UUID payee = UUID.randomUUID();

            assertThatThrownBy(() -> Transaction.builder("Cross-currency transfer")
                    .debit(payer, Money.of("100.00", BRL))
                    .credit(payee, Money.of("100.00", USD))
                    .build())
                    .isInstanceOf(CurrencyMismatchException.class);
        }

        @Test
        @DisplayName("Should reject transaction containing only debits or only credits")
        void shouldRejectAllDebits() {
            UUID acc1 = UUID.randomUUID();
            UUID acc2 = UUID.randomUUID();

            assertThatThrownBy(() -> Transaction.builder("All debits")
                    .debit(acc1, Money.of("50.00", BRL))
                    .debit(acc2, Money.of("50.00", BRL))
                    .build())
                    .isInstanceOf(UnbalancedTransactionException.class);
        }

        @Test
        @DisplayName("Should guarantee entries list is unmodifiable")
        void shouldGuaranteeEntriesListIsUnmodifiable() {
            UUID sourceId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            Transaction tx = Transaction.createTransfer(sourceId, targetId, Money.of("50.00", BRL), "Transfer");
            List<Entry> entries = tx.getEntries();

            assertThatThrownBy(() -> entries.add(Entry.debit(UUID.randomUUID(), Money.of("10.00", BRL))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
