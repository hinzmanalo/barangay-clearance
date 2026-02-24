-- V6: Add purpose_other and copies to clearance_requests
ALTER TABLE clearance_requests
    ADD COLUMN IF NOT EXISTS purpose_other VARCHAR(255),
    ADD COLUMN IF NOT EXISTS copies        INTEGER NOT NULL DEFAULT 1;
