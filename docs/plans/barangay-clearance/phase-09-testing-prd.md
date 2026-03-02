# PRD: Automated Testing & CI Pipeline — Barangay Clearance System

## 1. Product overview

### 1.1 Document title and version

- PRD: Automated Testing & CI Pipeline — Barangay Clearance System
- Version: 1.0

### 1.2 Product summary

The Barangay Clearance System is a modular monolith web application that digitizes clearance issuance for Filipino local government units. Phases 0–8 and 11 are complete, delivering a full-featured system with identity and auth, residents management, a clearance state machine (DRAFT → FOR_APPROVAL → APPROVED → RELEASED, with a rejection path), payment processing with idempotency, PDF generation, settings management, reports, and system user management.

Phase 9 introduces a formal automated testing strategy and a GitHub Actions CI pipeline. The goal is to provide developers with a fast, reliable safety net for the business-critical clearance workflow and security boundaries, while keeping the test suite lean and maintainable. Tests focus exclusively on code paths that carry the highest risk if broken: the clearance state machine, JWT security, payment idempotency, and role-based access control.

This document defines all required test classes, scenarios, infrastructure setup, and CI pipeline configuration. It is the authoritative specification for Phase 9 delivery and serves as a reference for any contributor adding tests going forward.

## 2. Goals

### 2.1 Business goals

- Prevent regressions in the clearance state machine, which is the core revenue-generating workflow.
- Enforce security boundaries between roles (RESIDENT, CLERK, APPROVER, ADMIN) so that no unauthorized action can be performed through a broken access-control rule.
- Provide barangay staff with confidence that deployments will not silently break clearance issuance or payment recording.
- Create a repeatable, automated quality gate that runs on every pull request without requiring manual QA intervention for routine changes.
- Establish a CI pipeline that gates merges to main on passing tests, reducing the cost of defect discovery.

### 2.2 User goals

- **Developers** can run the full backend and frontend test suites locally with a single command and receive clear, actionable failure messages.
- **Developers** opening a pull request receive automated test results within minutes, without needing to configure anything manually.
- **QA / Tech Lead** can read a CI report and immediately know which scenarios passed or failed across unit, integration, and end-to-end layers.
- **Future contributors** can add new tests by following the established patterns (BaseIntegrationTest, Vitest config, Cypress specs) without re-architecting the test infrastructure.

### 2.3 Non-goals

- Performance or load testing.
- Visual regression testing (e.g., Percy, Chromatic).
- Contract testing (e.g., Pact).
- Code coverage reporting or enforcement of a coverage threshold.
- Mock Service Worker (MSW) for frontend tests.
- ArchUnit structural rules.
- WireMock for external service stubbing.
- End-to-end testing of email or notification delivery.
- Mutation testing.
- Accessibility (a11y) automated testing.
- Testing of Phase 12 (Audit Logging) — that phase has its own specification.

## 3. User personas

### 3.1 Key user types

- Backend developer writing and running Java tests.
- Frontend developer writing and running TypeScript/Vitest tests.
- QA engineer reviewing CI results and writing Cypress specs.
- Tech lead / architect reviewing pull request CI gates.

### 3.2 Basic persona details

- **Backend developer**: A Java developer familiar with Spring Boot, JUnit 5, and Testcontainers. Needs reliable, fast unit tests for service logic and integration tests that exercise the real PostgreSQL schema without requiring a manually managed database.
- **Frontend developer**: A TypeScript developer familiar with Next.js 14, React Testing Library, and Vitest. Needs hook and context tests that verify data-fetching logic and auth state without spinning up a backend.
- **QA engineer**: Comfortable with Cypress and familiar with the clearance workflow end-to-end. Needs a reproducible Docker Compose environment and clear seed data to write deterministic specs.
- **Tech lead**: Reviews CI runs in GitHub Actions to decide whether a PR is safe to merge. Needs a clean job summary with pass/fail per layer.

### 3.3 Role-based access

- **RESIDENT**: Can submit clearances and view own clearances. Cannot access backoffice routes or perform staff actions.
- **CLERK**: Can manage residents, view clearances, record payments. Cannot approve/reject clearances or administer users.
- **APPROVER**: Can approve or reject clearances in FOR_APPROVAL state. Cannot manage residents or administer users.
- **ADMIN**: Full access including user management and settings. Cannot be impersonated by lower roles.

## 4. Functional requirements

### 4.1 Backend unit tests — JwtServiceTest

**Priority: High**

- Test JWT access token generation: given a valid `User` entity, `generateAccessToken()` returns a non-blank string that can be parsed by the same `JwtService`.
- Test access token validation: a freshly generated token satisfies `isTokenValid(token, userDetails)` → true.
- Test token expiry: a token constructed with a past `expiredAt` claim fails `isTokenValid()` → false.
- Test tampered signature: modifying any character in the token payload causes `isTokenValid()` to throw or return false (never silently pass).
- Test `extractUsername(token)` returns the email embedded in the token subject claim.
- Test `extractRole(token)` returns the role string embedded in the token claims.
- Test refresh token generation: `generateRefreshToken()` returns a UUID-format string different from the access token.
- All tests use Mockito only; no Spring context is loaded.

### 4.2 Backend unit tests — AuthServiceTest

**Priority: High**

- Test register success: given a unique email and valid password, `register()` persists a new `User` with `PENDING` status, hashes the password via BCrypt, and returns an `AuthResponse` with non-null access and refresh tokens.
- Test register duplicate email: calling `register()` with an email that already exists throws `AppException` with HTTP 409 Conflict.
- Test login success: given `ACTIVE` user status and correct password, `login()` returns `AuthResponse` with non-null tokens.
- Test login wrong password: `login()` throws `AppException` with HTTP 401 Unauthorized.
- Test login with PENDING status: `login()` throws `AppException` with HTTP 403 Forbidden (account not yet activated).
- Test refresh token: given a valid raw refresh token, `refresh()` returns a new `AuthResponse` with a fresh access token and rotated refresh token.
- Test refresh with expired/revoked token: `refresh()` throws `AppException` with HTTP 401 Unauthorized.
- Test logout: `logout()` marks the stored refresh token hash as revoked so a subsequent `refresh()` call with the same raw token fails.
- All tests mock `UserRepository`, `RefreshTokenRepository`, `JwtService`, `PasswordEncoder`.

### 4.3 Backend unit tests — ClearanceServiceTest

