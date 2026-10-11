package com.sbaldasso.ledgercore.application.service;

import com.sbaldasso.ledgercore.application.idempotency.IdempotencyConflictException;
import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import com.sbaldasso.ledgercore.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferMoneyServiceTest {
    @Mock
    private TransferExecutionService execution;

    private final TransferMoneyCommand command = new TransferMoneyCommand(UUID.randomUUID(), UUID.randomUUID(),
            Money.of("100", "BRL"), "Payment", "key");

    @Test
    void shouldRecoverCommittedWinnerAfterIntegrityFailure() {
        var winner = new TransferResult(UUID.randomUUID(), command.sourceAccountId(), command.targetAccountId(),
                command.amount(), Money.zero("BRL"), command.amount(), Instant.now());
        when(execution.execute(command)).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(execution.findCompleted(command)).thenReturn(Optional.of(winner));

        assertThat(new TransferMoneyService(execution).execute(command)).isEqualTo(winner);
    }

    @Test
    void shouldNotHideIntegrityFailureWithoutCommittedWinner() {
        var failure = new DataIntegrityViolationException("unrelated constraint");
        when(execution.execute(command)).thenThrow(failure);
        when(execution.findCompleted(command)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new TransferMoneyService(execution).execute(command)).isSameAs(failure);
    }

    @Test
    void shouldRejectDifferentRequestAfterCollision() {
        when(execution.execute(command)).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(execution.findCompleted(command)).thenThrow(new IdempotencyConflictException());

        assertThatThrownBy(() -> new TransferMoneyService(execution).execute(command))
                .isInstanceOf(IdempotencyConflictException.class);
    }
}
