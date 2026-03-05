# Phase 12 — Audit Logging

> **Goal:** Implement a complete audit trail that records every state-changing operation in the system, leveraging the existing `audit_logs` table and `ClearanceStatusChangedEvent` hook.

**Status:** � Complete
**Depends on:** Phase 0 (table exists), Phase 1–8 (services to instrument)
**Parallel with:** Phase 9 (Testing), Phase 10 (Deployment)

---

## Current State Assessment

| Component                              | Status                                    |
| -------------------------------------- | ----------------------------------------- |
| `audit_logs` table (V1 migration)      | Exists — schema ready                     |
| `AuditLog` entity                      | Missing                                   |
| `AuditLogRepository`                   | Missing                                   |
| `AuditService`                         | Missing                                   |
| `ClearanceStatusChangedEvent` listener | Missing — event published but no listener |
| Audit calls in services                | None — only `log.info()` statements       |
| IP address extraction                  | Not implemented                           |
| Frontend audit log viewer              | Not implemented                           |

---

## Deliverables

### Backend

#### 1. AuditLog Entity & Repository (`shared/audit/`)

Create under `shared/audit/` (cross-cutting concern, used by all modules):

**`AuditLog.java`** — JPA entity mapping to `audit_logs` table:

- Fields: `id` (UUID), `userId` (UUID, nullable), `action` (String, 100), `entityType` (String, 50), `entityId` (UUID), `details` (String/TEXT), `ipAddress` (String, 45), `createdAt` (Instant)
- `@Entity`, `@Table(name = "audit_logs")`
- Immutable: no setters after construction; use `@Builder` + `@AllArgsConstructor` + `@NoArgsConstructor(access = PROTECTED)`
- `createdAt` uses `@CreationTimestamp` or DB default

**`AuditLogRepository.java`** — Spring Data JPA:

- `JpaRepository<AuditLog, UUID>` + `JpaSpecificationExecutor<AuditLog>`
- Derived query: `findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable)` — for viewing history of a specific entity
- Derived query: `findByUserId(UUID userId, Pageable pageable)` — for viewing a user's actions
- Dynamic filters (list endpoint) handled via `JpaSpecificationExecutor` + `SpecificationBuilder<AuditLog>`

#### 2. AuditService (`shared/audit/`)

**`AuditService.java`**:

- `@Service` with `@Transactional(propagation = REQUIRES_NEW)` — audit writes must not roll back with the parent transaction
- Primary method:
  ```java
  void log(UUID userId, String action, String entityType, UUID entityId, String details)
  ```
- IP address resolved internally from `RequestContextHolder` → `HttpServletRequest.getRemoteAddr()`
- Null-safe: if no request context (e.g., background job), `ipAddress` is null
- Async consideration: use `@Async` with a dedicated thread pool (`audit-pool`, core=2, max=5) so audit writes don't add latency to the main request. Fallback: synchronous write if async executor is saturated.

#### 3. IP Address Extraction

Use `RequestContextHolder.getRequestAttributes()` to get the current `HttpServletRequest`:

```java
private String resolveIpAddress() {
    ServletRequestAttributes attrs = (ServletRequestAttributes)
        RequestContextHolder.getRequestAttributes();
    if (attrs == null) return null;
    HttpServletRequest request = attrs.getRequest();
    String xff = request.getHeader("X-Forwarded-For");
    return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
}
```

This handles both direct connections and requests behind Nginx reverse proxy.

#### 4. Audit Action Constants

**`AuditAction.java`** — constants class (not enum, for extensibility):

| Constant                   | Module    | Triggered By                                        |
| -------------------------- | --------- | --------------------------------------------------- |
| `USER_REGISTERED`          | identity  | `AuthService.register()`                            |
| `USER_LOGIN`               | identity  | `AuthService.login()`                               |
| `USER_LOGIN_FAILED`        | identity  | `AuthService.login()` (invalid credentials)         |
| `USER_LOGOUT`              | identity  | `AuthService.logout()`                              |
| `USER_TOKEN_REFRESHED`     | identity  | `AuthService.refresh()`                             |
| `USER_PASSWORD_CHANGED`    | identity  | `AuthService.changePassword()`                      |
| `STAFF_CREATED`            | identity  | `UserService.createStaff()`                         |
| `STAFF_ACTIVATED`          | identity  | `UserService.activate()`                            |
| `STAFF_DEACTIVATED`        | identity  | `UserService.deactivate()`                          |
| `STAFF_ROLE_CHANGED`       | identity  | `UserService.updateRole()`                          |
| `STAFF_PASSWORD_RESET`     | identity  | `UserService.adminResetPassword()`                  |
| `RESIDENT_CREATED`         | residents | `ResidentService.create()`                          |
| `RESIDENT_UPDATED`         | residents | `ResidentService.update()`                          |
| `RESIDENT_ACTIVATED`       | residents | `ResidentService.activatePortalAccount()`           |
| `CLEARANCE_SUBMITTED`      | clearance | `ClearanceService.submit()`                         |
| `CLEARANCE_APPROVED`       | clearance | `ClearanceService.approve()`                        |
| `CLEARANCE_REJECTED`       | clearance | `ClearanceService.reject()`                         |
| `CLEARANCE_RESUBMITTED`    | clearance | `ClearanceService.resubmit()`                       |
| `CLEARANCE_RELEASED`       | clearance | `ClearanceService.release()`                        |
| `CLEARANCE_PDF_DOWNLOADED` | clearance | `ClearanceController.downloadPdf()`                 |
| `PAYMENT_INITIATED`        | payments  | `PaymentService.initiate()`                         |
| `PAYMENT_SUCCESS`          | payments  | `PaymentService.initiate()` (after gateway success) |
| `PAYMENT_FAILED`           | payments  | `PaymentService.initiate()` (after gateway failure) |
| `PAYMENT_CASH_RECORDED`    | payments  | `PaymentService.markPaid()`                         |
| `SETTINGS_UPDATED`         | settings  | `SettingsService.updateSettings()`                  |
| `SETTINGS_LOGO_UPLOADED`   | settings  | `SettingsService.uploadLogo()`                      |
| `FEES_UPDATED`             | settings  | `SettingsService.updateFees()`                      |

