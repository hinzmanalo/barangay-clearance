# Phase 9.1 — Backend Unit & Integration Tests

## Overview

Implements all backend unit tests (6 test classes) and integration tests (6 test classes + BaseIntegrationTest infrastructure) for the Barangay Clearance System. Covers JWT security, auth flows, clearance state machine, payment idempotency, PDF generation, and role-based access control.

**PRD Reference:** [phase-09-testing-prd.md](phase-09-testing-prd.md) — Sections 4.1–4.14, 4.7, 4.22

---

## Prerequisites

- All Phases 0–8 and 11 code is complete and compiling
- `backend/pom.xml` already includes: `spring-boot-starter-test`, `spring-security-test`, `testcontainers/junit-jupiter`, `testcontainers/postgresql`
- `application-test.yml` already exists with fixed JWT secret and Flyway enabled
- Docker must be available locally (for Testcontainers integration tests)

---

## Step 1: Create test directory structure & update application-test.yml

**Files to create/modify:**

```
backend/src/test/java/com/barangay/clearance/
├── identity/service/
│   ├── JwtServiceTest.java
│   └── AuthServiceTest.java
├── clearance/service/
│   ├── ClearanceServiceTest.java
│   └── ClearanceNumberServiceTest.java
├── payments/service/
│   └── PaymentServiceTest.java
├── pdf/service/
│   └── ClearancePdfServiceTest.java
└── integration/
    ├── BaseIntegrationTest.java
    ├── AuthControllerIT.java
    ├── ResidentControllerIT.java
    ├── ClearanceWorkflowIT.java
    ├── PaymentControllerIT.java
    ├── SettingsControllerIT.java
    └── SecurityGuardIT.java
```

**Modify** `backend/src/main/resources/application-test.yml`:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    clean-on-validation-error: false
  jpa:
    show-sql: false

app:
  jwt:
    secret: test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm
    access-token-expiry-ms: 3600000   # 1 hour (prevent mid-test expiry)
    refresh-token-expiry-ms: 604800000  # 7 days

payment:
  stub:
    always-success: true
