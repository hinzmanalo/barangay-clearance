-- V1: Initial schema — all 9 tables
-- Enable uuid generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ─────────────────────────────────────────
-- 1. users
-- ─────────────────────────────────────────
CREATE TABLE users (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    role                VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','CLERK','APPROVER','RESIDENT')),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    must_change_password BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role_status ON users (role, status);

-- ─────────────────────────────────────────
-- 2. refresh_tokens
-- ─────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────
-- 3. residents
-- ─────────────────────────────────────────
CREATE TABLE residents (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        REFERENCES users(id) ON DELETE SET NULL,
    first_name      VARCHAR(100) NOT NULL,
    middle_name     VARCHAR(100),
    last_name       VARCHAR(100) NOT NULL,
    birth_date      DATE         NOT NULL,
    gender          VARCHAR(10)  NOT NULL CHECK (gender IN ('MALE','FEMALE','OTHER')),
    address         TEXT         NOT NULL,
    contact_number  VARCHAR(20),
    email           VARCHAR(255),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_residents_name ON residents (lower(last_name), lower(first_name));

-- ─────────────────────────────────────────
-- 4. barangay_settings  (singleton — id must be 1)
-- ─────────────────────────────────────────
CREATE TABLE barangay_settings (
    id              INTEGER     PRIMARY KEY CHECK (id = 1),
    barangay_name   VARCHAR(255) NOT NULL DEFAULT 'Barangay',
    municipality    VARCHAR(255) NOT NULL DEFAULT 'Municipality',
    province        VARCHAR(255) NOT NULL DEFAULT 'Province',
    captain_name    VARCHAR(255) NOT NULL DEFAULT 'Barangay Captain',
    logo            BYTEA,
    logo_mime_type  VARCHAR(50),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────
-- 5. fee_config  (singleton — id must be 1)
-- ─────────────────────────────────────────
CREATE TABLE fee_config (
    id              INTEGER     PRIMARY KEY CHECK (id = 1),
    standard_fee    NUMERIC(10,2) NOT NULL DEFAULT 50.00,
    rush_fee        NUMERIC(10,2) NOT NULL DEFAULT 100.00,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────
-- 6. clearance_number_sequence
-- ─────────────────────────────────────────
CREATE TABLE clearance_number_sequence (
    year_month  CHAR(7)  PRIMARY KEY,  -- e.g. '2025-02'
    last_seq    INTEGER  NOT NULL DEFAULT 0
);

-- ─────────────────────────────────────────
-- 7. clearance_requests
-- ─────────────────────────────────────────
CREATE TABLE clearance_requests (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    clearance_number VARCHAR(12),
    resident_id     UUID        NOT NULL REFERENCES residents(id),
    requested_by    UUID        NOT NULL REFERENCES users(id),
    purpose         VARCHAR(50)  NOT NULL,
    urgency         VARCHAR(10)  NOT NULL DEFAULT 'STANDARD' CHECK (urgency IN ('STANDARD','RUSH')),
    fee_amount      NUMERIC(10,2) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT','FOR_APPROVAL','APPROVED','REJECTED','RELEASED')),
    payment_status  VARCHAR(10)  NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID','PAID')),
    notes           TEXT,
    reviewed_by     UUID        REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    issued_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cr_status ON clearance_requests (status);
CREATE INDEX idx_cr_issued_at ON clearance_requests (issued_at);

-- ─────────────────────────────────────────
-- 8. payments
-- ─────────────────────────────────────────
CREATE TABLE payments (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    clearance_request_id UUID       NOT NULL REFERENCES clearance_requests(id),
    amount              NUMERIC(10,2) NOT NULL,
    idempotency_key     VARCHAR(64)  NOT NULL,
    initiated_by_user_id UUID       NOT NULL REFERENCES users(id),
    provider            VARCHAR(50)  NOT NULL DEFAULT 'STUB',
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    response_body       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_payments_idempotency ON payments (idempotency_key, initiated_by_user_id);

-- ─────────────────────────────────────────
-- 9. audit_logs  (append-only)
-- ─────────────────────────────────────────
CREATE TABLE audit_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   UUID,
    details     TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
