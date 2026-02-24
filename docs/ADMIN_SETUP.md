# Admin Setup Guide

## Initial Admin Credentials

After running Flyway migrations, an initial admin account is seeded:

- **Email:** admin@barangay.local
- **Temporary Password:** Admin@1234

## First Login Steps

1. Navigate to `http://localhost:3000/login`
2. Log in with the credentials above
3. You will be prompted to change your password (`must_change_password = true`)
4. Choose a strong password (min 12 chars, upper + lower + digit + special)
5. Update barangay settings (name, captain, logo) under Settings

## Password Hash Details

The seeded password hash is BCrypt at strength 12 for `Admin@1234`.

**Do not use this password in production.** Change it on first login.
