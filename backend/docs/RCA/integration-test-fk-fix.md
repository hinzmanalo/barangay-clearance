# Integration Test FK Fix — Root Cause & Resolution

**Date:** March 4, 2026

---

## Issue 1 — `PaymentControllerIT.initiate_clearanceNotApproved_returns400` → 500

### Root Cause

`clearance_requests.requested_by` has a FK constraint `REFERENCES users(id)`.
`truncateAllTables()` wipes the `users` table as part of cleanup, so the fixed
`CLERK_ID` (`00000000-0000-0000-0000-000000000002`) — used as the `requested_by`
value when creating a walk-in clearance — no longer existed as a DB row. The FK
violation caused a `DataIntegrityViolationException` → 500, preventing the
clearance from even being created.

### Resolution

Added a `seedStaffUsers()` helper to `BaseIntegrationTest` that re-inserts the
three fixed staff rows (ADMIN, CLERK, APPROVER with their constant UUIDs) after
truncation. `PaymentControllerIT.setUp()` was updated to call it.

---

## Issue 2 — `ClearanceWorkflowIT.happyPath_registerActivateSubmitApprovePayRelease` → 500

### Root Cause

Same underlying cause — `truncateAllTables()` wiped the `users` table. Two FK
columns were affected:

- `clearance_requests.reviewed_by REFERENCES users(id)` — set to `APPROVER_ID`
  during the `/approve` call
- `payments.initiated_by_user_id REFERENCES users(id)` — set to `CLERK_ID`
  during the `/mark-paid` call

Both writes threw `DataIntegrityViolationException` → 500 because neither
`APPROVER_ID` nor `CLERK_ID` had corresponding rows in the truncated `users`
table.

### Resolution

Added `seedStaffUsers()` to `ClearanceWorkflowIT.setUp()`, using the same helper
introduced in Issue 1.

---

## Common Pattern

The `BaseIntegrationTest` JWT helpers (`asClerk()`, `asApprover()`, etc.) are
**stateless** — they generate valid tokens using fixed UUIDs without needing DB
rows. However, any **write operation** that stores one of those UUIDs into a
column with a FK to `users(id)` requires the corresponding row to exist in the
database.

**Rule:** Any integration test that performs staff-initiated writes must call
`seedStaffUsers()` after `truncateAllTables()` in its `@BeforeEach`.

### Affected FK columns in `V1__initial_schema.sql`

| Table                    | Column                 | Constraint                    |
|--------------------------|------------------------|-------------------------------|
| `refresh_tokens`         | `user_id`              | `NOT NULL REFERENCES users(id) ON DELETE CASCADE` |
| `residents`              | `user_id`              | `REFERENCES users(id) ON DELETE SET NULL` |
| `clearance_requests`     | `requested_by`         | `NOT NULL REFERENCES users(id)` |
| `clearance_requests`     | `reviewed_by`          | `REFERENCES users(id)` |
| `payments`               | `initiated_by_user_id` | `NOT NULL REFERENCES users(id)` |
| `audit_logs`             | `user_id`              | `REFERENCES users(id) ON DELETE SET NULL` |
