package com.sbaldasso.ledgercore.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_receipts")
public class TransferReceiptJpaEntity {
    @Id
    @Column(name = "idempotency_key", length = 128, nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "transaction_id", nullable = false, unique = true, updatable = false)
    private UUID transactionId;

    @Column(name = "source_account_id", nullable = false, updatable = false)
    private UUID sourceAccountId;

    @Column(name = "target_account_id", nullable = false, updatable = false)
    private UUID targetAccountId;

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(nullable = false, length = 255, updatable = false)
    private String description;

    @Column(name = "source_new_balance", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal sourceNewBalance;

    @Column(name = "target_new_balance", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal targetNewBalance;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    protected TransferReceiptJpaEntity() {
    }

    public TransferReceiptJpaEntity(String idempotencyKey, UUID transactionId, UUID sourceAccountId, UUID targetAccountId, BigDecimal amount, String currency, String description, BigDecimal sourceNewBalance, BigDecimal targetNewBalance, Instant executedAt) {
        this.idempotencyKey = idempotencyKey;
        this.transactionId = transactionId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.sourceNewBalance = sourceNewBalance;
        this.targetNewBalance = targetNewBalance;
        this.executedAt = executedAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getSourceNewBalance() {
        return sourceNewBalance;
    }

    public BigDecimal getTargetNewBalance() {
        return targetNewBalance;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }
}
