package com.sbaldasso.ledgercore.application.service;

import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.AccountNotFoundException;
import com.sbaldasso.ledgercore.domain.model.CurrencyMismatchException;
import com.sbaldasso.ledgercore.domain.model.InsufficientFundsException;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.domain.model.Transaction;
import com.sbaldasso.ledgercore.domain.repository.AccountRepository;
import com.sbaldasso.ledgercore.domain.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferMoneyServiceTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private TransferMoneyService transferMoneyService;

    @BeforeEach
    void setUp() {
        transferMoneyService = new TransferMoneyService(accountRepository, transactionRepository);
    }

    @Test
    @DisplayName("Should transfer money successfully and record double-entry transaction")
    void shouldTransferMoneySuccessfully() {
        Account source = Account.openAccount("111-1", "Alice", BRL);
        source.credit(Money.of("200.00", BRL));

        Account target = Account.openAccount("222-2", "Bob", BRL);
        target.credit(Money.of("50.00", BRL));

        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferMoneyCommand command = new TransferMoneyCommand(
                source.getId(),
                target.getId(),
                Money.of("75.00", BRL),
                "Lunch split",
                "key-1"
        );

        TransferResult result = transferMoneyService.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.transferredAmount()).isEqualTo(Money.of("75.00", BRL));
        assertThat(result.sourceNewBalance()).isEqualTo(Money.of("125.00", BRL));
        assertThat(result.targetNewBalance()).isEqualTo(Money.of("125.00", BRL));

        verify(accountRepository).save(source);
        verify(accountRepository).save(target);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should return existing transaction result when idempotency key is already processed without duplicate debit or credit")
    void shouldReturnExistingResultWhenIdempotencyKeyAlreadyProcessed() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Money amount = Money.of("50.00", BRL);
        String idempotencyKey = "existing-idemp-key";

        Account source = Account.reconstitute(sourceId, "111", "Alice", Money.of("150.00", BRL), com.sbaldasso.ledgercore.domain.model.AccountStatus.ACTIVE, java.time.Instant.now());
        Account target = Account.reconstitute(targetId, "222", "Bob", Money.of("100.00", BRL), com.sbaldasso.ledgercore.domain.model.AccountStatus.ACTIVE, java.time.Instant.now());

        Transaction existingTx = Transaction.createTransfer(sourceId, targetId, amount, "Already processed", idempotencyKey);

        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingTx));
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(accountRepository.findById(targetId)).thenReturn(Optional.of(target));

        TransferMoneyCommand command = new TransferMoneyCommand(
                sourceId,
                targetId,
                amount,
                "Already processed",
                idempotencyKey
        );

        TransferResult result = transferMoneyService.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.transactionId()).isEqualTo(existingTx.getId());
        assertThat(result.transferredAmount()).isEqualTo(amount);
        assertThat(result.sourceNewBalance()).isEqualTo(Money.of("150.00", BRL));
        assertThat(result.targetNewBalance()).isEqualTo(Money.of("100.00", BRL));

        // Zero side effects: no locks acquired, no saves performed!
        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should acquire locks in deterministic order to prevent deadlocks (min UUID first)")
    void shouldAcquireLocksInDeterministicOrder() {
        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Account acc1 = Account.reconstitute(id1, "1", "User 1", Money.of("100.00", BRL), com.sbaldasso.ledgercore.domain.model.AccountStatus.ACTIVE, java.time.Instant.now());
        Account acc2 = Account.reconstitute(id2, "2", "User 2", Money.of("100.00", BRL), com.sbaldasso.ledgercore.domain.model.AccountStatus.ACTIVE, java.time.Instant.now());

        when(transactionRepository.findByIdempotencyKey("rev-key")).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(id1)).thenReturn(Optional.of(acc1));
        when(accountRepository.findByIdForUpdate(id2)).thenReturn(Optional.of(acc2));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Transfer from id2 to id1 (reverse order of UUID comparison)
        TransferMoneyCommand command = new TransferMoneyCommand(id2, id1, Money.of("10.00", BRL), "Reverse transfer", "rev-key");

        transferMoneyService.execute(command);

        // Verify id1 was locked BEFORE id2
        InOrder inOrder = inOrder(accountRepository);
        inOrder.verify(accountRepository).findByIdForUpdate(id1);
        inOrder.verify(accountRepository).findByIdForUpdate(id2);
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist")
    void shouldThrowWhenAccountNotFound() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        when(transactionRepository.findByIdempotencyKey("missing-acc-key")).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        TransferMoneyCommand command = new TransferMoneyCommand(sourceId, targetId, Money.of("10.00", BRL), "Transfer", "missing-acc-key");

        assertThatThrownBy(() -> transferMoneyService.execute(command))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate InsufficientFundsException when source balance is inadequate")
    void shouldPropagateInsufficientFunds() {
        Account source = Account.openAccount("111-1", "Alice", BRL);
        source.credit(Money.of("20.00", BRL));

        Account target = Account.openAccount("222-2", "Bob", BRL);

        when(transactionRepository.findByIdempotencyKey("insufficient-key")).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));

        TransferMoneyCommand command = new TransferMoneyCommand(
                source.getId(),
                target.getId(),
                Money.of("50.00", BRL),
                "Exceeds balance",
                "insufficient-key"
        );

        assertThatThrownBy(() -> transferMoneyService.execute(command))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate CurrencyMismatchException when accounts have different currencies")
    void shouldPropagateCurrencyMismatch() {
        Account source = Account.openAccount("111-1", "Alice", BRL);
        source.credit(Money.of("100.00", BRL));

        Account target = Account.openAccount("222-2", "Bob", USD);

        when(transactionRepository.findByIdempotencyKey("cross-currency-key")).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));

        TransferMoneyCommand command = new TransferMoneyCommand(
                source.getId(),
                target.getId(),
                Money.of("50.00", BRL),
                "Cross-currency",
                "cross-currency-key"
        );

        assertThatThrownBy(() -> transferMoneyService.execute(command))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