**Priority: Critical**

State machine transitions (each transition tested independently):

- DRAFT → FOR_APPROVAL: `submit()` on a DRAFT clearance owned by the calling resident returns the clearance in FOR_APPROVAL state.
- FOR_APPROVAL → APPROVED: `approve()` on a FOR_APPROVAL clearance by an APPROVER sets status to APPROVED.
- FOR_APPROVAL → REJECTED: `reject()` on a FOR_APPROVAL clearance with a non-blank reason sets status to REJECTED.
- APPROVED → RELEASED: `release()` on an APPROVED clearance whose associated payment is PAID sets status to RELEASED and records `releasedAt`.
- REJECTED → DRAFT: `resubmit()` on a REJECTED clearance sets status back to DRAFT, clearing the rejection reason.

Illegal transition guards (each must throw `AppException` with HTTP 400):

- Calling `approve()` on a DRAFT clearance.
- Calling `approve()` on an already APPROVED clearance.
- Calling `approve()` on a RELEASED clearance.
- Calling `reject()` on a DRAFT clearance.
- Calling `release()` on a FOR_APPROVAL clearance.
- Calling `release()` on a REJECTED clearance.
- Calling `submit()` on a FOR_APPROVAL clearance (already submitted).

Validation guards:

- `reject()` with a blank or null reason throws `AppException` with HTTP 400.
- `release()` when payment status is UNPAID throws `AppException` with HTTP 400.
- `submit()` by a user who does not own the clearance throws `AppException` with HTTP 403.

All tests mock `ClearanceRepository`, `PaymentRepository`, `ClearanceNumberService`, and `ResidentRepository`.

### 4.4 Backend unit tests — ClearanceNumberServiceTest

**Priority: High**

- Test sequential number assignment: calling `nextNumber(yearMonth)` twice in sequence returns `YYYY-MM0001` then `YYYY-MM0002`.
- Test monthly reset: `nextNumber()` for month A and month B produce independent sequences starting from 0001 each.
- Test concurrent uniqueness: simulate 10 concurrent calls to `nextNumber()` for the same month; all 10 returned strings are distinct (no duplicates).
- Test format: the returned string matches the pattern `^\d{4}-\d{2}\d{4}$` (e.g., `2025-020001`).
- All tests mock or stub the `ClearanceNumberSequenceRepository`; no database required.

### 4.5 Backend unit tests — PaymentServiceTest

**Priority: High**

- Test new payment — online: `initiatePayment()` with a fresh idempotency key calls `PaymentGateway.charge()`, persists a `Payment` record with PENDING status, and returns a `PaymentResponse` with `idempotent: false`.
- Test idempotent replay — completed: `initiatePayment()` with the same key for a PAID payment skips the gateway call and returns the cached `PaymentResponse` with `idempotent: true`.
- Test idempotent replay — pending: `initiatePayment()` with the same key for a PENDING payment throws `AppException` with HTTP 409 Conflict.
- Test already PAID: calling `initiatePayment()` when the clearance already has a PAID payment throws `AppException` with HTTP 409 Conflict.
- Test cash mark-as-paid: `markAsPaid()` on an UNPAID payment record sets status to PAID and records `paidAt`.
- Test duplicate mark-as-paid: `markAsPaid()` on an already PAID payment throws `AppException` with HTTP 409 Conflict.
- Test missing idempotency key header: validation at the controller layer (or service entry) rejects the request with HTTP 400 Bad Request when the header is absent.
- All tests mock `PaymentRepository`, `ClearanceRepository`, `PaymentGateway`.

### 4.6 Backend unit tests — ClearancePdfServiceTest

**Priority: Medium**

- Test generate returns non-null: `generatePdf(clearanceId)` returns a byte array with length greater than zero.
- Test PDF magic bytes: the first four bytes of the returned array equal `%PDF` (0x25, 0x50, 0x44, 0x46).
- Test null logo does not throw: when `BarangaySettings.logoBytes` is null, `generatePdf()` completes without exception and returns a valid byte array.
- Test clearance not found: `generatePdf()` with an unknown clearance ID throws `AppException` with HTTP 404 Not Found.
- All tests mock `ClearanceRepository`, `SettingsService`, `ResidentRepository`. PDFBox rendering is exercised with real in-memory execution (no mocking of PDFBox internals).

### 4.7 Backend integration tests — BaseIntegrationTest

**Priority: Critical (infrastructure)**

- Define an abstract class `BaseIntegrationTest` annotated with `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@Testcontainers`.
- Declare a static `PostgreSQLContainer<?>` field annotated `@Container` using the singleton container pattern (one container shared across all test classes in the same JVM).
- Use `@ServiceConnection` to wire the container's JDBC URL, username, and password into the Spring `DataSource` without manual property overrides.
- Expose helper methods: `asClerk()`, `asApprover()`, `asAdmin()`, `asResident(userId)` that return `Authorization: Bearer {token}` header strings for pre-seeded users.
- Expose `performGet(url, headers)`, `performPost(url, body, headers)`, `performPatch(url, body, headers)` wrappers over `MockMvc` that return `ResultActions`.
- Flyway runs V1–V3 migrations automatically on container startup; no manual schema setup required.

### 4.8 Backend integration tests — AuthControllerIT

**Priority: High**

- POST `/api/v1/auth/register` with valid payload → 201 Created; response body contains `accessToken` and `refreshToken`.
- POST `/api/v1/auth/login` with seeded ACTIVE user credentials → 200 OK with tokens.
- POST `/api/v1/auth/refresh` with valid `refreshToken` from prior login → 200 OK with new `accessToken`.
- POST `/api/v1/auth/logout` with valid access token → 200 OK; subsequent `refresh` call with the same refresh token → 401 Unauthorized.
- POST `/api/v1/auth/register` with already-registered email → 409 Conflict.
- POST `/api/v1/auth/login` with wrong password → 401 Unauthorized.
- POST `/api/v1/auth/refresh` with a previously used (rotated) refresh token → 401 Unauthorized.

### 4.9 Backend integration tests — ResidentControllerIT

**Priority: Medium**

