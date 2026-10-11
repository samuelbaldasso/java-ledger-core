-- Historical transfers cannot be backfilled: their original resulting balances were not stored.
CREATE TABLE transfer_receipts (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE REFERENCES transactions(id) ON DELETE RESTRICT,
    source_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    target_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255) NOT NULL,
    source_new_balance NUMERIC(19, 4) NOT NULL CHECK (source_new_balance >= 0),
    target_new_balance NUMERIC(19, 4) NOT NULL CHECK (target_new_balance >= 0),
    executed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CHECK (source_account_id <> target_account_id)
);
