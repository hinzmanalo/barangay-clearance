-- V3: Seed initial admin user
-- Temporary password: Admin@1234  (BCrypt strength 12)
-- IMPORTANT: Change this password immediately after first login.
-- See docs/ADMIN_SETUP.md for instructions.
INSERT INTO users (id, email, password_hash, first_name, last_name, role, status, must_change_password)
VALUES (
    gen_random_uuid(),
    'admin@barangay.local',
    '$2a$12$tuLjscE4ber2IriTHLiuAe0TFBqkxqnl5LsVHbBUSN.p9Mzn0uLwi',
    'System',
    'Admin',
    'ADMIN',
    'ACTIVE',
    TRUE
)
ON CONFLICT (email) DO NOTHING;