- GET `/api/v1/residents` with CLERK token → 200 OK; response is paginated.
- GET `/api/v1/residents?search=keyword` → 200 OK; results contain only records matching keyword.
- POST `/api/v1/residents` with CLERK token and valid payload → 201 Created.
- GET `/api/v1/residents/{id}` with CLERK token → 200 OK.
- PATCH `/api/v1/residents/{id}` with CLERK token and valid payload → 200 OK.
- GET `/api/v1/admin/users/pending` with ADMIN token → 200 OK; list contains users with PENDING status.
- PATCH `/api/v1/admin/users/{id}/activate` with ADMIN token → 200 OK; user status becomes ACTIVE.
- PATCH `/api/v1/admin/users/{id}/reject` with ADMIN token → 200 OK; user status becomes REJECTED.
- GET `/api/v1/residents` without token → 401 Unauthorized.

### 4.10 Backend integration tests — ClearanceWorkflowIT (happy path)

**Priority: Critical**

This test class exercises the full clearance lifecycle in a real PostgreSQL environment:

- Step 1: Register a resident user account via `/api/v1/auth/register`.
- Step 2: Admin activates the resident via `/api/v1/admin/users/{id}/activate`.
- Step 3: Resident logs in and obtains tokens.
- Step 4: Resident submits a clearance via POST `/api/v1/clearances` → 201 Created; status is DRAFT.
- Step 5: Resident submits the clearance for approval via PATCH `/api/v1/clearances/{id}/submit` → 200 OK; status is FOR_APPROVAL.
- Step 6: Approver approves via PATCH `/api/v1/clearances/{id}/approve` → 200 OK; status is APPROVED.
- Step 7: Clerk initiates payment via POST `/api/v1/payments` with valid idempotency key → 200 OK.
- Step 8: Clerk marks payment as paid via PATCH `/api/v1/payments/{id}/mark-paid` → 200 OK; payment status is PAID.
- Step 9: Clerk releases via PATCH `/api/v1/clearances/{id}/release` → 200 OK; status is RELEASED.
- Step 10: Resident downloads PDF via GET `/api/v1/clearances/{id}/pdf` → 200 OK; Content-Type is `application/pdf`; body is non-empty.
- Each step asserts the HTTP status code and the relevant state field in the JSON response body.

### 4.11 Backend integration tests — ClearanceWorkflowIT (rejection path)

**Priority: High**

- Step 1–3: Register, activate, and log in a resident (can share setup with happy path via `@BeforeEach`).
- Step 4–5: Submit and submit-for-approval a clearance (status: FOR_APPROVAL).
- Step 6: Approver rejects with reason "Incomplete documents" → 200 OK; status is REJECTED; `rejectionReason` is `"Incomplete documents"`.
- Step 7: Resident edits and resubmits via PATCH `/api/v1/clearances/{id}/resubmit` → 200 OK; status is DRAFT.
- Step 8: Resident submits for approval again → 200 OK; status is FOR_APPROVAL.
- Step 9: Attempt to reject without a reason → 400 Bad Request.

### 4.12 Backend integration tests — PaymentControllerIT

**Priority: High**

- POST `/api/v1/payments` with fresh idempotency key header and APPROVED clearance → 200 OK; `idempotent` field is false.
- POST `/api/v1/payments` with the same idempotency key for a PAID payment → 200 OK; `idempotent` field is true; gateway is not called again.
- POST `/api/v1/payments` with the same idempotency key for a PENDING payment → 409 Conflict.
- POST `/api/v1/payments` without the `Idempotency-Key` header → 400 Bad Request.
- POST `/api/v1/payments` for a clearance in DRAFT status → 400 Bad Request (not yet approved).

### 4.13 Backend integration tests — SettingsControllerIT

**Priority: Medium**

- GET `/api/v1/settings` with ADMIN token → 200 OK; response contains barangay name and captain fields.
- PUT `/api/v1/settings` with ADMIN token and valid payload → 200 OK; subsequent GET returns updated values.
- POST `/api/v1/settings/logo` with ADMIN token and a valid PNG file (under 2 MB) → 204 No Content.
- POST `/api/v1/settings/logo` with ADMIN token and a file exceeding the size limit → 400 Bad Request.
- POST `/api/v1/settings/logo` with ADMIN token and a non-image MIME type (e.g., `text/plain`) → 400 Bad Request.
- PUT `/api/v1/settings` with CLERK token → 403 Forbidden.

### 4.14 Backend integration tests — SecurityGuardIT

**Priority: Critical**

- PATCH `/api/v1/clearances/{id}/approve` with RESIDENT token → 403 Forbidden.
- PATCH `/api/v1/clearances/{id}/approve` with CLERK token → 403 Forbidden.
- GET `/api/v1/admin/users` with CLERK token → 403 Forbidden.
- GET `/api/v1/admin/users` with APPROVER token → 403 Forbidden.
- GET `/api/v1/residents` without any token → 401 Unauthorized.
- GET `/api/v1/clearances` with RESIDENT token → 200 OK (residents may list their own clearances).
- GET `/api/v1/clearances/{id}` with RESIDENT token for a clearance not owned by that resident → 403 Forbidden.
- POST `/api/v1/settings` with RESIDENT token → 403 Forbidden.

### 4.15 Frontend unit tests — AuthContext

**Priority: High**

- `login(credentials)` stores access token and refresh token in localStorage and updates context state with the decoded user role.
- `logout()` clears localStorage tokens and resets context state to unauthenticated.
- On re-hydration: when localStorage contains a valid (non-expired) access token, `AuthContext` initialises with `isAuthenticated: true` and the correct role.
- On re-hydration: when localStorage contains an expired access token, `AuthContext` calls the refresh API and, on success, stores the new token and sets `isAuthenticated: true`.
- On re-hydration: when localStorage contains an expired access token and refresh also fails (401), `AuthContext` clears storage and sets `isAuthenticated: false`.
- All tests mock the `api.ts` Axios instance at the module level; no real HTTP calls are made.

### 4.16 Frontend unit tests — React Query hooks

**Priority: Medium**

Each hook is tested by rendering a minimal wrapper with `QueryClientProvider` and asserting on loading, success, and error states.

**useClearances**
- Returns `isLoading: true` before the API response resolves.
- Returns paginated clearance data matching the mocked API response on success.
- Returns `isError: true` when the API call rejects.

**useResidents**
- Returns paginated resident data matching the mocked response on success.
- Passes search query parameter through to the API call.

**useSettings**
- Returns settings data on success.
- Exposes a `updateSettings` mutation that calls the PUT endpoint with the provided payload.

**useReports**
- Returns report data filtered by the provided date range parameters.
- Returns `isError: true` when the API call rejects with a 403 (no access).

