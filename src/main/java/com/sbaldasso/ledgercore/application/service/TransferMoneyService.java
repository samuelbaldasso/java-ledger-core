package com.sbaldasso.ledgercore.application.service;

import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferMoneyUseCase;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TransferMoneyService implements TransferMoneyUseCase {
    private final TransferExecutionService execution;

    public TransferMoneyService(TransferExecutionService execution) {
        this.execution = execution;
    }

    @Override
    public TransferResult execute(TransferMoneyCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");
        try {
            return execution.execute(command);
        } catch (DataIntegrityViolationException collision) {
            // The proxied execution has already rolled back, including balances and entries.
            // Recover only if a committed receipt exists; unrelated integrity errors still fail.
            return execution.findCompleted(command).orElseThrow(() -> collision);
        }
    }
}
