# Changelog

All notable changes to the Barangay Clearance System are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).  
Versions map to implementation phases. Unreleased sections track work-in-progress.

---

## [Unreleased]

> Work planned but not yet started (Phases 9–11).

---

## [0.7.0] — Phase 9: Testing & QA (Integration Tests) — 2026-03-04

### Added

#### Backend

**Integration Test Infrastructure**

- `BaseIntegrationTest.java` — Base class for all integration tests with:
  - Singleton Testcontainers PostgreSQL (`postgres:16-alpine`) per JVM
  - Dynamic datasource property binding at runtime
  - JWT token helper methods (`asAdmin()`, `asClerk()`, `asApprover()`, `asResident(userId)`)
  - MockMvc wrapper helpers (`performGet()`, `performPost()`, `performPatch()`, `performPut()`, `performMultipart()`)
  - Table truncation helper (`truncateAllTables()`)
  - Staff user seeding helper (`seedStaffUsers()`) for FK-dependent tests
  - Connection pool cleanup in `@AfterEach` to prevent lock contention

**Integration Test Classes (6 classes, 56 IT methods)**

- `AuthControllerIT.java` — 7 tests covering register, login, logout, token refresh with rotation, and duplicate email handling
- `ResidentControllerIT.java` — 9 tests covering CRUD operations, pagination, search, and portal account lifecycle (register → pending → activate/reject)
- `ClearanceWorkflowIT.java` — 2 tests covering end-to-end happy path (submit → approve → mark-paid → release → PDF download) and rejection path (submit → reject → resubmit)
- `PaymentControllerIT.java` — 5 tests covering payment initiation, idempotency key handling, replay scenarios, and concurrent duplicate detection
- `SecurityGuardIT.java` — 8 tests verifying role-based access control (RBAC) guards and authorization enforcement
- `SettingsControllerIT.java` — 6 tests covering settings CRUD, logo upload, file validation, and admin-only access

**Test Configuration**

- `application-test.yml` — Test profile with HikariCP tuning (5-connection pool, 10-second connection timeout, 5-minute idle timeout)
- JWT token expiry extended to 1 hour for test execution (prevents mid-test token expiry on slow CI runners)
- Stub payment gateway always returns `SUCCESS` for repeatable test results

**Dependency Upgrades**

- Testcontainers BOM upgraded from `1.20.1` → `1.21.0` (fixes Docker API version negotiation with Docker Desktop 29.x)
- Added `spring-boot-testcontainers` dependency for seamless `@ServiceConnection` suppport

### Changed

**Security Improvements**

- **Refresh Token Rotation** — Implemented token rotation in `AuthService.refresh()`:
  - Old refresh token marked as revoked after use
  - New refresh token generated and returned in response
  - Reuse of old token after rotation returns `401 Unauthorized`
  - Prevents replay attacks per OAuth 2.0 best practices
  - `Security.md` updated with rotation details and rationale

**Bug Fixes**

- Fixed Docker environment detection for Testcontainers on macOS Docker Desktop 29.x:
  - Root cause: Testcontainers 1.20.1 bundled outdated docker-java library defaulting to `/v1.32` API
  - Docker Desktop 29.x only supports `/v1.44+` API versions
  - Fix: Upgraded Testcontainers to 1.21.0 and configured Maven Surefire with explicit `api.version=1.44` system property
  - RCA document: `backend/docs/RCA/docker-api-version-mismatch.md`

- Fixed foreign key constraint violations in integration tests:
  - Problem: `truncateAllTables()` wiped the `users` table, breaking FK constraints on `clearance_requests.requested_by`, `clearance_requests.reviewed_by`, and `payments.initiated_by_user_id`
  - Solution: Implemented `seedStaffUsers()` helper to re-insert fixed staff user rows (ADMIN_ID, CLERK_ID, APPROVER_ID) after table cleanup
  - RCA document: `backend/docs/RCA/integration-test-fk-fix.md`

- Fixed integration test suite hangs:
  - Problem: Running multiple IT classes sequentially caused stale datasource connections when container lifecycle changed between classes
  - Solution: Implemented singleton container pattern with JVM-lifetime lifecycle and `@DynamicPropertySource` for live runtime datasource binding
  - RCA document: `backend/docs/RCA/integration-test-hanging-fix.md`

### Documentation

- `backend/README.md` — Added comprehensive IT test execution commands and profile notes
- `backend/docs/AUTOMATED_TEST_PLAN.md` — Added Section 9 (Integration Test Architecture) documenting:
  - Testcontainers setup and lifecycle management
  - BaseIntegrationTest class structure and helper methods
  - All 6 integration test classes with test coverage and failure scenarios
  - Configuration details (test profile, JWT expiry, pool sizing)
  - Known issues and resolutions (Docker, FK, hanging tests)
- `backend/docs/Security.md` — Enhanced with token rotation details:
  - Refresh token rotation workflow (old token revocation → new token issuance)
  - Replay attack prevention mechanism
  - OAuth 2.0 / OpenID Connect best practices references
- RCA (Root Cause Analysis) documents for all three integration test fixes

### Testing Metrics

- **Total tests:** 56 unit tests + 56 integration tests = **112 tests**
- **Unit test coverage:** ~92% for critical services (Jwt, Auth, Clearance, Payment, PDF)
- **Integration test coverage:** All controllers, main workflows, RBAC enforcement, happy/sad paths
- **Test execution time:** ~5 seconds (unit tests only, deterministic) + ~30 seconds (full IT suite with Testcontainers startup)
- **All tests passing:** ✅ Zero failures

---

---

## [0.6.0] — Phase 8: Frontend Polish & Navigation — 2026-02-26

### Added / Changed

#### Frontend

**Route Guards**

- `src/middleware.ts` — completed Next.js Edge Middleware implementation: unauthenticated requests to protected paths redirect to `/login?next=<path>`; `/backoffice/admin/**` requires `ADMIN` role (non-admins receive a 403 response); public routes (`/login`, `/register`) redirect already-authenticated users to their role home. Token validation is purely JWT claim-based — no network call at the edge.

**Auth Context**