### 4.17 Frontend unit tests — middleware route guards

**Priority: High**

Test `middleware.ts` by calling the middleware function with mock `NextRequest` objects:

- Unauthenticated request to `/backoffice/clearances` → redirected to `/login`.
- Unauthenticated request to `/portal/my-requests` → redirected to `/login`.
- Authenticated RESIDENT request to `/backoffice/clearances` → redirected to `/portal` (role mismatch).
- Authenticated CLERK request to `/backoffice/clearances` → allowed through (no redirect).
- Authenticated ADMIN request to `/backoffice/admin/users` → allowed through.
- Authenticated RESIDENT request to `/portal/my-requests` → allowed through.
- Request to `/login` (public route) → always allowed through regardless of auth state.
- Request to `/api/v1/auth/register` (public API route) → always allowed through.

### 4.18 End-to-end tests — auth flow spec

**Priority: High**

Spec file: `cypress/e2e/auth.cy.ts`

- Visit `/register`; fill form with valid data; submit; assert redirect to a confirmation or pending page.
- Visit `/login`; fill credentials for a pre-seeded ACTIVE resident; submit; assert redirect to `/portal`.
- On the portal page, assert the navbar shows the logged-in user's name.
- Click logout; assert redirect to `/login`; assert localStorage no longer contains an access token.
- Visit `/login` again with the same credentials; submit; assert login succeeds (session persistence check).
- Attempt login with wrong password; assert an error message is displayed on the login page.

### 4.19 End-to-end tests — full clearance lifecycle spec

**Priority: Critical**

Spec file: `cypress/e2e/clearance-lifecycle.cy.ts`

This spec uses pre-seeded users for all three roles (resident, clerk, approver) to avoid register/activate steps inside the test:

- **As resident**: log in, navigate to `/portal/new-request`, fill out the clearance request form, submit → assert clearance appears in "My Requests" with status DRAFT.
- **As resident**: submit the clearance for approval from the request detail page → assert status changes to FOR_APPROVAL.
- **As approver**: log in, navigate to `/backoffice/clearances`, find the submitted clearance, click Approve → assert status changes to APPROVED.
- **As clerk**: log in, navigate to the clearance detail, click Record Payment, fill form with valid amount and online method, submit → assert payment status is PENDING.
- **As clerk**: mark the payment as paid → assert payment status is PAID.
- **As clerk**: click Release → assert clearance status changes to RELEASED.
- **As clerk**: click Download PDF → assert that a file download is triggered (or PDF opens in new tab) without a browser error.

### 4.20 End-to-end tests — role guards spec

**Priority: High**

Spec file: `cypress/e2e/role-guards.cy.ts`

- Unauthenticated user visits `/backoffice/clearances` → redirected to `/login`.
- Unauthenticated user visits `/portal/my-requests` → redirected to `/login`.
- Pre-seeded RESIDENT user logs in and navigates directly to `/backoffice/clearances` → redirected to `/portal` (access denied).
- Pre-seeded CLERK user logs in and navigates to `/backoffice/admin/users` → redirected or shown an access-denied message.
- Pre-seeded APPROVER user logs in and navigates to `/backoffice/residents` → denied (if APPROVER does not have resident management access).

### 4.21 CI pipeline — GitHub Actions workflow

**Priority: Critical**

Workflow file: `.github/workflows/ci.yml`

- **Triggers**: `pull_request` targeting `main`; `push` to `main`.
- **Job: backend-tests**
  - Runner: `ubuntu-latest`.
  - Runs on: PR + push to main.
  - Steps: checkout → set up JDK 21 (Temurin) with Maven cache → `./mvnw test -pl backend` → upload Surefire test reports as artifact.
  - Testcontainers manages its own PostgreSQL container; no GitHub Actions service container is needed.
- **Job: frontend-tests**
  - Runner: `ubuntu-latest`.
  - Runs on: PR + push to main.
  - Steps: checkout → set up Node.js 20 with npm cache → `npm ci` in `frontend/` → `npm run test` → upload test results as artifact.
- **Job: e2e-tests**
  - Runner: `ubuntu-latest`.
  - Runs on: push to main ONLY (not on PRs).
  - Condition: `if: github.event_name == 'push' && github.ref == 'refs/heads/main'`.
  - Steps: checkout → set up JDK 21 and Node.js 20 → build backend JAR (`./mvnw clean package -DskipTests -pl backend`) → `docker compose -f docker-compose.test.yml up -d --wait` → `npm run cypress:run` in `frontend/` → `docker compose -f docker-compose.test.yml down` → upload Cypress screenshots/videos as artifact on failure.
- **Job dependencies**: `e2e-tests` has no `needs` dependency on `backend-tests` or `frontend-tests`; all three jobs run in parallel when triggered.
- **Failure behavior**: any job failure causes the overall workflow to report failure, blocking the PR or flagging the push.

### 4.22 Test infrastructure — application-test profile

**Priority: Critical**

- Create `backend/src/main/resources/application-test.yml`.
- Disable Flyway's `baseline-on-migrate` if set; enable `clean-on-validation-error: false`.
- Set `spring.jpa.show-sql: false` to keep test output readable.
- Set JWT secret to a fixed test value (e.g., a 256-bit base64 string) so tokens are deterministic across test runs.
- Set `app.jwt.access-token-expiration` to a long value (e.g., 1 hour) to prevent accidental expiry mid-test.
- Set `app.jwt.refresh-token-expiration` to 7 days.
- Disable any scheduled tasks or async jobs that would interfere with test assertions.

### 4.23 Test infrastructure — docker-compose.test.yml

**Priority: High**

- Define services: `postgres`, `backend`, `frontend`.
- `postgres`: image `postgres:16-alpine`; expose port 5432; environment variables for DB name, user, password matching the test profile.
- `backend`: built from `backend/Dockerfile` (or `backend/` directory with Jib); profile `test`; `depends_on: postgres` with `condition: service_healthy`; health check on `/actuator/health`; expose port 8080.
- `frontend`: built from `frontend/Dockerfile`; environment variable `NEXT_PUBLIC_API_URL=http://backend:8080`; `depends_on: backend` with `condition: service_healthy`; expose port 3000.
- All services in a dedicated network `barangay-test-net`.
- A `volumes` entry for postgres data to ensure clean state between runs (or `tmpfs` for ephemeral storage).

### 4.24 Test infrastructure — Cypress configuration

