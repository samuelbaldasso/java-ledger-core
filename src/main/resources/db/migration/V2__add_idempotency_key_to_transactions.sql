ALTER TABLE transactions ADD COLUMN idempotency_key VARCHAR(128) NOT NULL;
CREATE UNIQUE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);
