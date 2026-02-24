# Phase 9 — Testing & QA

**Status:** Not Started
**Estimated Timeline:** Week 7
**Priority:** High

---

## Goal

Verify correctness of critical business logic with automated unit and integration tests, and complete a manual end-to-end QA pass of the full clearance workflow.

---

## Dependencies

**Depends on:** All feature phases (0–8) — testing requires all modules to be implemented.

**Can run in parallel with:** Phase 10 (Deployment) can begin infrastructure setup while testing is underway, but the full QA pass should complete before deployment.

---

## Deliverables

### Unit Tests (JUnit 5 + Mockito)

| Test Class | Key Scenarios |
|---|---|
| `JwtServiceTest` | Token generation, validation, expiry, invalid signature → exception |
| `AuthServiceTest` | Register success, duplicate email → exception; login OK, wrong password, PENDING status; refresh, logout |
| `ClearanceServiceTest` | All state transitions with guards; illegal transitions throw `BadRequestException`; reject without reason throws; release with UNPAID throws |
| `ClearanceNumberServiceTest` | Sequential number assignment for one month; monthly reset (different `year_month` → resets to 0001); concurrent calls return unique sequential numbers |
| `PaymentServiceTest` | New payment success; idempotent replay (SUCCESS) → cached response + `idempotent: true`; PENDING → `ConflictException`; already PAID → `ConflictException`; cash mark-as-paid; duplicate mark-as-paid → existing record |
| `ClearancePdfServiceTest` | Generate returns non-null byte array; starts with `%PDF` magic bytes; no exception when logo is null |

### Integration Tests (Testcontainers PostgreSQL)

All extend `BaseIntegrationTest`:
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
```

| Test Class | Coverage |
|---|---|
| `AuthControllerIT` | Register → 201; login → 200 with tokens; refresh → 200; logout → 200; second use of same refresh → 401 |
| `ResidentControllerIT` | CRUD with CLERK token; search pagination; pending-users list; activate/reject |
| `ClearanceWorkflowIT` (happy path) | Register + activate resident → submit clearance → approve → mark paid → release → download PDF |
| `ClearanceWorkflowIT` (rejection path) | Reject with reason → resident edits + resubmits → `FOR_APPROVAL` |
| `PaymentControllerIT` | Same key → 200 + `idempotent:true`; PENDING → 409; missing header → 400 |
| `SettingsControllerIT` | Update settings → 200; upload valid logo → 204; oversized → 400; non-image → 400 |
| `SecurityGuardIT` | RESIDENT cannot hit `/clearances/{id}/approve`; CLERK cannot hit `/admin/users`; unauthenticated → 401 |

### Manual QA Checklist

**1. Full Clearance Workflow (end-to-end)**
- Register as resident → confirmation message
- Log in as CLERK → activate the resident
- Log in as resident → submit new clearance (Employment, Regular, 1 copy)
- Log in as APPROVER → approve the request
- Log in as resident → click "Pay Now" → payment success toast
- Log in as CLERK → click "Release" → clearance number assigned (e.g., `2026-02-0001`)
- Log in as resident → click "Download PDF" → PDF downloads and opens correctly

**2. Rejection + Resubmit Flow**
- Submit request as resident
- Log in as APPROVER → reject with reason "Incomplete documents"
- Log in as resident → see rejection reason → click "Edit & Resubmit" → update notes → resubmit
- Verify status returns to `FOR_APPROVAL`

**3. Duplicate Payment Idempotency**
- Approve a clearance
- Log in as resident → click "Pay Now" twice rapidly
- Verify only one payment record created

**4. Admin Settings**
- Log in as ADMIN → upload logo → update captain name
- Release a new clearance → verify logo and captain name in downloaded PDF

**5. Role Guard Verification**
- With resident JWT, navigate directly to `/backoffice/clearances` → redirected
- With resident JWT, call `POST /api/v1/clearances/{id}/approve` directly → 403

---

## Key Implementation Notes

### `ClearanceNumberServiceTest` — Concurrency Test
```java
@Test
void concurrentRelease_assignsUniqueSequentialNumbers() throws InterruptedException {
    int threads = 10;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    Set<String> numbers = ConcurrentHashMap.newKeySet();
    CountDownLatch latch = new CountDownLatch(threads);
    for (int i = 0; i < threads; i++) {
        pool.submit(() -> {
            numbers.add(clearanceNumberService.generateForMonth("2025-02"));
            latch.countDown();
        });
    }
    latch.await();
    assertThat(numbers).hasSize(threads); // all unique
}
```

### Test Profile
`application-test.yml`: `payment.stub.always-success: true`. Testcontainers `@ServiceConnection` overrides datasource URL automatically — no URL configuration needed.

### `ClearancePdfServiceTest`
```java
byte[] pdf = pdfService.generate(clearanceRequest, resident, settings);
assertThat(pdf).isNotNull();
assertThat(new String(Arrays.copyOf(pdf, 4))).isEqualTo("%PDF");
```

---

## Definition of Done

- [ ] `./mvnw test` passes with 0 failures
- [ ] All unit test classes implemented with scenarios listed above
- [ ] All integration test classes implemented
- [ ] All 5 manual QA checklists pass end-to-end on Docker Compose stack
- [ ] No `500 Internal Server Error` in any tested scenario
- [ ] (Optional) JaCoCo coverage on service classes ≥ 70%