**Priority: High**

- Cypress config file: `frontend/cypress.config.ts`.
- `baseUrl`: `http://localhost:3000`.
- `specPattern`: `cypress/e2e/**/*.cy.ts`.
- `video`: `true` (recorded for CI artifact upload on failure).
- `screenshotOnRunFailure`: `true`.
- `defaultCommandTimeout`: 10000 ms.
- E2E support file: `cypress/support/e2e.ts`; define custom commands: `cy.loginAs(role)` that calls the backend auth API directly (bypassing the UI login form) and sets tokens in localStorage for speed.
- Seed SQL script: `cypress/fixtures/seed.sql`; contains INSERT statements for test users (one per role: RESIDENT, CLERK, APPROVER, ADMIN) and a pre-seeded barangay settings row. This script is executed via `docker compose exec postgres psql` before the Cypress run begins.

### 4.25 Frontend test infrastructure — Vitest configuration

**Priority: High**

- Vitest config file: `frontend/vitest.config.ts`.
- Environment: `jsdom`.
- `globals: true` so tests do not need to import `describe`, `it`, `expect` explicitly.
- Setup file: `frontend/src/test/setup.ts`; imports `@testing-library/jest-dom/vitest` for extended matchers.
- Alias `@/` to `frontend/src/` to match the Next.js `tsconfig` path alias.
- `coverage` provider: disabled (out of scope per section 2.3).
- Add `test` script to `frontend/package.json`: `"test": "vitest run"`.
- Add `test:watch` script: `"test:watch": "vitest"`.
- Add `cypress:run` script: `"cypress:run": "cypress run"`.
- Add `cypress:open` script: `"cypress:open": "cypress open"`.

## 5. User experience

### 5.1 Entry points and first-time user flow

- A developer clones the repository and runs `cd backend && ./mvnw test`; Testcontainers pulls a PostgreSQL image on first run and executes all unit and integration tests.
- A developer runs `cd frontend && npm ci && npm run test` to execute all Vitest tests.
- A developer with Docker installed runs `docker compose -f docker-compose.test.yml up -d` then `npm run cypress:open` from the frontend directory to launch Cypress interactively.
- On opening a pull request, GitHub Actions automatically triggers the `backend-tests` and `frontend-tests` jobs and reports results on the PR.

### 5.2 Core experience

- **Running backend tests locally**: a single Maven command executes all unit and integration tests; Testcontainers manages the PostgreSQL lifecycle transparently; test output shows a clear summary of passed/failed/skipped tests per class.
- **Running frontend tests locally**: `npm run test` runs Vitest in single-run mode; output is concise and shows file-level pass/fail.
- **CI PR check**: within approximately 3–5 minutes of opening a PR, the developer sees green or red status checks for backend-tests and frontend-tests directly on the pull request.
- **CI main push**: after merging to main, the e2e-tests job spins up Docker Compose, runs Cypress specs, and reports results; failure uploads screenshots and videos as downloadable CI artifacts.

### 5.3 Advanced features and edge cases

- Testcontainers singleton container: the PostgreSQL container is started once per JVM process and shared across all `BaseIntegrationTest` subclasses, preventing startup overhead from multiplying with the number of test classes.
- `@BeforeEach` database cleanup: each integration test method clears all non-settings tables before running to ensure isolation without restarting the container.
- Cypress `cy.loginAs(role)` custom command: bypasses the UI login form by calling the API directly, reducing test execution time and decoupling E2E auth mechanics from UI form testing (which is covered in the auth spec).
- Concurrent number generation test: the `ClearanceNumberServiceTest` concurrent scenario uses `CompletableFuture` with a fixed thread pool to simulate 10 parallel callers; assertions use a `Set` to verify no duplicate numbers were returned.

### 5.4 UI/UX highlights

- CI status checks are named descriptively: `backend-tests`, `frontend-tests`, `e2e-tests` — not generic names like `build` — so developers immediately know which layer failed.
- Cypress failure artifacts (screenshots, videos) are attached to the CI run with a 7-day retention period, enabling post-hoc debugging without re-running the suite.
- Vitest output in CI uses the `verbose` reporter so individual test names are visible in the GitHub Actions log without clicking into steps.

## 6. Narrative

A barangay clerk submits a routine code change that accidentally inverts the state machine guard — allowing a RELEASED clearance to be approved again. Before Phase 9, this bug would reach production silently. After Phase 9, the `ClearanceServiceTest` catches the illegal transition in under a second during the developer's local build. The CI pipeline catches it again on the pull request before any reviewer even sees the code. The full clearance lifecycle integration test (`ClearanceWorkflowIT`) provides a second layer of assurance by walking the real database through the complete DRAFT-to-RELEASED journey. And on merge to main, the Cypress spec verifies the same journey from a real browser, giving the barangay staff the confidence that what they see in production is exactly what was tested.

## 7. Success metrics

### 7.1 User-centric metrics

- A developer can run the full backend test suite locally from a clean checkout in under 3 minutes (excluding the first Testcontainers image pull).
- A developer can run the full frontend test suite in under 30 seconds.
- A PR CI run (backend + frontend jobs in parallel) completes in under 5 minutes.
- Zero flaky tests: no test fails intermittently due to timing, port conflicts, or container startup race conditions over 20 consecutive CI runs.

### 7.2 Business metrics

- All 8 unit test classes are implemented and passing before Phase 9 is closed.
- All 6 integration test classes are implemented and passing before Phase 9 is closed.
- All 3 Cypress specs are implemented and passing in a clean Docker Compose environment before Phase 9 is closed.
- The GitHub Actions workflow executes on every pull request to main without manual intervention.

### 7.3 Technical metrics

- The `ClearanceWorkflowIT` happy-path test covers all 4 clearance status values (DRAFT, FOR_APPROVAL, APPROVED, RELEASED) and both payment statuses (UNPAID, PAID) in a single connected flow.
- The `SecurityGuardIT` covers at minimum 4 unauthorized access attempts (wrong role or no token) and 2 authorized access confirmations.
- The `ClearanceServiceTest` covers all 7 valid state transitions and all 7 illegal transition guards documented in the state machine.
- Cypress specs cover all 3 user roles (RESIDENT, CLERK, APPROVER) in the lifecycle spec.

## 8. Technical considerations

### 8.1 Integration points

