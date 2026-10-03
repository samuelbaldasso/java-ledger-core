package com.sbaldasso.ledgercore.application.usecase;

public interface TransferMoneyUseCase {

    TransferResult execute(TransferMoneyCommand command);
}
