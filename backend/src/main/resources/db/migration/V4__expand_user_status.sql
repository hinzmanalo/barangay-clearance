-- V4: Expand users.status constraint to include PENDING_VERIFICATION, REJECTED, DEACTIVATED
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check;

ALTER TABLE users
    ADD CONSTRAINT users_status_check
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING_VERIFICATION', 'REJECTED', 'DEACTIVATED'));
