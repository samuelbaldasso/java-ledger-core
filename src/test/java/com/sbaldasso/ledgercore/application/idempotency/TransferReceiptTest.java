package com.sbaldasso.ledgercore.application.idempotency;

import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import com.sbaldasso.ledgercore.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferReceiptTest {
    private static final UUID SOURCE = UUID.randomUUID();
    private static final UUID TARGET = UUID.randomUUID();
    private static final TransferMoneyCommand ORIGINAL =
            new TransferMoneyCommand(SOURCE, TARGET, Money.of("100", "BRL"), "Payment", "key");
    private static final TransferResult RESULT = new TransferResult(UUID.randomUUID(), SOURCE, TARGET,
            ORIGINAL.amount(), Money.of("900", "BRL"), Money.of("100", "BRL"), Instant.now());
    private final TransferReceipt receipt = new TransferReceipt(ORIGINAL, RESULT);

    @Test
    void shouldReplayEquivalentMonetaryValues() {
        var retry = new TransferMoneyCommand(SOURCE, TARGET, Money.of("100.00", "BRL"), "Payment", "key");
        assertThat(receipt.replay(retry)).isEqualTo(RESULT);
    }

    @ParameterizedTest
    @MethodSource("differentRequests")
    void shouldRejectChangesToAnyRequestField(TransferMoneyCommand retry) {
        assertThatThrownBy(() -> receipt.replay(retry)).isInstanceOf(IdempotencyConflictException.class);
    }

    static Stream<TransferMoneyCommand> differentRequests() {
        return Stream.of(
                new TransferMoneyCommand(UUID.randomUUID(), TARGET, ORIGINAL.amount(), "Payment", "key"),
                new TransferMoneyCommand(SOURCE, UUID.randomUUID(), ORIGINAL.amount(), "Payment", "key"),
                new TransferMoneyCommand(SOURCE, TARGET, Money.of("900", "BRL"), "Payment", "key"),
                new TransferMoneyCommand(SOURCE, TARGET, Money.of("100", "USD"), "Payment", "key"),
                new TransferMoneyCommand(SOURCE, TARGET, ORIGINAL.amount(), "Different", "key"),
                new TransferMoneyCommand(SOURCE, TARGET, ORIGINAL.amount(), "Payment", "different-key"));
    }
}