```

> **Note:** The current `application-test.yml` has `access-token-expiry-ms: 900000` (15 min). Increase to 1 hour per PRD requirement 4.22 to prevent mid-test token expiry.

---

## Step 2: Backend Unit Tests — JwtServiceTest

**File:** `backend/src/test/java/com/barangay/clearance/identity/service/JwtServiceTest.java`

**Target class:** [JwtService.java](../../backend/src/main/java/com/barangay/clearance/identity/service/JwtService.java)

**Test setup:**
- No Spring context (`@ExtendWith(MockitoExtension.class)` not needed — instantiate `JwtService` directly with test secret + expiry values)
- Use the same fixed JWT secret from `application-test.yml`

**Test methods:**

| # | Method | Description | Key assertion |
|---|--------|-------------|---------------|
| 1 | `generateAccessToken_returnsNonBlankParsableToken` | Generate token for a valid user | Token is non-blank; `extractUserId()` returns same UUID |
| 2 | `extractUserId_returnsCorrectSubject` | Extract userId from generated token | `UUID.equals()` |
| 3 | `extractRole_returnsCorrectClaim` | Extract role from generated token | `Role.RESIDENT.equals()` |
| 4 | `extractEmail_returnsCorrectClaim` | Extract email from generated token | `"test@example.com".equals()` |
| 5 | `expiredToken_throwsJwtException` | Instantiate JwtService with `accessTokenExpiryMs = -1000` (past), generate token, attempt extract | `assertThrows(JwtException/ExpiredJwtException)` |
| 6 | `tamperedToken_throwsJwtException` | Generate valid token, flip one character in payload segment | `assertThrows(JwtException)` |
| 7 | `generateRawRefreshToken_returnsUuidFormat` | Call `generateRawRefreshToken()` | Matches UUID pattern; differs from access token |
| 8 | `hashRefreshToken_returnsDeterministicSha256` | Hash same token twice | Both hashes are identical; hash is 64 hex chars |

**Implementation notes:**
- `JwtService` constructor takes `(String secret, long accessExpiryMs, long refreshExpiryMs)` — instantiate directly in tests
- For expiry test: create a separate `JwtService` instance with negative expiry, or use reflection/a builder to set a past date
- Actual approach: The `getClaims()` method inside `JwtService` is private and uses `Jwts.parser().verifyWith(signingKey)` which validates expiry automatically. Generate a token with a JwtService that has `accessTokenExpiryMs = 1` (1ms), then `Thread.sleep(5)` before parsing to ensure it's expired.

---

## Step 3: Backend Unit Tests — AuthServiceTest

**File:** `backend/src/test/java/com/barangay/clearance/identity/service/AuthServiceTest.java`

**Target class:** [AuthService.java](../../backend/src/main/java/com/barangay/clearance/identity/service/AuthService.java)

**Test setup:**
- `@ExtendWith(MockitoExtension.class)`
- `@Mock` UserRepository, RefreshTokenRepository, JwtService, PasswordEncoder, ResidentService
- `@InjectMocks` AuthService

**Test methods:**

| # | Method | Description | Key assertion |
|---|--------|-------------|---------------|
| 1 | `register_success_returnsAuthResponseWithTokens` | Mock `userRepo.existsByEmail → false`, `passwordEncoder.encode → hash`, `userRepo.save → user`, `jwtService.generateAccessToken → token` | `AuthResponse` has non-null `accessToken` and `refreshToken` |
| 2 | `register_duplicateEmail_throws409` | Mock `userRepo.existsByEmail → true` | `AppException` with HTTP 409 |
| 3 | `login_success_returnsTokens` | Mock `userRepo.findByEmail → ACTIVE user`, `passwordEncoder.matches → true` | `AuthResponse` with non-null tokens |
| 4 | `login_wrongPassword_throws401` | Mock `passwordEncoder.matches → false` | `AppException` with HTTP 401 |
| 5 | `login_pendingStatus_throws403` | Mock user with `PENDING_VERIFICATION` status | `AppException` with HTTP 403 |
| 6 | `refresh_validToken_returnsNewTokens` | Mock `refreshTokenRepo.findByTokenHash → valid token` | New `AuthResponse` with rotated tokens |
| 7 | `refresh_expiredOrRevoked_throws401` | Mock `refreshTokenRepo.findByTokenHash → empty` | `AppException` with HTTP 401 |
| 8 | `logout_revokesRefreshToken` | Call `logout()` then verify `refreshTokenRepo.deleteByUserId` was called | `verify(refreshTokenRepo).deleteByUserId(userId)` |

**Implementation notes:**
- Read `AuthService.java` to confirm exact method signatures (register takes `RegisterRequest`, login takes `LoginRequest`, etc.)
- The `register()` method also calls `residentService.createFromRegistration()` — mock this too
- User entity has `UserStatus` enum: `ACTIVE`, `INACTIVE`, `PENDING_VERIFICATION`, `DEACTIVATED`, `REJECTED`

---

## Step 4: Backend Unit Tests — ClearanceServiceTest

**File:** `backend/src/test/java/com/barangay/clearance/clearance/service/ClearanceServiceTest.java`

**Target class:** [ClearanceService.java](../../backend/src/main/java/com/barangay/clearance/clearance/service/ClearanceService.java)

**Test setup:**
- `@ExtendWith(MockitoExtension.class)`
- `@Mock` ClearanceRequestRepository, ResidentRepository, ClearanceMapper, ClearanceNumberService, ApplicationEventPublisher, FeeConfigRepository
- `@InjectMocks` ClearanceService

**IMPORTANT: State machine deviations from PRD**

The PRD assumes a DRAFT state and `submit()` method. The actual implementation **skips DRAFT** — new requests go directly to `FOR_APPROVAL` via `submitPortal()`. There is no `submit()` method. Adjust tests accordingly:

| PRD Transition | Actual Code |
|----------------|-------------|
| DRAFT → FOR_APPROVAL via `submit()` | `submitPortal()` creates at FOR_APPROVAL directly |
| REJECTED → DRAFT via `resubmit()` | `resubmit()` sets status to FOR_APPROVAL (not DRAFT) |

**Valid transition tests:**

| # | Method | Description |
|---|--------|-------------|
| 1 | `submitPortal_createsRequestInForApprovalStatus` | Mock resident lookup, verify saved entity has `FOR_APPROVAL` |
| 2 | `approve_fromForApproval_setsApproved` | Mock clearance with `FOR_APPROVAL`, call `approve()` → `APPROVED` |
| 3 | `reject_fromForApproval_setsRejected` | Mock clearance with `FOR_APPROVAL`, call `reject()` with reason → `REJECTED` |
| 4 | `release_fromApprovedAndPaid_setsReleased` | Mock clearance with `APPROVED` + `PAID`, call `release()` → `RELEASED` |
| 5 | `resubmit_fromRejected_setsForApproval` | Mock clearance with `REJECTED`, call `resubmit()` → `FOR_APPROVAL` |

**Illegal transition guard tests (all throw AppException 400):**

| # | Method | Description |
|---|--------|-------------|
| 6 | `approve_onApproved_throws400` | `approve()` on APPROVED clearance |
| 7 | `approve_onReleased_throws400` | `approve()` on RELEASED clearance |
| 8 | `approve_onRejected_throws400` | `approve()` on REJECTED clearance |
| 9 | `reject_onApproved_throws400` | `reject()` on APPROVED clearance |
| 10 | `reject_onReleased_throws400` | `reject()` on RELEASED clearance |
| 11 | `release_onForApproval_throws400` | `release()` on FOR_APPROVAL clearance |
| 12 | `release_onRejected_throws400` | `release()` on REJECTED clearance |
| 13 | `resubmit_onForApproval_throws400` | `resubmit()` on FOR_APPROVAL clearance |
| 14 | `resubmit_onApproved_throws400` | `resubmit()` on APPROVED clearance |

**Validation guard tests:**

| # | Method | Description |
|---|--------|-------------|
| 15 | `reject_withBlankReason_throws400` | `reject()` with blank reason string |
| 16 | `reject_withNullReason_throws400` | `reject()` with null reason |
| 17 | `release_whenUnpaid_throws400` | `release()` on APPROVED but UNPAID clearance |
| 18 | `submitPortal_noResidentProfile_throws404` | `submitPortal()` for user with no linked resident |
| 19 | `getForResident_wrongOwner_throws404` | `getForResident()` with non-matching resident → not found |

**Implementation notes:**
- `ClearanceService` uses `enrich()` which calls `mapper.toDTO()` and `residentRepository.findById()` — mock both
- `release()` calls `numberService.next()` — mock to return `"2025-030001"`
- `ClearanceMapper` is MapStruct-generated; mock `toDTO()` to return a pre-built DTO

---

## Step 5: Backend Unit Tests — ClearanceNumberServiceTest

**File:** `backend/src/test/java/com/barangay/clearance/clearance/service/ClearanceNumberServiceTest.java`

**Target class:** [ClearanceNumberService.java](../../backend/src/main/java/com/barangay/clearance/clearance/service/ClearanceNumberService.java)

**Test setup:**
- This service uses `EntityManager` with native SQL (`INSERT ... ON CONFLICT DO UPDATE RETURNING`) and `@Transactional(propagation = REQUIRES_NEW)`
- **Cannot be unit-tested with mocks** — requires integration test with real PostgreSQL
- Move these tests to integration layer (run within `BaseIntegrationTest`)

**Test methods (integration):**

| # | Method | Description | Key assertion |
|---|--------|-------------|---------------|
| 1 | `next_returnsFormattedNumber` | Call `next()` | Matches pattern `^\d{4}-\d{2}\d{4}$` |
| 2 | `next_calledTwice_returnsSequential` | Call `next()` twice | Second number > first number |
| 3 | `next_concurrent_allUnique` | 10 parallel calls via `CompletableFuture` | All 10 results in a `Set` have size 10 |

---

## Step 6: Backend Unit Tests — PaymentServiceTest

**File:** `backend/src/test/java/com/barangay/clearance/payments/service/PaymentServiceTest.java`

**Target class:** [PaymentService.java](../../backend/src/main/java/com/barangay/clearance/payments/service/PaymentService.java)

**Test setup:**
- `@ExtendWith(MockitoExtension.class)`
- `@Mock` PaymentRepository, ClearanceRequestRepository, ClearanceService, PaymentGateway, PaymentMapper, ObjectMapper
- `@InjectMocks` PaymentService

**Test methods:**

| # | Method | Description | Key assertion |
|---|--------|-------------|---------------|
| 1 | `initiate_freshKey_callsGatewayAndReturnsPending` | No existing payment, gateway returns success | `paymentGateway.initiate()` called once; payment saved with SUCCESS |
| 2 | `initiate_replaySuccess_returnsCachedIdempotent` | Existing payment with SUCCESS status | `paymentGateway.initiate()` NOT called; `dto.idempotent == true` |
| 3 | `initiate_replayPending_throws409` | Existing payment with PENDING status | `AppException` with HTTP 409 |
| 4 | `initiate_clearanceNotApproved_throws400` | Clearance in FOR_APPROVAL status | `AppException` with HTTP 400 |
| 5 | `initiate_missingIdempotencyKey_throws400` | null key | `AppException` with HTTP 400 |
| 6 | `initiate_invalidIdempotencyKey_throws400` | "not-a-uuid" key | `AppException` with HTTP 400 |
| 7 | `markPaid_approvedClearance_createsSuccessPayment` | Clearance APPROVED + UNPAID | Payment saved with SUCCESS + CASH |
| 8 | `markPaid_alreadyPaid_returnsExistingIdempotent` | Clearance APPROVED + PAID, existing payment | Returns existing payment (no duplicate) |
| 9 | `markPaid_notApproved_throws400` | Clearance in FOR_APPROVAL | `AppException` with HTTP 400 |

**Implementation notes:**
- `PaymentService.initiate()` takes `(UUID clearanceId, UUID userId, String idempotencyKey)` — matches the PRD
- `markPaid()` takes `(UUID clearanceId, UUID staffUserId)` — is idempotent (returns existing on duplicate)
- The PRD expects `markAsPaid()` to throw 409 on duplicate, but actual code is idempotent. Test the actual behavior.

---

## Step 7: Backend Unit Tests — ClearancePdfServiceTest

**File:** `backend/src/test/java/com/barangay/clearance/pdf/service/ClearancePdfServiceTest.java`

**Target class:** [ClearancePdfServiceImpl.java](../../backend/src/main/java/com/barangay/clearance/pdf/service/ClearancePdfServiceImpl.java)

**Test setup:**
- `ClearancePdfServiceImpl` is the implementation class
- The `generate()` method takes `(ClearanceRequest, Resident, BarangaySettings)` — no repository injection needed
- Construct real entity objects; exercise PDFBox rendering in-memory

**Test methods:**

| # | Method | Description | Key assertion |
|---|--------|-------------|---------------|
| 1 | `generate_returnsNonEmptyByteArray` | Pass valid clearance + resident + settings | `byte[].length > 0` |
| 2 | `generate_startsWithPdfMagicBytes` | Check first 4 bytes | `%PDF` (0x25, 0x50, 0x44, 0x46) |
| 3 | `generate_nullLogo_doesNotThrow` | Settings with `logoBytes = null` | No exception; valid PDF |
| 4 | `generate_withLogo_doesNotThrow` | Settings with a real small PNG byte array | No exception; valid PDF |

**Implementation notes:**
- The PRD expects `generatePdf(clearanceId)` that throws 404 for unknown IDs. The actual implementation takes pre-resolved entities (`ClearanceRequest, Resident, BarangaySettings`) — no repository calls inside `generate()`. The 404 is thrown at the controller/orchestration level. Adjust tests accordingly.
- Create minimal but complete entity fixtures (Resident with name/address, ClearanceRequest with RELEASED status and clearance number, BarangaySettings with barangay name and captain name).

---

## Step 8: Integration Test Infrastructure — BaseIntegrationTest

**File:** `backend/src/test/java/com/barangay/clearance/integration/BaseIntegrationTest.java`

**Implementation:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JwtService jwtService;
    @Autowired protected UserRepository userRepository;

    // Helper token generators
    protected String asAdmin() { ... }
    protected String asClerk() { ... }
    protected String asApprover() { ... }
    protected String asResident(UUID userId) { ... }

    // MockMvc wrappers
    protected ResultActions performGet(String url, String token) { ... }
    protected ResultActions performPost(String url, Object body, String token) { ... }
    protected ResultActions performPatch(String url, Object body, String token) { ... }
    protected ResultActions performPut(String url, Object body, String token) { ... }
    protected ResultActions performMultipart(String url, MockMultipartFile file, String token) { ... }
}
```