- **Testcontainers + Spring Boot 3.3**: use `@ServiceConnection` (Spring Boot 3.1+) to eliminate manual `@DynamicPropertySource` boilerplate. The `spring-boot-testcontainers` module on the test classpath enables this.
- **Flyway in test**: the `application-test.yml` profile must set `spring.flyway.locations` to `classpath:db/migration` so V1–V3 run in the Testcontainers PostgreSQL instance. No separate test-only migrations are required.
- **Vitest + Next.js 14**: Next.js uses ESM and custom Webpack aliases. The Vitest config must declare `resolve.alias` for `@/` and configure `environment: 'jsdom'`. The `next/navigation` and `next/router` modules must be mocked at the module level since they depend on Next.js internals unavailable outside the framework runtime.
- **Cypress + Docker Compose**: the CI job must wait for the frontend service to be healthy before launching Cypress. Use `docker compose up --wait` (Compose v2.1+) with health checks defined on all services, or a `wait-on` npm package step as a fallback.
- **GitHub Actions + Testcontainers**: the `ubuntu-latest` runner includes Docker; no additional setup is needed for Testcontainers to pull and run the PostgreSQL image.

### 8.2 Data storage and privacy

- Integration tests use an isolated Testcontainers PostgreSQL instance with no connection to any shared or production database.
- E2E tests use the `docker-compose.test.yml` postgres service with a dedicated `barangay_test` database, destroyed after each CI run.
- Seed data contains fictitious names and addresses only; no real resident data is used in any test fixture.
- Test JWT secrets are fixed strings committed to the repository; they are only used in test profiles and carry no production privilege.

### 8.3 Scalability and performance

- The Testcontainers singleton pattern (`static @Container`) is mandatory to prevent the PostgreSQL startup cost (approximately 3–5 seconds) from multiplying with each test class. All `BaseIntegrationTest` subclasses share one container instance per Maven Surefire fork.
- `@BeforeEach` cleanup via `TRUNCATE ... CASCADE` (or `DELETE FROM` in dependency order) ensures test isolation without the cost of container restart.
- Vitest's parallel execution mode (`pool: 'threads'`) is acceptable for hook tests since they mock all I/O. If any test exhibits state sharing issues, switch individual files to `singleThread` via file-level config.
- Cypress specs are isolated by test: each spec uses `cy.loginAs()` to set a fresh session, avoiding cross-spec token pollution.

### 8.4 Potential challenges

- **Flaky E2E tests due to async UI rendering**: Cypress assertions on status badges after API mutations must use `cy.contains()` or `cy.get()` with `.should('have.text', ...)` retry semantics rather than immediate assertions. Avoid `cy.wait(ms)` for fixed delays.
- **Testcontainers image pull in CI**: the first run may time out if the `postgres:16-alpine` image is not cached. Mitigate by adding a Docker layer cache step using `actions/cache` with the Docker cache directory, or rely on GitHub-hosted runner caching which retains images between runs on the same runner pool.
- **Next.js module resolution in Vitest**: `next/navigation` (used in `middleware.ts` and hooks) is not available in a jsdom environment. These modules must be mocked with `vi.mock('next/navigation', ...)` in the test setup file or per test file.
- **Clearance number concurrency test reliability**: the concurrent uniqueness test using `CompletableFuture` may behave differently under unit-test conditions vs. production PostgreSQL atomicity. The unit test verifies the service logic contracts; the integration test should include a concurrent scenario against the real database to validate the `RETURNING` atomic query.
- **Docker Compose availability on CI runner**: confirm the `ubuntu-latest` runner uses a Compose v2 plugin (`docker compose`) rather than the legacy `docker-compose` standalone binary. The workflow YAML must use `docker compose` (space, not hyphen).

## 9. Milestones and sequencing

### 9.1 Project estimate

- Medium: 5–8 business days

### 9.2 Team size and composition

- Small team: 1 backend developer (Java/Spring), 1 frontend developer (TypeScript/Next.js), optional QA engineer for Cypress

### 9.3 Suggested phases

- **Phase 9.1**: Backend unit tests (2 days)
  - Implement `JwtServiceTest`, `AuthServiceTest`, `ClearanceServiceTest`, `ClearanceNumberServiceTest`, `PaymentServiceTest`, `ClearancePdfServiceTest`.
  - All tests pass with `./mvnw test -pl backend`.

- **Phase 9.2**: Backend integration tests + test infrastructure (2 days)
  - Implement `BaseIntegrationTest` and `application-test.yml`.
  - Implement `AuthControllerIT`, `ResidentControllerIT`, `ClearanceWorkflowIT` (happy + rejection), `PaymentControllerIT`, `SettingsControllerIT`, `SecurityGuardIT`.
  - All integration tests pass with `./mvnw test -pl backend`.

