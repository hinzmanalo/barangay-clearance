# Backend Performance Improvements

**Created:** 2026-02-27
**Scope:** Backend code review — Spring Boot 3.3.4, PostgreSQL, JPA/Hibernate
**Status:** Recommendations only — no code changes yet

---

## Summary

A full scan of the backend codebase identified **19 performance issues** across 4 severity levels. The most impactful are N+1 query patterns in the clearance and resident services, missing database indexes on frequently queried columns, and the complete absence of caching for singleton configuration rows.

| Severity | Count | Estimated Effort |
|----------|-------|------------------|
| Critical | 5     | 1–2 days         |
| High     | 5     | 1–2 days         |
| Medium   | 6     | 2–3 days         |
| Low      | 3     | 1 day            |

---

## Critical Issues

### 1. N+1 Query — `ClearanceService.enrich()`

**File:** [ClearanceService.java:493-523](backend/src/main/java/com/barangay/clearance/clearance/service/ClearanceService.java#L493-L523)

**Problem:** Every call to `enrich()` executes a separate `SELECT` on the `residents` table to resolve the resident name. The `list()` and `listForResident()` methods call `enrich()` per row via `.map(this::enrich)`. A page of 20 clearances produces **21 queries** (1 page query + 20 resident lookups).

```java
// Line 497 — fires once PER clearance request in the page
String residentName = residentRepository.findById(entity.getResidentId())
        .map(r -> r.getLastName() + ", " + r.getFirstName())
        .orElse("Unknown Resident");
```

**Impact:** Every backoffice list page load, every portal "My Requests" page load, and every single-record GET triggers this.

**Fix options:**
- **Option A (recommended):** Add a JPQL projection query to `ClearanceRequestRepository` that joins `clearance_requests` with `residents` in a single query, returning the name inline. Similar to what `ReportRepository` already does correctly.
- **Option B:** Batch-fetch resident names using `residentRepository.findAllById(residentIds)` before mapping, then populate from a `Map<UUID, String>`.
- **Option C:** Denormalize — store `resident_full_name` on `clearance_requests`. Trade-off: must update on resident name change.

---

### 2. N+1 Query + Unbounded Fetch — `ResidentService.findPendingUsers()`

**File:** [ResidentService.java:150-162](backend/src/main/java/com/barangay/clearance/residents/service/ResidentService.java#L150-L162)

**Problem:** Two issues combined:
1. `Pageable.unpaged()` loads **all** pending users from the `users` table with no limit.
2. For each user, a separate `residentRepository.findByUserId()` query is executed (N+1).

```java
List<User> pendingUsers = userRepository.findByStatus(
        User.UserStatus.PENDING_VERIFICATION,
        Pageable.unpaged()).getContent();  // Unbounded!

return pendingUsers.stream()
        .filter(u -> u.getRole() == User.Role.RESIDENT)
        .map(u -> residentRepository.findByUserId(u.getId())  // N+1
                .map(residentMapper::toDTO)
                .orElse(null))
```

**Fix:** Replace with a single native or JPQL query joining `users` and `residents`:
```sql
SELECT r.* FROM residents r
JOIN users u ON r.user_id = u.id
WHERE u.status = 'PENDING_VERIFICATION' AND u.role = 'RESIDENT'
```
Add pagination support.

---

### 3. Missing Index — `clearance_requests.resident_id`

**File:** [V1__initial_schema.sql:92-112](backend/src/main/resources/db/migration/V1__initial_schema.sql#L92-L112)

**Problem:** `ClearanceRequestRepository.findByResidentId(UUID, Pageable)` is the primary query for every portal user viewing "My Requests". Without an index on `resident_id`, PostgreSQL performs a **sequential scan** of the entire `clearance_requests` table.

**Fix (new Flyway migration):**
```sql
CREATE INDEX idx_cr_resident_id ON clearance_requests (resident_id);
```

---

### 4. Missing Index — `clearance_requests.created_at`

**File:** [V1__initial_schema.sql:92-112](backend/src/main/resources/db/migration/V1__initial_schema.sql#L92-L112)

**Problem:** The default sort for all paginated clearance list endpoints is `ORDER BY created_at DESC`. Without an index, every list query requires a full table scan + in-memory sort.

**Fix (new Flyway migration):**
```sql
CREATE INDEX idx_cr_created_at ON clearance_requests (created_at DESC);
```

---

### 5. Missing Index — `payments.clearance_request_id`

**File:** [V1__initial_schema.sql:117-131](backend/src/main/resources/db/migration/V1__initial_schema.sql#L117-L131)

**Problem:** `PaymentRepository.findByClearanceRequestIdOrderByCreatedAtDesc()` scans the full `payments` table. This is called on every payment history lookup.

**Fix (new Flyway migration):**
```sql
CREATE INDEX idx_payments_clearance_id ON payments (clearance_request_id);
```

---

## High Priority

### 6. No Caching for Singleton `fee_config`

**File:** [ClearanceService.java:532-539](backend/src/main/java/com/barangay/clearance/clearance/service/ClearanceService.java#L532-L539)

**Problem:** `feeConfigRepository.findById(1)` is called on every `submitPortal()`, `createWalkIn()`, and `resubmit()` — a DB round-trip for a row that almost never changes.

**Fix:**
1. Add `spring-boot-starter-cache` + Caffeine dependency to `pom.xml`
2. Add `@EnableCaching` to the application class
3. Create a `SettingsService.getFeeConfig()` method annotated with `@Cacheable("fee-config")`
4. Evict on update: `@CacheEvict(value = "fee-config", allEntries = true)` on the fee update method
5. Replace direct `feeConfigRepository.findById(1)` in `ClearanceService` with `settingsService.getFeeConfig()`

---

### 7. Logo BYTEA Loaded on Every PDF Request

**Files:**
- [ClearanceController.java](backend/src/main/java/com/barangay/clearance/clearance/controller/ClearanceController.java) — direct `settingsRepository.findById(1)`
- [PortalClearanceController.java](backend/src/main/java/com/barangay/clearance/clearance/controller/PortalClearanceController.java) — same pattern

**Problem:** The `BarangaySettings` entity includes a `logo` field (`byte[]`, BYTEA, up to 2MB). Every PDF download loads the entire row including the logo binary, even when the logo hasn't changed. Both controllers also bypass `SettingsService`, accessing the repository directly (layering violation).

**Fix:**
1. Remove `BarangaySettingsRepository` from both controllers; delegate to `SettingsService`
2. Cache the `BarangaySettings` entity via `@Cacheable("barangay-settings")` in `SettingsService`
3. Evict on settings update and logo upload

---

### 8. No HikariCP Connection Pool Tuning

**File:** [application.yml](backend/src/main/resources/application.yml)

**Problem:** No `spring.datasource.hikari.*` properties are configured anywhere. Spring Boot defaults to HikariCP with **10 max connections**. Under concurrent load, threads will block waiting for connections before any query optimization matters.

**Fix — add to `application.yml`:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

### 9. Double Clearance Fetch in Portal Payment

**File:** [PaymentController.java](backend/src/main/java/com/barangay/clearance/payments/controller/PaymentController.java)

**Problem:** `payPortal()` calls `clearanceService.getForResident(id, principal.getUserId())` (fetches clearance + resident), then `paymentService.initiate(id, ...)` fetches the **same clearance again** via `clearanceRepo.findById(clearanceId)`. This is a redundant DB round-trip on every portal payment attempt.

**Fix:** Pass the resolved clearance entity or ID from `getForResident` into `initiate()`, or merge the ownership check into the payment flow.

---

### 10. Missing Composite Index — `(status, payment_status)`

**File:** [V1__initial_schema.sql:111](backend/src/main/resources/db/migration/V1__initial_schema.sql#L111)

**Problem:** The dashboard `summary()` endpoint calls `countByStatusAndPaymentStatus(APPROVED, UNPAID)`. The existing `idx_cr_status` only covers `status`. A composite index would satisfy both the single-status and combined queries.

**Fix (new Flyway migration):**
```sql
-- Replace the single-column index with a composite one
DROP INDEX IF EXISTS idx_cr_status;
CREATE INDEX idx_cr_status_payment ON clearance_requests (status, payment_status);
```

---

## Medium Priority

### 11. Resident Name Search Defeats B-tree Index

**File:** [ResidentRepository.java](backend/src/main/java/com/barangay/clearance/residents/repository/ResidentRepository.java)

**Problem:** The search query uses `LIKE '%term%'` (leading wildcard), which cannot use the B-tree index `idx_residents_name`. The existing code comment claims the index is exploited — this is incorrect.

**Fix options:**
- For the current scale (small barangay), this is acceptable
- For scale: enable `pg_trgm` extension and create a GIN trigram index
- Correct the misleading comment in the repository

---

### 12. Synchronous PDF Generation Blocks Tomcat Threads

**File:** [ClearancePdfServiceImpl.java](backend/src/main/java/com/barangay/clearance/pdf/service/ClearancePdfServiceImpl.java)

**Problem:** PDFBox operations (byte array manipulation, image embedding, font rendering) execute synchronously on the Tomcat request thread. High concurrency on PDF endpoints could exhaust the thread pool.

**Fix:** For future scale, offload to `@Async` with a dedicated `ThreadPoolTaskExecutor`, or stream the PDF to the response as it generates.

---

### 13. Missing Indexes on `refresh_tokens`

**File:** [V1__initial_schema.sql:27-34](backend/src/main/resources/db/migration/V1__initial_schema.sql#L27-L34)

**Problem:** `deleteByUserId()` and `deleteByExpiresAtBefore()` both scan the full `refresh_tokens` table.

**Fix (new Flyway migration):**
```sql
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
```

---

### 14. No Scheduled Cleanup of Expired Refresh Tokens

**Problem:** `RefreshTokenRepository.deleteByExpiresAtBefore(Instant)` exists but is **never called** anywhere in the codebase. The `refresh_tokens` table grows indefinitely.

**Fix:** Add a `@Scheduled` component:
```java
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")  // Daily at 3 AM
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
```

---

### 15. Direct Repository Access in Controllers

**Files:**
- [ClearanceController.java](backend/src/main/java/com/barangay/clearance/clearance/controller/ClearanceController.java) — injects `BarangaySettingsRepository`
- [PortalClearanceController.java](backend/src/main/java/com/barangay/clearance/clearance/controller/PortalClearanceController.java) — same

**Problem:** Both PDF download endpoints bypass `SettingsService` and access the repository directly. This prevents centralized caching, validation, or future business logic from applying.

**Fix:** Remove `BarangaySettingsRepository` from both controllers; delegate to `SettingsService.getSettingsEntity()`.

---

### 16. Staff Search Uses Three Wildcard LIKEs with OR

**File:** [UserService.java](backend/src/main/java/com/barangay/clearance/identity/service/UserService.java)

**Problem:** `listStaff` builds a JPA Specification with three `LIKE '%term%'` predicates ORed together (firstName, lastName, email). No index can satisfy this.

**Impact:** Acceptable for the expected staff count (< 50 users). Document the limitation and consider trigram indexes if user count grows significantly.

---

## Low Priority

### 17. Dashboard Summary Makes 4 Separate COUNT Queries

**File:** [ClearanceService.java:378-396](backend/src/main/java/com/barangay/clearance/clearance/service/ClearanceService.java#L378-L396)

**Problem:** `summary()` makes 4 round-trips:
```java
long pendingApproval = clearanceRepo.countByStatus(FOR_APPROVAL);
long approved = clearanceRepo.countByStatus(APPROVED);
long awaitingPayment = clearanceRepo.countByStatusAndPaymentStatus(APPROVED, UNPAID);
long releasedToday = clearanceRepo.countReleasedToday(startOfDay, endOfDay);
```

**Fix:** Consolidate into 1–2 native queries using `GROUP BY status, payment_status` plus a separate range count for `releasedToday`.

---

### 18. No CORS Origin Configuration via Properties

**File:** [SecurityConfig.java](backend/src/main/java/com/barangay/clearance/shared/security/SecurityConfig.java)

**Problem:** CORS origins are hardcoded to `localhost:3000`. Production deployment will require a code change.

**Fix:** Externalize to `application.yml`:
```yaml
app:
  cors:
    allowed-origins: http://localhost:3000
```

---

### 19. No Rate Limiting on Auth Endpoints

**File:** [SecurityConfig.java](backend/src/main/java/com/barangay/clearance/shared/security/SecurityConfig.java)

**Problem:** `/api/v1/auth/login` and `/api/v1/auth/register` have no rate limiting. Brute-force attacks are unmitigated.

**Fix options:**
- Add `bucket4j-spring-boot-starter` for token-bucket rate limiting
- Or use Nginx `limit_req_zone` in the reverse proxy (simpler, no code change)

---

## Recommended Implementation Order

| Step | Issues | Effort | Impact |
|------|--------|--------|--------|
| 1    | #3, #4, #5, #10, #13 — New Flyway migration with all missing indexes | 1 hour | High — immediate query speedup |
| 2    | #1 — Fix N+1 in `ClearanceService.enrich()` | 2–3 hours | Critical — reduces queries by ~95% on list endpoints |
| 3    | #2 — Fix N+1 in `ResidentService.findPendingUsers()` | 1 hour | Critical — eliminates unbounded fetch |
| 4    | #6, #7, #15 — Add Spring Cache (Caffeine) for settings/fees + fix layering | 2–3 hours | High — eliminates repeated singleton reads |
| 5    | #8 — HikariCP tuning | 15 min | High — prevents connection starvation |
| 6    | #9 — Remove double clearance fetch | 30 min | Medium — saves 1 query per payment |
| 7    | #14 — Add scheduled token cleanup | 30 min | Medium — prevents table bloat |
| 8    | #17 — Consolidate dashboard COUNT queries | 1 hour | Low — saves 2–3 queries per dashboard load |
| 9    | #18, #19 — CORS externalization + rate limiting | 1–2 hours | Low — operational improvement |

**Total estimated effort:** ~2–3 days for all issues.

---

## Verification Plan

After implementing fixes:

1. **Index verification:** Run `EXPLAIN ANALYZE` on key queries to confirm index usage
2. **N+1 verification:** Enable Hibernate SQL logging (`spring.jpa.show-sql=true`), load a clearance list page, and count the number of `SELECT` statements (should be 1–2, not N+1)
3. **Cache verification:** Load barangay settings, update them, load again — confirm cached value is returned then evicted correctly
4. **Connection pool:** Monitor HikariCP metrics via `/actuator/metrics/hikaricp.connections.active` under load
5. **Token cleanup:** Verify expired tokens are deleted after the scheduled job runs
6. **Regression:** Run `./mvnw test` — all tests must pass