#### 5. Instrument Existing Services

Add `AuditService.log()` calls to each service method listed above. Example pattern:

```java
// In ClearanceService.approve():
auditService.log(actorId, AuditAction.CLEARANCE_APPROVED, "ClearanceRequest",
    clearanceRequest.getId(), "Approved clearance for resident " + residentId);
```

**Placement rules:**

- Log **after** the operation succeeds (not before) — to avoid recording actions that fail
- For `REQUIRES_NEW` audit transactions, log even if the outer transaction hasn't committed yet (acceptable trade-off: rare false-positive audit entry vs. missing audit entry)
- For failed login attempts, log with `userId = null` and `details` containing the attempted email

#### 6. ClearanceStatusChangedEvent Listener

Register an `@EventListener` for `ClearanceStatusChangedEvent` that delegates to `AuditService`:

```java
@Component
@RequiredArgsConstructor
public class ClearanceAuditListener {
    private final AuditService auditService;

    @EventListener
    public void onStatusChanged(ClearanceStatusChangedEvent event) {
        String action = "CLEARANCE_" + event.getTo().name();
        auditService.log(event.getActorId(), action, "ClearanceRequest",
            event.getClearanceRequestId(),
            "Status changed: " + event.getFrom() + " → " + event.getTo());
    }
}
```

**Note:** This may create duplicate audit entries if services also call `auditService.log()` directly. Choose one approach per transition:

- **Option A (recommended):** Use the event listener for all clearance transitions; remove direct `auditService.log()` calls from `ClearanceService` for status changes.
- **Option B:** Keep direct calls and don't register the listener. Simpler but misses future event publishers.

Recommended: **Option A** — single source of truth via the event.

#### 7. Audit Log Query Endpoints

**`AuditLogController.java`** (`/api/v1/audit-logs`, ADMIN only):

| Endpoint                                    | Method | Description                   |
| ------------------------------------------- | ------ | ----------------------------- |
| `GET /api/v1/audit-logs`                    | GET    | Paginated list with filters   |
| `GET /api/v1/audit-logs/entity/{type}/{id}` | GET    | History for a specific entity |

**Query parameters for the list endpoint:**

- `action` — filter by action (exact match)
- `entityType` — filter by entity type
- `userId` — filter by actor
- `from` / `to` — date range filter on `createdAt`
- `page`, `size`, `sort` — pagination (default: `sort=createdAt,desc`)

Use `SpecificationBuilder<AuditLog>` for dynamic query building (already available in `shared/util/`).

#### 8. AuditLogDTO

**`AuditLogDTO.java`**:

- All fields from entity
- `actorEmail` (String) — enriched from `User` table (optional; can be null if user deleted)
- Use a simple `@Query` join or post-fetch enrichment (avoid N+1)

### Frontend

#### 9. Audit Log Viewer Page

**Location:** `/backoffice/admin/audit-logs`

**Features:**

- Paginated table with columns: Timestamp, Actor (email), Action, Entity Type, Entity ID, IP Address
- Filter bar: action dropdown, entity type dropdown, date range picker, actor search
- Click row to expand `details` text
- Sidebar link under Admin section (ADMIN role only)

**Components:**

- `AuditLogTable.tsx` — paginated table with expandable rows
- `useAuditLogs.ts` — React Query hook for fetching audit logs

**Types:**

- `AuditLog` type in `types/audit.ts`

---

## Flyway Migration

**No new migration needed.** The `audit_logs` table already exists in `V1__initial_schema.sql` with the correct schema. If `actorEmail` enrichment requires a view or index:

**Optional — `V9__audit_logs_indexes.sql`:**

```sql
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
```

These indexes support the query patterns from the audit log viewer. Add only if query performance is a concern.

---

## Implementation Order

