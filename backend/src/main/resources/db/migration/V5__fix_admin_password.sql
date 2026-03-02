-- V5: Fix admin password hash — corrects the incorrect hash seeded in V3.
-- Temporary password: Admin@1234  (BCrypt strength 12)
-- IMPORTANT: Change this password immediately after first login.
UPDATE users
SET password_hash = '$2y$12$Jp21lnJ006ABra6mQlI40O.LV7ATfjQiqC/BtdVLa3UZ0vJFTjoYi'
WHERE email = 'admin@barangay.local';