**Key design decisions:**
- Singleton container pattern: `static @Container` + `@ServiceConnection` — one PostgreSQL per JVM
- `@BeforeEach` in each IT subclass truncates tables in dependency order (payments → clearance_requests → residents → users, preserving settings/fee_config)
- Helper `asXxx()` methods: seed a user in `@BeforeAll` or on-demand, generate a real JWT via `jwtService.generateAccessToken()`
- No `@DynamicPropertySource` needed thanks to `@ServiceConnection`

---

## Step 9: Integration Tests — AuthControllerIT

**File:** `backend/src/test/java/com/barangay/clearance/integration/AuthControllerIT.java`

**PRD Reference:** Section 4.8

| # | Test | HTTP | Expected |
|---|------|------|----------|
| 1 | Register with valid payload | `POST /api/v1/auth/register` | 201; body has `accessToken`, `refreshToken` |
| 2 | Login with seeded ACTIVE user | `POST /api/v1/auth/login` | 200; body has tokens |
| 3 | Refresh with valid token | `POST /api/v1/auth/refresh` | 200; new `accessToken` |
| 4 | Logout then refresh fails | `POST /api/v1/auth/logout` then `POST /api/v1/auth/refresh` | 200 then 401 |
| 5 | Register duplicate email | `POST /api/v1/auth/register` | 409 |
| 6 | Login wrong password | `POST /api/v1/auth/login` | 401 |
| 7 | Refresh with rotated token | `POST /api/v1/auth/refresh` with old token | 401 |