- **Phase 9.3**: Frontend tests + Vitest infrastructure (1–2 days)
  - Add Vitest, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event` to `frontend/package.json`.
  - Create `vitest.config.ts` and `src/test/setup.ts`.
  - Implement `AuthContext.test.tsx`, hook tests, and middleware tests.
  - All frontend tests pass with `npm run test`.

- **Phase 9.4**: E2E tests + Docker Compose + CI pipeline (2 days)
  - Add Cypress to `frontend/package.json`.
  - Create `frontend/cypress.config.ts`, `cypress/support/e2e.ts`, `cypress/fixtures/seed.sql`.
  - Implement `auth.cy.ts`, `clearance-lifecycle.cy.ts`, `role-guards.cy.ts`.
  - Create `docker-compose.test.yml`.
  - Create `.github/workflows/ci.yml`.
  - Full E2E run passes against `docker-compose.test.yml`.
  - CI pipeline passes on a test PR and on push to main.

## 10. User stories

### 10.1. Developer runs backend unit tests locally

- **ID**: TST-001
- **Description**: As a backend developer, I want to run all backend unit tests with a single Maven command so that I can verify my changes to service logic before committing.
- **Acceptance criteria**:
  - `./mvnw test -pl backend` executes all unit test classes defined in requirements 4.1–4.6.
  - All tests complete in under 60 seconds on a developer workstation.
  - Failing tests display a clear message identifying the test method name and the assertion that failed.
  - No external services (database, Docker) are required for unit tests to pass.

### 10.2. JwtService token generation and validation are tested

- **ID**: TST-002
- **Description**: As a backend developer, I want `JwtServiceTest` to cover token generation, validation, expiry, and tamper detection so that I have confidence in the authentication foundation.
- **Acceptance criteria**:
  - A freshly generated access token passes `isTokenValid()` → true.
  - A token with a past expiry fails `isTokenValid()` → false (no exception escapes the test).
  - A token with a modified payload character fails validation (returns false or throws; never returns true).
  - `extractUsername()` returns the subject embedded at generation time.
  - `extractRole()` returns the role claim embedded at generation time.
  - All scenarios run without a Spring application context.

### 10.3. AuthService registration and login scenarios are tested

- **ID**: TST-003
- **Description**: As a backend developer, I want `AuthServiceTest` to cover all registration and login paths so that I can detect regressions in account creation and authentication.
- **Acceptance criteria**:
  - Successful registration returns an `AuthResponse` with non-null `accessToken` and `refreshToken`.
  - Registration with a duplicate email throws `AppException` with HTTP 409.
  - Successful login with correct credentials returns tokens.
  - Login with wrong password throws `AppException` with HTTP 401.
  - Login with a PENDING account throws `AppException` with HTTP 403.
  - Refresh with a valid token returns new tokens.
  - Logout followed by refresh with the same token throws `AppException` with HTTP 401.

### 10.4. Clearance state machine transitions are fully unit-tested

- **ID**: TST-004
- **Description**: As a backend developer, I want `ClearanceServiceTest` to assert every valid transition and every illegal transition guard in the state machine so that no state machine regression can go undetected.
- **Acceptance criteria**:
  - All 5 valid transitions (DRAFT→FOR_APPROVAL, FOR_APPROVAL→APPROVED, FOR_APPROVAL→REJECTED, APPROVED→RELEASED, REJECTED→DRAFT) are tested and pass.
  - All 7 illegal transitions listed in requirement 4.3 throw `AppException` with HTTP 400.
  - `reject()` with a blank reason throws `AppException` with HTTP 400.
  - `release()` when payment is UNPAID throws `AppException` with HTTP 400.
  - `submit()` by a non-owner throws `AppException` with HTTP 403.
  - No Spring context is loaded; all dependencies are mocked.

### 10.5. Payment idempotency scenarios are unit-tested

- **ID**: TST-005
- **Description**: As a backend developer, I want `PaymentServiceTest` to cover idempotency replay, conflict cases, and cash payment paths so that payment integrity is verifiable without a real database.
- **Acceptance criteria**:
  - New payment with fresh key returns `idempotent: false`.
  - Replay with same key on PAID payment returns `idempotent: true` and skips the gateway.
  - Replay on PENDING payment throws `AppException` with HTTP 409.
  - `markAsPaid()` on UNPAID sets status to PAID.
  - Duplicate `markAsPaid()` throws `AppException` with HTTP 409.

### 10.6. PDF generation is unit-tested with null-safety

- **ID**: TST-006
- **Description**: As a backend developer, I want `ClearancePdfServiceTest` to verify that PDF generation returns a valid PDF byte array even when the barangay logo is absent.
- **Acceptance criteria**:
  - `generatePdf()` returns a non-null, non-empty byte array.
  - The byte array starts with `%PDF` magic bytes.
  - Calling `generatePdf()` with a null logo does not throw an exception.
  - Calling `generatePdf()` with an unknown clearance ID throws `AppException` with HTTP 404.

### 10.7. Integration test infrastructure is established with Testcontainers

- **ID**: TST-007
- **Description**: As a backend developer, I want a shared `BaseIntegrationTest` class that starts a real PostgreSQL container once per JVM so that all integration tests share a fast, isolated database environment.
- **Acceptance criteria**:
  - `BaseIntegrationTest` is annotated with `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@Testcontainers`.
  - A single `PostgreSQLContainer` is declared `static` with `@Container` and `@ServiceConnection`.
  - Flyway V1–V3 migrations execute automatically on container startup.
  - Helper methods `asClerk()`, `asApprover()`, `asAdmin()`, `asResident(userId)` return valid bearer tokens for pre-seeded users.
  - The container starts only once when multiple integration test classes run in the same Maven build.

### 10.8. Full clearance happy path runs against a real database

- **ID**: TST-008
- **Description**: As a QA engineer, I want `ClearanceWorkflowIT` to walk the complete DRAFT-to-RELEASED path against a real PostgreSQL instance so that the entire workflow is verified end-to-end at the API level.
- **Acceptance criteria**:
  - All 10 steps in requirement 4.10 complete without error.
  - Each step asserts the correct HTTP status code.
  - Each step asserts the relevant state field (status, paymentStatus) in the JSON response.
  - The PDF download step returns Content-Type `application/pdf` and a non-empty body.
  - The test is deterministic: it passes on every run against a clean database.

### 10.9. Clearance rejection and resubmission path is integration-tested

- **ID**: TST-009
- **Description**: As a QA engineer, I want `ClearanceWorkflowIT` to also cover the rejection and resubmission path so that the REJECTED → DRAFT → FOR_APPROVAL cycle is verified against real data.
- **Acceptance criteria**:
  - Rejection sets `status: REJECTED` and populates `rejectionReason`.
  - Resubmission sets `status: DRAFT`.
  - Re-submission-for-approval sets `status: FOR_APPROVAL`.
  - Rejection with a blank reason returns HTTP 400.

### 10.10. Role-based access control is verified by integration tests

- **ID**: TST-010
- **Description**: As a security-conscious developer, I want `SecurityGuardIT` to assert that every unauthorized role receives 401 or 403 on protected endpoints so that access control regressions are caught automatically.
- **Acceptance criteria**:
  - RESIDENT token on `/clearances/{id}/approve` → HTTP 403.
  - CLERK token on `/clearances/{id}/approve` → HTTP 403.
  - CLERK token on `/admin/users` → HTTP 403.
  - APPROVER token on `/admin/users` → HTTP 403.
  - No token on `/residents` → HTTP 401.
  - RESIDENT token on `/clearances` (own list) → HTTP 200.
  - RESIDENT token on another resident's clearance → HTTP 403.

### 10.11. Frontend AuthContext is tested for login, logout, and re-hydration

- **ID**: TST-011
- **Description**: As a frontend developer, I want `AuthContext` tests to verify login, logout, and token re-hydration logic so that authentication state management regressions are caught.
- **Acceptance criteria**:
  - `login()` stores tokens in localStorage and sets `isAuthenticated: true`.
  - `logout()` clears localStorage and sets `isAuthenticated: false`.
  - Re-hydration with a valid stored token sets `isAuthenticated: true` without calling the refresh API.
  - Re-hydration with an expired token calls the refresh API; on success, sets `isAuthenticated: true`.
  - Re-hydration with an expired token and failed refresh sets `isAuthenticated: false` and clears localStorage.
  - All API calls are mocked at the module level; no real HTTP requests are made.

### 10.12. React Query hooks are tested for loading, success, and error states

- **ID**: TST-012
- **Description**: As a frontend developer, I want each React Query hook tested for its loading, success, and error states so that data-fetching regressions surface immediately.
- **Acceptance criteria**:
  - Each hook (`useClearances`, `useResidents`, `useSettings`, `useReports`) has at minimum one test for loading state, one for success state, and one for error state.
  - `useSettings` exposes an `updateSettings` mutation tested with a mock PUT call.
  - `useResidents` passes search query parameters to the API call.
  - All tests render within a `QueryClientProvider` wrapper; no global state pollution between tests.

### 10.13. Middleware route guards redirect unauthenticated and wrong-role users

- **ID**: TST-013
- **Description**: As a frontend developer, I want `middleware.ts` tested with mock requests so that route guard regressions are detected without running a Next.js server.
- **Acceptance criteria**:
  - Unauthenticated request to any `/backoffice/*` path results in a redirect to `/login`.
  - Unauthenticated request to any `/portal/*` path results in a redirect to `/login`.
  - RESIDENT token on `/backoffice/*` results in a redirect to `/portal`.
  - CLERK token on `/backoffice/*` passes through without redirect.
  - ADMIN token on `/backoffice/admin/users` passes through without redirect.
  - Public paths (`/login`, `/register`) always pass through regardless of auth state.

### 10.14. Cypress e2e auth flow spec covers register, login, logout, and session persistence

- **ID**: TST-014
- **Description**: As a QA engineer, I want the Cypress auth spec to verify that a new user can register, log in, log out, and maintain session persistence so that the auth UX is validated in a real browser.
- **Acceptance criteria**:
  - All 6 scenarios in requirement 4.18 pass against the Docker Compose test environment.
  - Login with wrong password displays a visible error message on the page.
  - Logout clears the access token from localStorage.
  - All assertions use Cypress retry semantics (`.should()`) rather than fixed `cy.wait()` delays.

### 10.15. Cypress clearance lifecycle spec covers multi-role happy path

- **ID**: TST-015
- **Description**: As a QA engineer, I want the Cypress clearance lifecycle spec to exercise the full DRAFT-to-RELEASED flow across three user roles in a real browser so that the end-to-end user journey is validated.
- **Acceptance criteria**:
  - All 7 steps in requirement 4.19 complete without Cypress errors.
  - Each status change is verified by asserting a visible status badge on the page.
  - The PDF download step does not result in a browser error or 404.
  - The spec uses `cy.loginAs(role)` for fast role switching without UI login forms (except in the auth spec).

### 10.16. Cypress role guard spec prevents unauthorized navigation

- **ID**: TST-016
- **Description**: As a QA engineer, I want the Cypress role guard spec to confirm that unauthenticated users and wrong-role users cannot access protected pages so that security is verified at the browser level.
- **Acceptance criteria**:
  - Unauthenticated visit to `/backoffice` redirects to `/login`.
  - Unauthenticated visit to `/portal/my-requests` redirects to `/login`.
  - RESIDENT user navigating to `/backoffice` is redirected or shown an access-denied state.
  - All redirect assertions use `cy.url().should('include', '/login')` or equivalent.

### 10.17. GitHub Actions CI workflow runs automatically on PRs and pushes

- **ID**: TST-017
- **Description**: As a tech lead, I want the GitHub Actions workflow to automatically run backend and frontend tests on every PR and push to main so that the team has a consistent, enforced quality gate.
- **Acceptance criteria**:
  - The workflow file exists at `.github/workflows/ci.yml`.
  - `backend-tests` and `frontend-tests` jobs run on both PR and push-to-main triggers.
  - `e2e-tests` job runs only on push to main.
  - A failing job marks the workflow as failed and blocks the PR merge (via required status checks in GitHub branch protection).
  - The workflow uses JDK 21 (Temurin) and Node.js 20.
  - Maven and npm caches are enabled to reduce build time on subsequent runs.

### 10.18. New frontend test dependencies are added to package.json

- **ID**: TST-018
- **Description**: As a frontend developer, I want all required new test dependencies declared in `package.json` so that any developer can reproduce the test environment with `npm ci`.
- **Acceptance criteria**:
  - `vitest`, `@vitest/ui`, `jsdom` are added to `devDependencies`.
  - `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event` are added to `devDependencies`.
  - `cypress` is added to `devDependencies`.
  - `npm run test` executes `vitest run`.
  - `npm run cypress:run` executes `cypress run`.
  - `npm run cypress:open` executes `cypress open`.
  - `npm ci` installs all new dependencies without error after the changes.

### 10.19. Docker Compose test environment starts cleanly and seeds data

- **ID**: TST-019
- **Description**: As a QA engineer, I want `docker-compose.test.yml` to bring up a reproducible postgres + backend + frontend stack and seed test data so that Cypress specs run against a predictable environment.
- **Acceptance criteria**:
  - `docker compose -f docker-compose.test.yml up -d --wait` completes without error.
  - The backend `/actuator/health` endpoint returns `{"status":"UP"}` after startup.
  - The frontend root page returns HTTP 200 after startup.
  - The seed SQL script (`cypress/fixtures/seed.sql`) inserts one user per role and a barangay settings row.
  - `docker compose -f docker-compose.test.yml down -v` cleanly removes all containers and volumes.

### 10.20. Test profile configuration isolates test environment from other profiles

- **ID**: TST-020
- **Description**: As a backend developer, I want `application-test.yml` to define all test-specific configuration so that integration tests never interfere with local or dev environment state.
- **Acceptance criteria**:
  - `application-test.yml` is present at `backend/src/main/resources/application-test.yml`.
  - JWT secret is a fixed string (not loaded from environment variables) so tests are deterministic.
  - Access token expiration is set to at least 1 hour to prevent mid-test expiry.
  - `spring.jpa.show-sql` is false.
  - No scheduled tasks execute during integration tests.
  - Running `./mvnw test -pl backend` with profile `test` does not require any running services outside Testcontainers.
