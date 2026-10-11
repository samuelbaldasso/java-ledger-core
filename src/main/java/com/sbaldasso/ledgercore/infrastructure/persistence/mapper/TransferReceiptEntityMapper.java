package com.sbaldasso.ledgercore.infrastructure.persistence.mapper;

import com.sbaldasso.ledgercore.application.idempotency.TransferReceipt;
import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.infrastructure.persistence.entity.TransferReceiptJpaEntity;

import java.util.Currency;

public final class TransferReceiptEntityMapper {
    private TransferReceiptEntityMapper() {
    }

    public static TransferReceiptJpaEntity toEntity(TransferReceipt receipt) {
        TransferMoneyCommand request = receipt.request();
        TransferResult result = receipt.result();
        return new TransferReceiptJpaEntity(request.idempotencyKey(), result.transactionId(),
                request.sourceAccountId(), request.targetAccountId(), request.amount().amount(),
                request.amount().currency().getCurrencyCode(), request.description(),
                result.sourceNewBalance().amount(), result.targetNewBalance().amount(), result.executedAt());
    }

    public static TransferReceipt toReceipt(TransferReceiptJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        Money amount = Money.of(entity.getAmount(), currency);
        TransferMoneyCommand request = new TransferMoneyCommand(entity.getSourceAccountId(),
                entity.getTargetAccountId(), amount, entity.getDescription(), entity.getIdempotencyKey());
        TransferResult result = new TransferResult(entity.getTransactionId(), entity.getSourceAccountId(),
                entity.getTargetAccountId(), amount, Money.of(entity.getSourceNewBalance(), currency),
                Money.of(entity.getTargetNewBalance(), currency), entity.getExecutedAt());
        return new TransferReceipt(request, result);
    }
}
