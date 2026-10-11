package com.sbaldasso.ledgercore.application.service;

import com.sbaldasso.ledgercore.application.idempotency.IdempotencyResultUnavailableException;
import com.sbaldasso.ledgercore.application.idempotency.TransferReceipt;
import com.sbaldasso.ledgercore.application.idempotency.TransferReceiptRepository;
import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.AccountNotFoundException;
import com.sbaldasso.ledgercore.domain.model.Transaction;
import com.sbaldasso.ledgercore.domain.repository.AccountRepository;
import com.sbaldasso.ledgercore.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TransferExecutionService {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final TransferReceiptRepository receipts;

    public TransferExecutionService(AccountRepository accounts, TransactionRepository transactions,
                                    TransferReceiptRepository receipts) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.receipts = receipts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public TransferResult execute(TransferMoneyCommand command) {
        Optional<TransferReceipt> existing = receipts.findByKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get().replay(command);
        }

        // Old transactions have no balance snapshot. Never invent a response or repeat their debit.
        if (transactions.findByIdempotencyKey(command.idempotencyKey()).isPresent()) {
            // A concurrent winner may have committed between the two reads.
            return receipts.findByKey(command.idempotencyKey())
                    .map(receipt -> receipt.replay(command))
                    .orElseThrow(IdempotencyResultUnavailableException::new);
        }

        UUID firstId = command.sourceAccountId().compareTo(command.targetAccountId()) < 0
                ? command.sourceAccountId() : command.targetAccountId();
        UUID secondId = firstId.equals(command.sourceAccountId())
                ? command.targetAccountId() : command.sourceAccountId();
        Account first = accounts.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accounts.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        // A retry may have waited for the winner's account locks, which can exhaust the balance.
        existing = receipts.findByKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get().replay(command);
        }

        Account source = first.getId().equals(command.sourceAccountId()) ? first : second;
        Account target = first.getId().equals(command.targetAccountId()) ? first : second;
        source.debit(command.amount());
        target.credit(command.amount());

        Transaction transaction = Transaction.createTransfer(source.getId(), target.getId(),
                command.amount(), command.description(), command.idempotencyKey());
        accounts.save(source);
        accounts.save(target);
        Transaction saved = transactions.save(transaction);
        TransferResult result = new TransferResult(saved.getId(), source.getId(), target.getId(),
                command.amount(), source.getBalance(), target.getBalance(), saved.getCreatedAt());
        receipts.insert(new TransferReceipt(command, result));
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Optional<TransferResult> findCompleted(TransferMoneyCommand command) {
        return receipts.findByKey(command.idempotencyKey()).map(receipt -> receipt.replay(command));
    }
}