---

## Step 10: Integration Tests — ResidentControllerIT

**File:** `backend/src/test/java/com/barangay/clearance/integration/ResidentControllerIT.java`

**PRD Reference:** Section 4.9

| # | Test | HTTP | Expected |
|---|------|------|----------|
| 1 | List residents as CLERK | `GET /api/v1/residents` | 200; paginated response |
| 2 | Search residents | `GET /api/v1/residents?q=keyword` | 200; filtered results |
| 3 | Create resident as CLERK | `POST /api/v1/residents` | 201 |
| 4 | Get resident by ID | `GET /api/v1/residents/{id}` | 200 |
| 5 | Update resident | `PATCH /api/v1/residents/{id}` | 200 |
| 6 | List pending users as ADMIN | `GET /api/v1/residents/pending` | 200 |
| 7 | Activate user as ADMIN | `POST /api/v1/residents/{userId}/activate` | 200 |
| 8 | Reject user as ADMIN | `POST /api/v1/residents/{userId}/reject` | 200 |
| 9 | List without token | `GET /api/v1/residents` | 401 |

**Note:** Check actual controller endpoints — the PRD references `/api/v1/admin/users/pending` but resident activation may be under `/api/v1/residents/`. Verify against `ResidentController.java` and `UserController.java`.

---