| Step | Task                                               | Files                                                     |
| ---- | -------------------------------------------------- | --------------------------------------------------------- |
| 1    | Create `AuditLog` entity                           | `shared/audit/AuditLog.java`                              |
| 2    | Create `AuditLogRepository`                        | `shared/audit/AuditLogRepository.java`                    |
| 3    | Create `AuditAction` constants                     | `shared/audit/AuditAction.java`                           |
| 4    | Create `AuditService` (with IP resolution + async) | `shared/audit/AuditService.java`                          |
| 5    | Configure async thread pool for audit              | `shared/audit/AuditAsyncConfig.java` or `application.yml` |
| 6    | Create `ClearanceAuditListener`                    | `clearance/service/ClearanceAuditListener.java`           |
| 7    | Instrument `AuthService`                           | `identity/service/AuthService.java`                       |
| 8    | Instrument `UserService`                           | `identity/service/UserService.java`                       |
| 9    | Instrument `ResidentService`                       | `residents/service/ResidentService.java`                  |
| 10   | Instrument `PaymentService`                        | `payments/service/PaymentService.java`                    |
| 11   | Instrument `SettingsService`                       | `settings/service/SettingsService.java`                   |
| 12   | Create `AuditLogDTO`                               | `shared/audit/AuditLogDTO.java`                           |
| 13   | Create `AuditLogController`                        | `shared/audit/AuditLogController.java`                    |
| 14   | Add audit log indexes (optional migration)         | `db/migration/V9__audit_logs_indexes.sql`                 |
| 15   | Frontend: `types/audit.ts`                         | `frontend/src/types/audit.ts`                             |
| 16   | Frontend: `useAuditLogs.ts` hook                   | `frontend/src/hooks/useAuditLogs.ts`                      |
| 17   | Frontend: Audit log viewer page                    | `frontend/src/app/backoffice/admin/audit-logs/page.tsx`   |
| 18   | Frontend: `AuditLogTable.tsx`                      | `frontend/src/components/backoffice/AuditLogTable.tsx`    |
| 19   | Update Sidebar with Audit Logs link                | `frontend/src/components/backoffice/Sidebar.tsx`          |

---

## Key Implementation Notes

1. **`REQUIRES_NEW` propagation** — Audit writes must commit independently. If the parent service transaction rolls back (e.g., state machine guard rejects), the audit entry (e.g., `USER_LOGIN_FAILED`) should still persist.

2. **`@Async` with fallback** — Audit logging should not add latency to user-facing requests. Use Spring's `@Async` with a bounded thread pool. If the pool is full, fall back to synchronous execution rather than dropping the audit event.

3. **No circular dependencies** — `AuditService` lives in `shared/audit/`. It has no dependencies on module-specific services. Module services depend on `AuditService`, not the reverse.

4. **Immutable records** — `AuditLog` entities should have no update methods. The repository should not expose `save()` for updates — only `save()` for inserts. Consider extending `CrudRepository` instead of `JpaRepository` to limit available operations, or simply enforce by convention.

5. **Details field** — Use structured JSON strings for the `details` field where useful (e.g., `{"from": "FOR_APPROVAL", "to": "APPROVED", "residentId": "..."}`). This enables future parsing for advanced audit analytics.

6. **Failed login handling** — `USER_LOGIN_FAILED` should record `userId = null` (user may not exist) and `details = "Attempted email: user@example.com"`. Do NOT log the attempted password.

7. **PDF download logging** — Track who downloads clearance PDFs for accountability. Log at the controller level since the service is purely generative.

---

## Definition of Done

- [ ] `AuditLog` entity maps to existing `audit_logs` table
- [ ] `AuditLogRepository` with paginated queries + specification support
- [ ] `AuditService` logs actions with IP address resolution
- [ ] `AuditService` uses `@Async` with bounded thread pool
- [ ] `AuditService` uses `REQUIRES_NEW` transaction propagation
- [ ] `AuditAction` constants cover all 27 action types listed above
- [ ] `ClearanceAuditListener` listens to `ClearanceStatusChangedEvent`
- [ ] `AuthService` instrumented: register, login, login failed, logout, refresh, change password
- [ ] `UserService` instrumented: create staff, activate, deactivate, role change, password reset
- [ ] `ResidentService` instrumented: create, update, activate
- [ ] `PaymentService` instrumented: initiate, success, failed, cash recorded
- [ ] `SettingsService` instrumented: update settings, upload logo, update fees
- [ ] `AuditLogController` — `GET /audit-logs` (paginated, filtered), `GET /audit-logs/entity/{type}/{id}`
- [ ] Controller secured with `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Frontend: Audit log viewer page at `/backoffice/admin/audit-logs`
- [ ] Frontend: Filterable, paginated table with expandable detail rows
- [ ] Frontend: Sidebar link (ADMIN only)
- [ ] `./mvnw clean compile` succeeds
- [ ] `./mvnw test` passes (no regressions)
- [ ] Manual test: perform key operations → verify audit records appear in viewer