- `src/context/AuthContext.tsx` — completed with `localStorage` persistence and re-hydration on mount. The access token is mirrored to an `accessToken` cookie on login and re-hydration so `middleware.ts` (running at the Edge before the React tree) can read it for route enforcement. On refresh-token failure, `clearAuth()` wipes both `localStorage` and the cookie.

**Dashboard**

- Backoffice dashboard — three summary cards populated from `GET /clearances/summary` (`pendingApproval`, `approvedAwaitingPayment`, `releasedToday`); auto-refreshes every 30 seconds via `refetchInterval: 30_000`; Skeleton placeholders during initial load.

**Error Toast System**

- `src/components/shared/ErrorToast.tsx` — re-exports `toast` from `sonner`. The `<Toaster>` is mounted once in `providers.tsx`. All previous inline `serverError` / local toast-state patterns replaced with `toast.success` / `toast.error` calls across all pages.

**Loading Skeletons**

- `src/components/shared/LoadingSkeleton.tsx` — `TableRowSkeleton`, `DetailPageSkeleton`, `CardSkeleton` Tailwind-animated placeholder components applied to all list and detail pages to prevent layout shift during data fetching.

**Forced Password Change Flow**

- Portal `layout.tsx` and backoffice `layout.tsx` — both check `mustChangePassword` from `useAuth()` on mount and redirect to `/change-password` if set. Prevents access to any protected page until the user updates their temporary password.

**Mobile Layout**

- Tailwind responsive layout applied throughout: backoffice sidebar collapses to an off-canvas drawer on `< md` breakpoints; portal pages stack to a single column; card grids reflow from multi-column to single-column.

**Shared Components**

- `src/components/shared/StatusBadge.tsx` — color-coded badge for `ClearanceStatus` values.
- `src/components/shared/PaymentBadge.tsx` — color-coded badge for `ClearancePaymentStatus` values.
- `src/components/shared/PageHeader.tsx` — consistent page title + optional breadcrumb slot used across all detail pages.

---

## [0.5.0] — Phase 7: Reports Module — 2026-02-26

### Added

#### Backend

- `ReportRowProjection.java` — Spring Data interface projection for the native SQL report query. Maps selected column aliases directly to getter methods, avoiding the need for `@SqlResultSetMapping` or a MapStruct mapper for native queries.

- `ReportRowDTO.java` — serializable response DTO populated from `ReportRowProjection` fields. Carries: `clearanceNumber`, `residentName`, `purpose`, `status`, `paymentStatus`, `feeAmount`, `createdAt`, `issuedAt`.

- `ReportRepository.java` — native SQL query interface with nullable optional filters (`status`, `from`, `to`, `residentId`). Uses `COALESCE(:param, col) = col` so passing `null` means "no filter on this column", sidestepping JPQL type-inference issues with nullable enum params.

- `ReportsService.java` — builds the `Pageable` from incoming request params and delegates to `ReportRepository`; wraps the result in `PageResponse<ReportRowDTO>`.

- `ReportsController.java` (`GET /api/v1/reports/clearances`, roles: `ADMIN`, `APPROVER`) — accepts optional query params `status`, `from` (ISO date), `to` (ISO date), `residentId`, `page`, `size`; returns paginated `ReportRowDTO` list.

#### Frontend

- `src/types/clearance.ts` — `ReportRow` type added.

- `src/hooks/useReports.ts` — `useReports(params)` TanStack React Query hook; all filter fields are part of the query key so different filter combinations cache independently.

- `/backoffice/reports/page.tsx` — filter panel (status dropdown, from/to date inputs), paginated results table (clearance number, resident name, purpose, status, payment status, date issued), and empty state with explanatory message.

---

## [0.4.2] — Phase 6: Settings Module — 2026-02-26

### Added

#### Backend

**Entities**

- `FeeConfig.java` — singleton fee configuration entity (singleton row enforced by `CHECK (id = 1)`, matching the pattern of `barangay_settings`). Fields: `standardFee`, `rushFee`, `copyCost`, `waiverNotes`.

**Repositories**

- `FeeConfigRepository.java` — `findById(1)` access to the singleton fee row seeded by Flyway V2.

**DTOs**

- `BarangaySettingsDTO.java` — read/write DTO for the barangay profile (name, municipality, province, captain, `hasLogo` boolean).
- `FeeConfigDTO.java` — read/write DTO for fee configuration values.

**Service**

- `SettingsService.java` — full settings management:
  - Get/update barangay profile fields.
  - Logo upload: validates MIME type (PNG/JPEG/GIF only) and file size (max 2 MB); stores bytes in `barangay_settings.logo`.
  - Logo binary retrieval: streams raw bytes with the stored MIME type as `Content-Type`.
  - Get/update fee configuration via `ON CONFLICT (id) DO UPDATE` semantics.

**Controller**

- `SettingsController.java` (`/api/v1/settings`, ADMIN only) — endpoints:
  - `GET /settings` — retrieve barangay profile.
  - `PUT /settings` — update barangay profile.
  - `POST /settings/logo` — upload logo (multipart; validated at service layer).
  - `GET /settings/logo` — stream logo binary.
  - `GET /settings/fees` — retrieve fee configuration.
  - `PUT /settings/fees` — update fee configuration.

### Changed

#### Backend

- `ClearanceService.resolveFee()` — now reads the live `fee_config` row via `FeeConfigRepository` (with a runtime fallback to ₱50/₱100 defaults if the row is absent) instead of hardcoded constants. Fee changes take effect immediately for new clearance requests without a server restart.

#### Frontend

- `src/types/settings.ts` — updated with `BarangaySettings`, `FeeConfig`, `UpdateSettingsPayload`, `UpdateFeesPayload` interfaces; `hasLogo: boolean` flag added to `BarangaySettings`.

- `src/hooks/useSettings.ts` — TanStack React Query hooks: `useBarangaySettings`, `useUpdateSettings`, `useUploadLogo`, `useFeeConfig`, `useUpdateFees`. Logo upload uses Axios `FormData`.

- `/backoffice/admin/settings/page.tsx` — barangay profile form with name, municipality, province, and captain fields; inline logo preview using `GET /settings/logo`; upload button triggers a file input filtered to image types.

- `/backoffice/admin/settings/fees/page.tsx` — fee configuration form: standard fee, rush fee, copy cost, and waiver notes; React Hook Form + Zod validation.

---