## Step 11: Integration Tests — ClearanceWorkflowIT

**File:** `backend/src/test/java/com/barangay/clearance/integration/ClearanceWorkflowIT.java`

**PRD Reference:** Sections 4.10 (happy path) and 4.11 (rejection path)

### Happy Path Test (single `@Test` method walking through all steps):

| Step | Action | Endpoint | Expected |
|------|--------|----------|----------|
| 1 | Register resident | `POST /api/v1/auth/register` | 201 |
| 2 | Admin activates | `POST /api/v1/residents/{userId}/activate` | 200 |
| 3 | Resident logs in | `POST /api/v1/auth/login` | 200; tokens |
| 4 | Submit clearance (portal) | `POST /api/v1/me/clearances` | 201; status=FOR_APPROVAL |
| 5 | Approver approves | `POST /api/v1/clearances/{id}/approve` | 200; status=APPROVED |
| 6 | Clerk initiates payment | `POST /api/v1/clearances/{id}/payments` with `Idempotency-Key` | 200 |
| 7 | Clerk marks paid (cash) | `POST /api/v1/clearances/{id}/mark-paid` | 200; paymentStatus=PAID |
| 8 | Clerk releases | `POST /api/v1/clearances/{id}/release` | 200; status=RELEASED |
| 9 | Resident downloads PDF | `GET /api/v1/me/clearances/{id}/pdf` | 200; Content-Type=application/pdf |

**Note on state machine difference:** The PRD expects DRAFT as the initial state, but the actual code creates requests at FOR_APPROVAL directly from `submitPortal()`. Step 4 asserts FOR_APPROVAL (not DRAFT). There is no separate "submit for approval" step — it's implicit in creation.

### Rejection Path Test:

| Step | Action | Expected |
|------|--------|----------|
| 1-3 | Register, activate, login | (same as happy path) |
| 4 | Submit clearance | 201; status=FOR_APPROVAL |
| 5 | Approver rejects with reason | 200; status=REJECTED; notes contain reason |
| 6 | Resident resubmits | 200; status=FOR_APPROVAL |
| 7 | Reject without reason | 400 |

