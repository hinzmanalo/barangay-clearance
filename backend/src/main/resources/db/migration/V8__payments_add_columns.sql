-- V8: Add idempotency_expires_at and payment_method to payments table
ALTER TABLE payments
    ADD COLUMN idempotency_expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '24 hours'),
    ADD COLUMN payment_method         VARCHAR(10)  NOT NULL DEFAULT 'STUB'
                CHECK (payment_method IN ('STUB', 'CASH'));