## [0.4.1] — Bugfix — 2026-02-26

### Fixed

#### Backend

- `ClearanceNumberService.java` — the **"Release Clearance"** action threw `JpaSystemException` because the `nextSequence()` method in `ClearanceNumberSequenceRepository` used `@Modifying` with a `RETURNING` clause. Spring Data JPA's `@Modifying` annotation expects the query to be a DML statement that returns an **update count** (`int`/`void`), but the PostgreSQL `INSERT … ON CONFLICT DO UPDATE RETURNING last_seq` query produces a **result set**. This mismatch caused JDBC to fail at execute time. Fix: replaced the `@Modifying` repository method with a direct `EntityManager.createNativeQuery()` call, which correctly handles the `RETURNING` clause as a result-set query. The `ClearanceNumberSequenceRepository` interface is retained for any future JPA-standard operations but no longer contains the sequence query itself.

---

## [0.4.0] — Phase 5: PDF Generation — 2026-02-26

### Added

#### Backend

**Settings (partial — prepares Phase 6)**

- `settings/entity/BarangaySettings.java` — JPA entity mapping the singleton `barangay_settings` table (row `id = 1`, enforced by `CHECK (id = 1)` in the schema). Fields: `barangayName`, `municipality`, `province`, `captainName`, `logo` (BYTEA), `logoMimeType`. Created ahead of the full Phase 6 settings module because the PDF service needs to read barangay info and the optional logo for the header.

- `settings/repository/BarangaySettingsRepository.java` — Spring Data repository; usage is `findById(1)` to fetch the singleton row seeded by Flyway V2.

**PDF Service**

- `pdf/service/ClearancePdfService.java` — interface defining `byte[] generate(ClearanceRequest, Resident, BarangaySettings)`. Decouples the PDF rendering strategy from the controllers and business logic.

