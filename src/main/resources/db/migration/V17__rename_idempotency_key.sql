-- Drop the column if it was added in V15
ALTER TABLE idempotency_records DROP COLUMN IF EXISTS idempotency_key;

-- Rename the original primary key constraint and column
ALTER TABLE idempotency_records RENAME COLUMN key TO idempotency_key;
