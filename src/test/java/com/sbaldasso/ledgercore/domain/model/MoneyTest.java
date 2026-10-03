package com.sbaldasso.ledgercore.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency JPY = Currency.getInstance("JPY");

    @Nested
    @DisplayName("Creation and Invariants")
    class CreationAndInvariants {

        @Test
        @DisplayName("Should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> new Money(null, BRL))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        @Test
        @DisplayName("Should reject null currency")
        void shouldRejectNullCurrency() {
            assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Currency cannot be null");
        }

        @Test
        @DisplayName("Should normalize scale according to currency default fraction digits")
        void shouldNormalizeScaleAccordingToCurrency() {
            Money brlMoney = Money.of("10", BRL);
            assertThat(brlMoney.amount()).isEqualTo(new BigDecimal("10.00"));
            assertThat(brlMoney.amount().scale()).isEqualTo(2);

            Money jpyMoney = Money.of("10.00", JPY);
            assertThat(jpyMoney.amount()).isEqualTo(new BigDecimal("10"));
            assertThat(jpyMoney.amount().scale()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should consider equal money with different initial scales")
        void shouldConsiderEqualMoneyWithDifferentInitialScales() {
            Money m1 = Money.of("100", BRL);
            Money m2 = Money.of("100.00", BRL);
            Money m3 = Money.of("100.000", BRL);

            assertThat(m1).isEqualTo(m2);
            assertThat(m2).isEqualTo(m3);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        }

        @Test
        @DisplayName("Should create zero money correctly")
        void shouldCreateZeroMoneyCorrectly() {
            Money zero = Money.zero(BRL);

            assertThat(zero.isZero()).isTrue();
            assertThat(zero.amount()).isEqualTo(new BigDecimal("0.00"));
            assertThat(zero.currency()).isEqualTo(BRL);
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperations {

        @Test
        @DisplayName("Should add money with the same currency")
        void shouldAddMoneyWithSameCurrency() {
            Money m1 = Money.of("100.50", BRL);
            Money m2 = Money.of("49.50", BRL);

            Money result = m1.add(m2);

            assertThat(result).isEqualTo(Money.of("150.00", BRL));
            // Immutability check
            assertThat(m1).isEqualTo(Money.of("100.50", BRL));
            assertThat(m2).isEqualTo(Money.of("49.50", BRL));
        }

        @Test
        @DisplayName("Should throw CurrencyMismatchException when adding different currencies")
        void shouldThrowWhenAddingDifferentCurrencies() {
            Money brl = Money.of("100.00", BRL);
            Money usd = Money.of("50.00", USD);

            assertThatThrownBy(() -> brl.add(usd))
                    .isInstanceOf(CurrencyMismatchException.class)
                    .hasMessageContaining("expected BRL, but got USD");
        }

        @Test
        @DisplayName("Should subtract money with the same currency")
        void shouldSubtractMoneyWithSameCurrency() {
            Money m1 = Money.of("100.00", BRL);
            Money m2 = Money.of("35.50", BRL);

            Money result = m1.subtract(m2);

            assertThat(result).isEqualTo(Money.of("64.50", BRL));
        }

        @Test
        @DisplayName("Should throw CurrencyMismatchException when subtracting different currencies")
        void shouldThrowWhenSubtractingDifferentCurrencies() {
            Money brl = Money.of("100.00", BRL);
            Money usd = Money.of("10.00", USD);

            assertThatThrownBy(() -> brl.subtract(usd))
                    .isInstanceOf(CurrencyMismatchException.class);
        }

        @Test
        @DisplayName("Should negate money correctly")
        void shouldNegateMoneyCorrectly() {
            Money positive = Money.of("50.00", BRL);
            Money negated = positive.negate();

            assertThat(negated).isEqualTo(Money.of("-50.00", BRL));
            assertThat(negated.isNegative()).isTrue();
        }
    }

    @Nested
    @DisplayName("Comparisons and Predicates")
    class ComparisonsAndPredicates {

        @Test
        @DisplayName("Should compare amounts correctly when currency is the same")
        void shouldCompareAmountsCorrectly() {
            Money m1 = Money.of("100.00", BRL);
            Money m2 = Money.of("50.00", BRL);
            Money m3 = Money.of("100.00", BRL);

            assertThat(m1.isGreaterThan(m2)).isTrue();
            assertThat(m2.isLessThan(m1)).isTrue();
            assertThat(m1.isGreaterThanOrEqual(m3)).isTrue();
            assertThat(m1.isLessThanOrEqual(m3)).isTrue();
            assertThat(m1.compareTo(m2)).isPositive();
        }

        @Test
        @DisplayName("Should throw CurrencyMismatchException on comparisons with different currencies")
        void shouldThrowOnComparisonWithDifferentCurrencies() {
            Money brl = Money.of("100.00", BRL);
            Money usd = Money.of("100.00", USD);

            assertThatThrownBy(() -> brl.isGreaterThan(usd))
                    .isInstanceOf(CurrencyMismatchException.class);
        }

        @Test
        @DisplayName("Should identify positive, negative and zero amounts")
        void shouldIdentifySignPredicates() {
            Money positive = Money.of("0.01", BRL);
            Money zero = Money.of("0.00", BRL);
            Money negative = Money.of("-0.01", BRL);

            assertThat(positive.isPositive()).isTrue();
            assertThat(positive.isZero()).isFalse();
            assertThat(positive.isNegative()).isFalse();

            assertThat(zero.isPositive()).isFalse();
            assertThat(zero.isZero()).isTrue();
            assertThat(zero.isNegative()).isFalse();

            assertThat(negative.isPositive()).isFalse();
            assertThat(negative.isZero()).isFalse();
            assertThat(negative.isNegative()).isTrue();
        }
    }
}