---

## Step 12: Integration Tests — PaymentControllerIT

**File:** `backend/src/test/java/com/barangay/clearance/integration/PaymentControllerIT.java`

**PRD Reference:** Section 4.12

| # | Test | Expected |
|---|------|----------|
| 1 | Initiate payment with fresh key on APPROVED clearance | 200; `idempotent=false` |
| 2 | Replay same key on SUCCESS payment | 200; `idempotent=true` |
| 3 | Replay same key on PENDING payment | 409 |
| 4 | Missing Idempotency-Key header | 400 |
| 5 | Payment on non-APPROVED clearance | 400 |

**Setup:** Each test needs a clearance in the appropriate state. Create helper methods that walk the clearance through register → activate → submit → approve.

---

## Step 13: Integration Tests — SettingsControllerIT

**File:** `backend/src/test/java/com/barangay/clearance/integration/SettingsControllerIT.java`

**PRD Reference:** Section 4.13

| # | Test | Expected |
|---|------|----------|
| 1 | GET settings as ADMIN | 200; has barangayName, captainName |
| 2 | PUT settings as ADMIN | 200; subsequent GET returns updated values |
| 3 | Upload valid PNG logo as ADMIN | 204 |
| 4 | Upload oversized file | 400 |
| 5 | Upload non-image MIME type | 400 |
| 6 | PUT settings as CLERK | 403 |

---

## Step 14: Integration Tests — SecurityGuardIT

**File:** `backend/src/test/java/com/barangay/clearance/integration/SecurityGuardIT.java`

**PRD Reference:** Section 4.14

| # | Test | Expected |
|---|------|----------|
| 1 | RESIDENT approves clearance | 403 |
| 2 | CLERK approves clearance | 403 |
| 3 | CLERK accesses admin users | 403 |
| 4 | APPROVER accesses admin users | 403 |
| 5 | No token on residents list | 401 |
| 6 | RESIDENT lists own clearances | 200 |
| 7 | RESIDENT accesses another's clearance | 403 or 404 |
| 8 | RESIDENT accesses settings | 403 |

**Setup:** Requires a seeded clearance in FOR_APPROVAL state for the approve-attempt tests. Use `BaseIntegrationTest` helper methods for tokens.

---

## Step 15: Verify all tests pass

**Command:** `cd backend && ./mvnw test`

**Definition of Done:**

- [ ] All 6 unit test classes compile and pass
- [ ] `BaseIntegrationTest` starts PostgreSQL container once per JVM
- [ ] All 6 integration test classes pass against Testcontainers PostgreSQL
- [ ] `ClearanceWorkflowIT` covers the full DRAFT→RELEASED happy path (noting FOR_APPROVAL is the initial state)
- [ ] `ClearanceWorkflowIT` covers the rejection→resubmission path
- [ ] `SecurityGuardIT` covers at minimum 4 unauthorized + 2 authorized scenarios
- [ ] Full backend test suite completes in under 3 minutes (excluding first image pull)
- [ ] No flaky tests over 3 consecutive runs

---

## Deviations from PRD

These differences between the PRD and actual codebase must be accounted for:

| PRD Expectation | Actual Code | Impact on Tests |
|-----------------|-------------|-----------------|
| DRAFT is the initial state | Requests start at FOR_APPROVAL | No DRAFT→FOR_APPROVAL transition test needed |
| `submit()` method for DRAFT→FOR_APPROVAL | `submitPortal()` creates at FOR_APPROVAL | Test `submitPortal()` instead |
| `resubmit()` returns to DRAFT | `resubmit()` returns to FOR_APPROVAL | Assert FOR_APPROVAL after resubmit |
| `generatePdf(clearanceId)` on service | `generate(ClearanceRequest, Resident, BarangaySettings)` | No 404 test at service level; test with entity fixtures |
| `markAsPaid()` throws 409 on duplicate | `markPaid()` is idempotent (returns existing) | Test idempotent behavior instead of 409 |
| `isTokenValid(token, userDetails)` method | `getClaims(token)` throws on invalid (no boolean method) | Test via `extractXxx()` throwing exceptions |
| Payment `PENDING` status | Payment uses `PENDING`, `SUCCESS`, `FAILED` | Align test assertions with actual enum values |
