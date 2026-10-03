package com.sbaldasso.ledgercore.application.service;

import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferMoneyUseCase;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.AccountNotFoundException;
import com.sbaldasso.ledgercore.domain.model.Transaction;
import com.sbaldasso.ledgercore.domain.repository.AccountRepository;
import com.sbaldasso.ledgercore.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferMoneyService implements TransferMoneyUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferMoneyService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public TransferResult execute(TransferMoneyCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        // 1. Verificação de Idempotência: Se já foi processada, retorna o resultado existente sem duplicar
        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existingTransaction.isPresent()) {
            Transaction tx = existingTransaction.get();
            Account source = accountRepository.findById(command.sourceAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(command.sourceAccountId()));
            Account target = accountRepository.findById(command.targetAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(command.targetAccountId()));

            return new TransferResult(
                    tx.getId(),
                    source.getId(),
                    target.getId(),
                    command.amount(),
                    source.getBalance(),
                    target.getBalance(),
                    tx.getCreatedAt()
            );
        }

        // 2. Prevenção de Deadlock: Ordenação determinística de locks
        UUID firstLockId;
        UUID secondLockId;

        if (command.sourceAccountId().compareTo(command.targetAccountId()) < 0) {
            firstLockId = command.sourceAccountId();
            secondLockId = command.targetAccountId();
        } else {
            firstLockId = command.targetAccountId();
            secondLockId = command.sourceAccountId();
        }

        // 3. Aquisição dos locks pessimistas na ordem determinística
        Account firstAccount = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new AccountNotFoundException(firstLockId));

        Account secondAccount = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new AccountNotFoundException(secondLockId));

        // 4. Identifica origem e destino pós-aquisição de lock
        Account source = firstAccount.getId().equals(command.sourceAccountId()) ? firstAccount : secondAccount;
        Account target = firstAccount.getId().equals(command.targetAccountId()) ? firstAccount : secondAccount;

        // 5. Executa mutações de domínio (invariantes protegidos dentro das entidades)
        source.debit(command.amount());
        target.credit(command.amount());

        // 6. Registra o evento contábil no Livro-Razão com chave de idempotência
        Transaction transaction = Transaction.createTransfer(
                source.getId(),
                target.getId(),
                command.amount(),
                command.description(),
                command.idempotencyKey()
        );

        // 7. Persistência atômica
        accountRepository.save(source);
        accountRepository.save(target);
        Transaction savedTransaction = transactionRepository.save(transaction);

        return new TransferResult(
                savedTransaction.getId(),
                source.getId(),
                target.getId(),
                command.amount(),
                source.getBalance(),
                target.getBalance(),
                savedTransaction.getCreatedAt()
        );
    }
}