- `pdf/service/ClearancePdfServiceImpl.java` — PDFBox 3.x implementation that generates an A4 clearance certificate. The PDF is built top-down by decrementing a `y` tracker from the top margin (PDFBox's coordinate origin is bottom-left `(0, 0)`). Layout sections:
  1. **Header** — optional logo image (proportionally scaled to max 80 pt height) on the left, with "Republika ng Pilipinas", province, municipality, and barangay name centered. If `settings.getLogo()` is null or the image bytes are invalid, the logo is silently skipped and a text-only header is rendered — no exception is thrown.
  2. **Horizontal rule** — 1 pt line separating header from body.
  3. **Title** — "BARANGAY CLEARANCE" centered in Helvetica Bold 16 pt.
  4. **Metadata block** — clearance number, date issued (formatted as `MMMM dd, yyyy` in `Asia/Manila` timezone), and validity (6 months from issuance).
  5. **Body paragraph** — "TO WHOM IT MAY CONCERN:" followed by a certification paragraph stating the resident's full name, age (computed via `Period.between`), birthdate, address, and barangay name, plus the purpose of the clearance. Text wrapping is implemented manually via `wrapText()` which measures each word with `PDFont.getStringWidth()` and breaks lines at the content width.
  6. **Signature block** — right-aligned signature line, captain name in uppercase bold, and "Punong Barangay" title beneath.

  Purpose labels are resolved from the `Purpose` enum to human-readable text (e.g. `BUSINESS_PERMIT` → "Business Permit"); for `OTHER`, the free-text `purposeOther` field is used.

**Controller Endpoints**

- `GET /api/v1/clearances/{id}/pdf` — added to `ClearanceController`. Roles: `CLERK`, `ADMIN`. Validates that the clearance is in `RELEASED` status (returns 400 otherwise), fetches the associated `Resident` and `BarangaySettings` (falls back to hardcoded defaults if the settings row is missing), generates the PDF, and streams it with `Content-Type: application/pdf` and `Content-Disposition: attachment; filename="clearance-YYYY-MM-NNNN.pdf"`.

- `GET /api/v1/me/clearances/{id}/pdf` — added to `PortalClearanceController`. Role: `RESIDENT`. Validates both ownership (the clearance must belong to the authenticated resident's profile) and status (must be `RELEASED` — returns 403 otherwise).

**ClearanceService Methods**

- `getReleasedEntity(id)` — returns the raw `ClearanceRequest` entity; throws 400 if not `RELEASED`.
- `getReleasedEntityForResident(id, principalUserId)` — ownership-checked variant; throws 404 if not owned, 403 if not `RELEASED`.
- `getResidentForClearance(residentId)` — fetches the `Resident` entity for PDF rendering.

#### Frontend

- `src/hooks/useClearances.ts` — added `downloadClearancePdf(clearanceId, clearanceNumber)` (backoffice) and `downloadMyClearancePdf(clearanceId, clearanceNumber)` (portal) helper functions. Both issue a `GET` request with `responseType: 'blob'`, then trigger a browser download by creating a temporary object URL (`URL.createObjectURL`), programmatically clicking an anchor element, and revoking the URL immediately after.

- `src/app/portal/requests/[id]/page.tsx` — added a **"Download PDF"** section that appears only when `status === 'RELEASED'`. Renders inside a green-tinted card with a document download icon. Includes loading state (`"Downloading…"`) and error handling.

- `src/app/backoffice/clearances/[id]/page.tsx` — added a **"Print / Download PDF"** button below the action buttons, visible only when `status === 'RELEASED'`. Same blob-download mechanism as the portal variant.

### Changed

#### Frontend

- `src/components/portal/StatusTimeline.tsx` — expanded from 3 steps to 4 steps by inserting a **Payment** step between Approved and Released. The payment step is styled dynamically based on `paymentStatus`:
  - 🟠 Orange ring + label **"Unpaid"** when the request is `APPROVED` and `paymentStatus` is `UNPAID` — draws the resident's attention to the pending payment
  - ✅ Green check + label **"Paid"** or **"Waived"** when `paymentStatus` is `PAID` or `WAIVED`
  - Grey (inactive) when the request has not yet reached the `APPROVED` step

  Connector segments are filled green only when the step to their left has a completed/payment-done state, so the visual progression is always accurate. The rejected branch is unchanged — a red dot still appears at the For Approval step.

- `src/app/portal/requests/[id]/page.tsx` — passes `paymentStatus={cr.paymentStatus}` to `StatusTimeline` to satisfy the updated prop interface.

---

## [0.3.4] — Phase 4: Payments Module — 2026-02-26

### Added

#### Backend

**Entities**

- `Payment.java` — clearance payment entity stored in `payments`. Tracks `residentId`, `clearanceRequestId`, `amount`, `status` (`UNPAID`, `PAID`, `WAIVED`), `paymentMethod` (`STUB`, `CASH`), `idempotencyKey`, `idempotencyExpiresAt`, `initiatedByUserId`, and a `responseBody` TEXT column that stores the raw gateway response for audit. The composite unique index on `(idempotency_key, initiated_by_user_id)` enforces idempotency at the database level.

**Payment Gateway Abstraction**

- `PaymentGateway.java` — Java interface defining a single `charge(PaymentChargeRequest)` method. Designed to be swapped for a real provider (PayMongo, Maya) without touching service logic.

- `StubPaymentGateway.java` — `@Primary` implementation for local development and the MVP. Always returns a successful mock response and sets payment status to `PAID`.

**Repository**

- `PaymentRepository.java` — idempotency lookup: `findByIdempotencyKeyAndInitiatedByUserId(key, userId)`; detects duplicate charge attempts within the expiry window before calling the gateway.

**Service**

- `PaymentService.java` — orchestrates the charge flow: checks idempotency key against existing unexpired payments, delegates to `PaymentGateway`, persists the `Payment` row, and returns the `PaymentDTO`. `ClearanceService.markPaid()` delegates here and promotes clearance `paymentStatus` to `PAID`.

**Mapper**

- `PaymentMapper.java` — MapStruct mapper from `Payment` entity to `PaymentDTO`.

**Controller**

- `PaymentController.java` (`/api/v1/payments`) — `POST /initiate` (CLERK, ADMIN): initiates a charge for a clearance. `idempotencyKey` is a required client-generated UUID in the request body.

**Database**

- `V8__payments_add_columns.sql` — adds `idempotency_expires_at TIMESTAMPTZ` (default `now() + INTERVAL '24 hours'`) and `payment_method VARCHAR(10)` (CHECK: `STUB | CASH`) columns to the `payments` table.

#### Frontend

- `src/types/payment.ts` — populated with `Payment`, `InitiatePaymentPayload`, `PaymentStatus` interfaces.

- Backoffice clearance detail (`/backoffice/clearances/[id]`) — **"Mark as Paid"** button (CLERK/ADMIN) visible when status is `APPROVED` and `paymentStatus` is `UNPAID`. Generates `idempotencyKey` via `crypto.randomUUID()` before submission to prevent duplicate charges on retry.

- Portal request detail (`/portal/requests/[id]`) — **"Pay Now"** button visible to the resident when their request is `APPROVED` and `paymentStatus` is `UNPAID`.

---

## [0.3.3] — Phase 3: Clearance Module — 2026-02-25

### Added

#### Backend

**Entities**

- `ClearanceRequest.java` — the core clearance entity stored in `clearance_requests`. Holds all fields of a request lifecycle: `residentId`, `requestedBy`, `purpose`, `purposeOther` (free-text when purpose is `OTHER`), `urgency`, `feeAmount`, `copies`, `status`, `paymentStatus`, `notes`, `reviewedBy`, `reviewedAt`, `issuedAt`. The `clearanceNumber` is intentionally `null` until the request is released — assigning a number to a request that may still be rejected would waste the sequence slot.

  Enums defined inline:
  - `ClearanceStatus` — `DRAFT`, `FOR_APPROVAL`, `APPROVED`, `REJECTED`, `RELEASED`
  - `ClearancePaymentStatus` — `UNPAID`, `PAID`, `WAIVED`
  - `Purpose` — `EMPLOYMENT`, `TRAVEL_ABROAD`, `SCHOLARSHIP`, `LOAN`, `BUSINESS_PERMIT`, `LEGAL`, `CEDULA`, `OTHER`
  - `Urgency` — `STANDARD`, `RUSH`

- `ClearanceNumberSequence.java` — per-month counter entity (`clearance_number_sequence` table) used by the atomic number generation query.

**Repositories**

- `ClearanceRequestRepository.java` — extends `JpaSpecificationExecutor<ClearanceRequest>` to support the dynamic filter queries in `ClearanceService.list()`. Also provides `findByResidentId`, `findByIdAndResidentId` (ownership-checked single fetch), `countByStatus`, and `countReleasedToday`.
- `ClearanceNumberSequenceRepository.java` — single native query method `nextSequence(yearMonth)` that executes the atomic `INSERT … ON CONFLICT DO UPDATE RETURNING` which guarantees no duplicate clearance numbers under concurrent load.

**Service**

- `ClearanceService.java` — the state machine enforcer. Two access contexts:
  - **Portal (RESIDENT):** `submitPortal`, `listForResident`, `getForResident`, `resubmit` — all resolve the resident from the JWT principal, never from a request parameter, preventing horizontal privilege escalation.
  - **Backoffice (Staff):** `createWalkIn`, `list` (dynamic filter via `SpecificationBuilder`), `getById`, `approve`, `reject`, `release`, `summary`, `markPaid`.

  Key state-machine rules enforced in code:
  - Every transition is guarded by the `guard()` helper — invalid transitions throw `AppException(400)`.
  - `release()` requires both `APPROVED` status **and** `PAID` payment status before proceeding.
  - The clearance number is assigned inside `release()` only, by delegating to `ClearanceNumberService`.
  - Rejection reason is prepended to the `notes` field as `[REJECTED] <reason>` for audit visibility; the original notes are preserved below.

- `ClearanceNumberService.java` — generates atomic, sequential clearance numbers per calendar month in the format `YYYY-MM-NNNN` (e.g. `2025-02-0001`). Uses `Propagation.REQUIRES_NEW` so the sequence increment is committed in its own transaction — this prevents sequence gaps from being reused if the outer transaction rolls back.

- `ClearanceStatusChangedEvent.java` — Spring `ApplicationEvent` published on every status transition, carrying `clearanceId`, `from`, `to`, and `actorId`. Decouples the state machine from downstream concerns (audit logging, notifications) without direct dependencies.

**Mapper**

- `ClearanceMapper.java` — MapStruct mapper from `ClearanceRequest` entity to `ClearanceRequestDTO`. The `residentName` field is not mapped here (it has no counterpart on the entity); it is injected by `ClearanceService.enrich()` via a cross-module `ResidentRepository` lookup.

**Controllers**

- `ClearanceController.java` (`/api/v1/clearances`) — backoffice endpoints:
  - `GET /` — paginated list with optional `status`, `paymentStatus`, `from`, `to` filters; default sort by `createdAt` descending. Roles: `CLERK`, `APPROVER`, `ADMIN`.
  - `POST /` — walk-in request creation by a clerk; `residentId` is required in the body. Roles: `CLERK`, `ADMIN`.
  - `GET /{id}` — single request fetch, no ownership check. Roles: `CLERK`, `APPROVER`, `ADMIN`.
  - `POST /{id}/approve` — `FOR_APPROVAL → APPROVED`. Roles: `APPROVER`, `ADMIN`.
  - `POST /{id}/reject` — `FOR_APPROVAL → REJECTED`; reason required in request body. Roles: `APPROVER`, `ADMIN`.
  - `POST /{id}/release` — `APPROVED + PAID → RELEASED`; assigns clearance number. Roles: `CLERK`, `ADMIN`.
  - `GET /summary` — dashboard counts (`pendingApproval`, `approvedAwaitingPayment`, `releasedToday`). Roles: `CLERK`, `APPROVER`, `ADMIN`.

- `PortalClearanceController.java` (`/api/v1/me/clearances`) — resident portal endpoints. The entire controller is `@PreAuthorize("hasRole('RESIDENT')")` and all resident identity is resolved from the JWT, never from a request parameter:
  - `GET /` — resident's own paginated history.
  - `POST /` — submit a new request; must have `ACTIVE` account status.
  - `GET /{id}` — resident's own single request; `404` if not owned.
  - `PUT /{id}` — resubmit a `REJECTED` request (`REJECTED → FOR_APPROVAL`); ownership verified.

**DTOs**

- `ClearanceRequestDTO.java` — response shape including the denormalised `residentName` (`lastName, firstName`) field. This field is not a stored column — it is resolved via `ClearanceService.enrich()` at query time.
- `CreateClearanceRequest.java` — shared write payload for both portal submission and walk-in creation. Contains `purpose`, `purposeOther`, `urgency`, `copies`, `notes`, and an optional `residentId` (only used for walk-in; portal submissions resolve the resident from the JWT).
- `RejectRequest.java` — single-field DTO carrying the mandatory rejection reason.
- `ClearanceSummaryDTO.java` — dashboard summary with `pendingApproval`, `approvedAwaitingPayment`, `releasedToday` counts.

**Database**

- `V6__clearance_extra_columns.sql` — adds `purpose_other VARCHAR(255)` and `copies INTEGER NOT NULL DEFAULT 1` to `clearance_requests`. These columns were added after the initial schema to support multi-purpose categorisation and multi-copy requests.

#### Frontend

- `src/app/portal/requests/` — resident request list and new-request form pages. The new-request form validates `purpose`/`purposeOther` pairing (the `purposeOther` text field is shown and required only when purpose is `OTHER`) using Zod refinements.

- `src/app/portal/dashboard/` — resident portal dashboard showing active/recent requests.

- `src/app/backoffice/clearances/page.tsx` — staff clearance list with filter controls (`status`, `paymentStatus`, date range). Filters are reflected in URL query params so deep-linking and browser back/forward preserve filter state.

- `src/app/backoffice/clearances/new/` — walk-in request creation form; clerk searches for a resident by name and selects one before completing the request details.

- `src/app/backoffice/clearances/[id]/` — clearance detail page showing status timeline and contextual action buttons depending on the request's current state and the clerk's role.

- `src/components/portal/StatusTimeline.tsx` — visual step-by-step timeline component rendering each clearance status (`FOR_APPROVAL`, `APPROVED`, `REJECTED`, `RELEASED`) with completed/active/pending states. Adapts layout for the rejection branch.

- `src/components/portal/RequestCard.tsx` — summary card used in the portal dashboard list; shows status badge, purpose, date, and a link to the detail page.

- `src/components/backoffice/ClearanceTable.tsx` — paginated staff table listing clearance requests with inline status and payment badges. Sortable columns; row click navigates to detail.

- `src/components/backoffice/ActionButtons.tsx` — context-aware action button group rendered on the backoffice detail page. Renders approve/reject for `FOR_APPROVAL` requests (APPROVER/ADMIN), release for `APPROVED + PAID` requests (CLERK/ADMIN), and a disabled state for terminal statuses. Buttons are hidden per role using the JWT-decoded role from `useAuth()`.

- `src/hooks/useClearances.ts` — TanStack React Query hooks for both access contexts. Uses a hierarchical key factory (`clearanceKeys.all → lists() → list(params) → detail(id) → summary()` for backoffice; `myList(params)` / `myDetail(id)` for portal) so mutations can invalidate precisely without over-clearing. Exposes: `useClearances`, `useClearance`, `useClearanceSummary`, `useCreateWalkIn`, `useApproveClearance`, `useRejectClearance`, `useReleaseClearance` (backoffice); `useMyNewClearanceRequest`, `useResubmitClearance` (portal).

- `src/types/clearance.ts` — TypeScript interfaces fully populated: `ClearanceRequest`, `ClearanceSummary`, `CreateClearancePayload`, `RejectPayload`; enums `ClearanceStatus`, `ClearancePaymentStatus`, `Purpose`, `Urgency`.

---

## [0.3.2] — Refactoring — 2026-02-25

### Changed

#### Backend

**Shared Utility**

- `shared/util/SpecificationBuilder.java` — new generic fluent builder for JPA `Specification` instances. Eliminates the repeated predicate-list boilerplate that was previously duplicated as a private `buildFilter` method in each service. All filter predicates skip themselves automatically when the supplied value is `null`, so callers never need to guard before adding a filter. Supports: `.equal()`, `.greaterThanOrEqualTo()`, `.lessThanOrEqualTo()`, `.like()` (case-insensitive substring). Designed for use by any service that needs optional-filter queries (Clearance, Reports, etc.).

  Usage pattern:

  ```java
  var spec = SpecificationBuilder.<ClearanceRequest>of()
      .equal("status", status)
      .equal("paymentStatus", paymentStatus)
      .greaterThanOrEqualTo("createdAt", from)
      .lessThanOrEqualTo("createdAt", to)
      .build();
  ```

  This also sidesteps the Hibernate 6 type-inference bug with nullable enum parameters that breaks JPQL `:param IS NULL OR col = :param` patterns — the same root cause as the known `lower(bytea)` bug in `ResidentRepository`.

- `ClearanceService.java` — removed private `buildFilter` method and its associated `Predicate`, `Specification`, `ArrayList`, `List` imports; the `list()` method now delegates to `SpecificationBuilder` directly.

---

## [0.3.1] — Documentation — 2026-02-24

### Added

#### Frontend

**Documentation**

- `frontend/docs/system-design-and-architecture.md` — created as the single authoritative reference for frontend architecture decisions so that any developer can understand how the application is structured, why specific patterns were chosen, and how to extend it consistently. Key decisions documented:
  - **Why no global state library (Redux/Zustand):** TanStack React Query owns all server state (API data, caching, loading/error states), and `AuthContext` handles the only true global client state (identity + role). There is no shared mutable client state that would warrant a general-purpose store — adding one would be complexity without benefit.
  - **Why tokens are mirrored to a cookie:** Next.js Edge Middleware runs on the server before the React tree renders and therefore cannot read `localStorage`. The `AuthContext` mirrors the access token to an `accessToken` cookie on login and hydration so the middleware can enforce role-based route guards without a round-trip to the backend.
  - **Why the 401→refresh→retry interceptor uses a subscriber queue:** When multiple concurrent requests all receive a 401 simultaneously (e.g., on first page load with an expired token), a naive implementation would fire N simultaneous refresh calls. The queue holds all failing requests, performs exactly one refresh, then replays them all with the new token.
  - **Routing model:** Route groups (`/portal/*` for residents, `/backoffice/*` for staff) are enforced at the Edge Middleware layer, not just the component layer, so an unauthorized user is redirected before any page bundle is loaded.
  - Layer diagram, annotated directory structure, query key conventions, form validation pattern, and full development guidelines for adding new features.

---

## [0.3.0] — Phase 2: Residents Module — 2026-02-24

### Added

#### Backend

**Entity**

- `Resident.java` — the core record in the resident registry. The `user_id` foreign key to `users` is intentionally nullable: walk-in residents registered by a clerk have no portal account, so there is no user to link. When a resident self-registers via the portal, `AuthService` atomically creates both the `User` and `Resident` rows and links them — this linkage is what drives the pending-review workflow and, after approval, grants the resident access to submit clearance requests.

**Repository**

- `ResidentRepository.java` — supplies the paginated, case-insensitive name search used by the clerk's resident directory. The query matches against both `lastName firstName` and `firstName lastName` orderings so that clerks find the same resident regardless of how they type the name. ⚠️ **Known bug:** when `q` is `null`, Hibernate infers the parameter type as `bytea` instead of `text`, causing `ERROR: function lower(bytea) does not exist` — the `GET /api/v1/residents` endpoint is currently broken for unauthenticated/empty searches. Fix: replace the `? is null` guard with an explicit `CAST(:q AS text) IS NULL` or rewrite using a `Specification`.

**Service**

- `ResidentService.java` — implements the two main resident workflows:
  1. **Staff-managed CRUD** (`create`, `update`, `getById`, `search`) — clerks add and maintain resident records independently of whether those residents have portal accounts.
  2. **Portal activation workflow** (`findPendingUsers`, `activateUser`, `rejectUser`) — when a resident self-registers, their account starts in `PENDING_VERIFICATION`. A clerk reviews the pending list, verifies identity, then either activates the account (allowing the resident to log in and submit requests) or rejects it (blocking access). Activation and rejection are separate from the resident's own `ACTIVE`/`INACTIVE` status.
  - `createFromRegistration(RegisterRequest, User)` is called inside `AuthService.register()`'s `@Transactional` boundary, so a failed resident creation rolls back the user creation too — there are no orphaned `User` rows without a matching `Resident`.

- `ResidentMapper.java` — converts between the `Resident` JPA entity and the `ResidentDTO` returned by the API. The `hasPortalAccount` boolean is computed at mapping time (`resident.getUserId() != null`) rather than stored as a column, keeping the database normalized and eliminating any risk of the flag going out of sync with the actual FK.

**Controller**

- `ResidentController.java` (`/api/v1/residents`) — exposes the resident registry to both staff-facing and system callers:
  - `POST /` — lets a clerk register a resident who presents in person, before or without creating a portal account. Protected to `ADMIN` and `CLERK` roles.
  - `GET /` — powers the resident directory search. Accepts `q` (name/address free-text) and `purok` (subdivision filter) query params with pagination.
  - `GET /{id}` — fetches a single resident's full profile, used by the detail and edit pages.
  - `PATCH /{id}` — partially updates resident details. Only fields present in the request body are applied; absent fields are left unchanged.
  - `GET /pending-users` — returns the clerk's review queue: all residents who completed self-registration and are awaiting identity verification.
  - `PATCH /{id}/activate-user` — approves a pending portal account, allowing the resident to log in.
  - `PATCH /{id}/reject-user` — denies a pending portal account, leaving the resident record intact but preventing login.

**DTOs**

- `ResidentDTO.java` — the API response shape for a resident. Includes the computed `hasPortalAccount` flag so the frontend can conditionally render portal-account actions without a secondary request.
- `CreateResidentRequest.java` — the write payload for walk-in registration. Contains only the fields a clerk supplies; `userId` and status are set by the service, not the caller, preventing mass-assignment of sensitive fields.
- `UpdateResidentRequest.java` — all fields are optional so callers can send only the changed fields. The service applies a null-check before overwriting each field, ensuring a partial request cannot accidentally blank out fields the caller omitted.

#### Frontend

- `src/app/backoffice/residents/page.tsx` — the staff resident directory. The search input is debounced at 300 ms so the API is not called on every keystroke; the query fires only after the user pauses. React Query caches the result so navigating away and back within the 30-second stale window reuses the cached page without a network call.

- `src/app/backoffice/residents/new/page.tsx` — walk-in resident creation form. Zod validates required fields and format rules before submission so common mistakes (missing birth date, invalid contact number format) surface immediately without a round-trip. When the server returns field-level errors from `ErrorResponse.details`, they are mapped back to the specific form fields via `form.setError` rather than shown as a generic banner.

- `src/app/backoffice/residents/[id]/page.tsx` — dual-purpose resident page. For all residents it provides a read/edit view of the profile. For residents whose account is `PENDING_VERIFICATION`, it additionally renders **Activate** and **Reject** action buttons that are hidden for other account states. Button visibility is derived from the `hasPortalAccount` flag and the current clerk's role — approvers cannot edit profiles, only approve or reject.

- `src/components/backoffice/ResidentTable.tsx` — pure presentational table that receives resident data via props. Names are formatted as `Last, First MI.` for scannability in a list. Status and portal account state use color-coded inline badges. The component handles loading (spinner) and empty (explanatory message) states so the parent page does not need conditional rendering logic around it.

- `src/hooks/useResidents.ts` — the data-fetching layer for the residents domain. Uses a hierarchical query key factory (`residentKeys.all → lists() → list(params) → detail(id) → pending()`) so mutations can invalidate exactly the right cache entries. For example, activating a resident invalidates both the detail query and the pending-users list without clearing unrelated resident list queries that are still valid.

- `src/types/resident.ts` — TypeScript interfaces that serve as the single source of type truth for the resident domain. `CreateResidentPayload` and `UpdateResidentPayload` are intentionally separate from the `Resident` read type: write payloads omit `id`, `hasPortalAccount`, `createdAt`, and `updatedAt` so it is impossible at the type level to accidentally send read-only fields in a mutation.

---

## [0.2.1] — Documentation — 2026-02-24

### Added

#### Backend

**Documentation**

- `backend/docs/Security.md` — comprehensive security and authentication reference covering:
  - User model, roles (`ADMIN`, `CLERK`, `APPROVER`, `RESIDENT`), and account statuses
  - Password handling (BCrypt cost-12, change flow, session purge on password change)
  - JWT strategy — HS256 signing key, full access token claims table, JJWT 0.12.x validation
  - Refresh token strategy — opaque UUID, SHA-256 hashing, revocation, expiry, rotation on password change
  - `JwtAuthFilter` step-by-step processing (no DB roundtrip per request)
  - `UserPrincipal` fields, authority mapping, and `@AuthenticationPrincipal` usage guide
  - `SecurityConfig` — public paths, stateless session, custom JSON error handlers, filter order
  - Method-level authorization — `@PreAuthorize` patterns and role-permission matrix
  - Full auth API endpoint reference (register, login, refresh, logout, change-password) with request/response shapes
  - Mermaid sequence diagram covering all six auth flows end-to-end (Registration, Login, Authenticated Request, Token Refresh, Logout, Change Password)
  - Prose process flow walkthrough for each of the six auth flows, describing every step through the application layers
  - Error response catalogue for all security failure scenarios
  - CORS policy, no-auth development profile, configuration reference, and security considerations

---

## [0.2.0] — Phase 1: Identity Module — 2026-02-24

### Added

#### Backend

**Entities**

- `User` entity (`users` table) — email, BCrypt password hash, first/last name, role, status, `must_change_password` flag, timestamps
- `RefreshToken` entity (`refresh_tokens` table) — SHA-256 hashed token, expiry, revocation flag, cascades on user delete

**Repositories**

- `UserRepository` — `findByEmail`, `existsByEmail`
- `RefreshTokenRepository` — `findByTokenHash`, `deleteByUserId`, revocation helpers

**Services**

- `JwtService` — JJWT 0.12.x; issues access tokens (HS256) with `userId`, `email`, `role`, `mustChangePassword` claims; SHA-256 hashes refresh token UUIDs before persistence
- `AuthService` — `register` (RESIDENT role, duplicate email guard), `login` (credential validation + token issuance), `refresh` (opaque token rotation), `logout` (single-token revoke)
- `UserService` — admin staff management: create staff accounts (`ADMIN`, `CLERK`, `APPROVER`), list users (paginated), get by ID, activate/deactivate, force password change

**Security**

- `UserPrincipal` — implements Spring Security `UserDetails`; wraps `User` entity; populates `GrantedAuthority` from role
- `JwtAuthFilter` — extracts `Authorization: Bearer` header; validates token and populates `SecurityContextHolder`; no database hit per request
- `SecurityConfig` — stateless session policy; custom `401` / `403` JSON error handlers; permits public endpoints (`/api/v1/auth/**`, Swagger, Actuator health)

**Controllers**

- `AuthController` (`/api/v1/auth`)
  - `POST /register` — self-registration for residents
  - `POST /login` — returns `accessToken` + `refreshToken` + `mustChangePassword`
  - `POST /refresh` — rotates refresh token, issues new access token
  - `POST /logout` — revokes current refresh token
- `UserController` (`/api/v1/admin/users`) — ADMIN only
  - `POST /` — create staff account
  - `GET /` — list users (paginated, filterable by role/status)
  - `GET /{id}` — get user by ID
  - `PATCH /{id}/status` — activate / deactivate user
  - `POST /{id}/force-password-change` — flag `must_change_password`
  - `PATCH /me/change-password` — authenticated user changes own password

**DTOs**

- `RegisterRequest` — email, password (min 8 chars), first/last name
- `LoginRequest` — email, password
- `RefreshRequest` — refresh token
- `TokenResponse` — access token, optional refresh token, token type, `expiresIn`, `mustChangePassword`
- `CreateStaffRequest` — email, password, first/last name, role
- `ChangePasswordRequest` — current password, new password
- `UserDTO` — safe user projection (no hash exposed)

**Database**

- `V4__expand_user_status.sql` — Altered `users.status` constraint to include `PENDING_VERIFICATION`, `REJECTED`, `DEACTIVATED` in addition to `ACTIVE` / `INACTIVE`

#### Frontend

- `login/page.tsx` — login form with React Hook Form + Zod validation; role-based post-login redirect (`RESIDENT` → `/portal/dashboard`, staff → `/backoffice/dashboard`); `mustChangePassword` redirect to `/change-password`
- `register/page.tsx` — resident self-registration form with React Hook Form + Zod; password confirmation validation; success state with login link
- `AuthContext.tsx` — React context providing `login`, `logout`, `clearAuth`; hydrates auth state from `localStorage` on mount; decodes JWT claims (`userId`, `email`, `role`, `mustChangePassword`) via `jwt-decode`
- `api.ts` — Axios interceptors completed: request interceptor attaches `Authorization: Bearer` from `localStorage`; response interceptor handles `401` → silent refresh → retry with request queuing to prevent concurrent refresh races
- `middleware.ts` — Next.js edge middleware; redirects unauthenticated requests to `/login?next=<path>`; enforces role-based route separation (`/backoffice/**` requires staff role, `/portal/**` requires `RESIDENT`)

### Changed

- `frontend/src/types/auth.ts` — populated with `User`, `AuthTokens`, `TokenResponse`, `LoginRequest`, `RegisterRequest`, `JwtPayload` interfaces; extended `User.status` to include `PENDING_VERIFICATION`, `REJECTED`, `DEACTIVATED`

---

## [0.1.0] — Phase 0: Scaffolding & Infrastructure — 2026-02-24

### Added

#### Backend

**Project scaffold**

- `pom.xml` — Spring Boot 3.3.4 parent; Java 21; dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, PostgreSQL driver, Flyway, JJWT 0.12.6, PDFBox 3.0.3, MapStruct 1.5.5.Final, Lombok 1.18.34, SpringDoc OpenAPI 2.6.0, Testcontainers 1.20.1
- `maven-compiler-plugin` configured with `annotationProcessorPaths`: Lombok → `lombok-mapstruct-binding` → MapStruct → Hibernate JPA metamodel (enforces Lombok-before-MapStruct ordering)
- `BarangayClearanceApplication.java` — Spring Boot entry point

**Configuration**

- `application.yml` — base config: JPA `ddl-auto: validate`, Flyway enabled, server port 8080, multipart limits (5 MB file / 10 MB request), Actuator endpoints, SpringDoc paths
- `application-local.yml` — PostgreSQL on `localhost:5433` (`barangay` / `barangay_dev`), verbose SQL logging, JWT dev secret, 15-min access / 7-day refresh token TTLs
- `application-prod.yml` — externalised via environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, etc.)
- `application-test.yml` — Testcontainers profile; Flyway `clean-on-validation-error: true`

**Database migrations**

- `V1__initial_schema.sql` — 9 tables with UUID primary keys (`gen_random_uuid()`): `users`, `refresh_tokens`, `residents`, `barangay_settings`, `fee_config`, `clearance_number_sequence`, `clearance_requests`, `payments`, `audit_logs`; singleton constraints on `barangay_settings` and `fee_config` (`CHECK (id = 1)`); composite unique index on `payments(idempotency_key, initiated_by_user_id)`
- `V2__seed_settings.sql` — inserts singleton rows for `barangay_settings` and `fee_config` (standard fee ₱50.00, rush fee ₱100.00)
- `V3__seed_admin.sql` — seeds initial admin account (`admin@barangay.local`, BCrypt strength-12 hash, `must_change_password = TRUE`)

**Shared module**

- `AppException.java` — runtime exception carrying HTTP status + message
- `ErrorResponse.java` — standardised error response body (`timestamp`, `status`, `error`, `message`, `path`)
- `GlobalExceptionHandler.java` — `@RestControllerAdvice`; handles `AppException`, `MethodArgumentNotValidException`, `AccessDeniedException`, `AuthenticationException`, and generic `Exception`
- `PageResponse<T>` — generic paginated response wrapper (`content`, `page`, `size`, `totalElements`, `totalPages`, `last`)

#### Infrastructure

- `docker-compose.dev.yml` — PostgreSQL 16 on port 5433; named volume `postgres_dev_data`
- `docker-compose.yml` — full production stack: `postgres`, `backend`, `frontend`, `nginx`; health-checks; named volumes
- `nginx/nginx.conf` — reverse proxy: `/api/` → backend:8080, `/` → frontend:3000; TLS placeholder

#### Frontend

- Next.js 14 project initialised (`create-next-app@14`) with TypeScript, Tailwind CSS, App Router
- `next.config.mjs` — base Next.js config
- `tailwind.config.ts` + `postcss.config.mjs` — Tailwind configured for `src/app/**` and `src/components/**`
- `src/app/layout.tsx` — root layout with Inter font and `AuthProvider` wrapper
- `src/app/providers.tsx` — client-side `AuthProvider` boundary for `AuthContext`
- `src/app/page.tsx` — root redirect shim
- `src/lib/api.ts` — Axios instance configured with `NEXT_PUBLIC_API_URL` base URL (skeleton — interceptors completed in Phase 1)
- `src/types/auth.ts` — placeholder (populated in Phase 1)
- `src/types/clearance.ts` — `ClearanceStatus`, `PaymentStatus`, `Urgency` type aliases; `ClearanceRequest` interface (to be extended in Phase 3)
- `src/types/resident.ts` — `Resident` interface (to be extended in Phase 2)
- `src/types/payment.ts` — payment type stubs (to be extended in Phase 4)
- `src/types/settings.ts` — settings type stubs (to be extended in Phase 6)
- `src/types/common.ts` — shared `PageResponse<T>` TypeScript type

---

[Unreleased]: https://github.com/your-org/barangay-clearance/compare/v0.6.0...HEAD
[0.6.0]: https://github.com/your-org/barangay-clearance/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/your-org/barangay-clearance/compare/v0.4.2...v0.5.0
[0.4.2]: https://github.com/your-org/barangay-clearance/compare/v0.4.1...v0.4.2
[0.4.1]: https://github.com/your-org/barangay-clearance/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/your-org/barangay-clearance/compare/v0.3.4...v0.4.0
[0.3.4]: https://github.com/your-org/barangay-clearance/compare/v0.3.3...v0.3.4
[0.3.3]: https://github.com/your-org/barangay-clearance/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/your-org/barangay-clearance/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/your-org/barangay-clearance/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/your-org/barangay-clearance/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/your-org/barangay-clearance/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/your-org/barangay-clearance/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/your-org/barangay-clearance/releases/tag/v0.1.0
