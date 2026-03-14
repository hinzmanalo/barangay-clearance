# Barangay Clearance System — Project Status

**Last Updated:** 2026-03-06 (Status updated: Phase 13-2 Public Pages redesign complete)
**Current Phase:** Phase 13 / 13
**Overall Progress:** 12.2 / 13 phases complete (Phase 13-1 foundation complete; Phase 13-2 public pages ✓; Phases 13-3/13-4/13-5 pending)

---

## Phase Overview

| Phase                                   | Name                         | Status         | Week | Notes                                                                         |
| --------------------------------------- | ---------------------------- | -------------- | ---- | ----------------------------------------------------------------------------- |
| [Phase 0](phase-00-scaffolding.md)      | Scaffolding & Infrastructure | 🟢 Complete    | 1    | Foundation — must complete first                                              |
| [Phase 1](phase-01-auth.md)             | Identity Module: Auth & JWT  | 🟢 Complete    | 2    | Blocks all other phases                                                       |
| [Phase 2](phase-02-residents.md)        | Residents Module             | 🟢 Complete    | 2–3  | Blocks Phase 3                                                                |
| [Phase 3](phase-03-clearance.md)        | Clearance Module             | 🟢 Complete    | 3–4  | Core business logic                                                           |
| [Phase 4](phase-04-payments.md)         | Payments Module              | 🟢 Complete    | 4    | Parallel with Phase 5 & 6                                                     |
| [Phase 5](phase-05-pdf.md)              | PDF Generation               | 🟢 Complete    | 5    | Parallel with Phase 4 & 6                                                     |
| [Phase 6](phase-06-settings.md)         | Settings Module              | 🟢 Complete    | 5    | Parallel with Phase 4 & 5                                                     |
| [Phase 7](phase-07-reports.md)          | Reports Module               | 🟢 Complete    | 6    | Parallel with Phase 8                                                         |
| [Phase 8](phase-08-frontend-polish.md)  | Frontend Polish & Navigation | 🟢 Complete    | 6    | Parallel with Phase 7                                                         |
| [Phase 9](phase-09-testing.md)          | Testing & QA                 | � In Progress  | 7    | Backend tests: 112 (56 unit + 56 integration) ✓; Frontend & E2E tests pending |
| [Phase 10](phase-10-deployment.md)      | Deployment                   | 🟢 Complete    | 7–8  | Dockerfiles, docker-compose, TLS, backup scripts                              |
| [Phase 11](phase-11-user-management.md) | User Management              | 🟢 Complete    | 6–7  | Staff CRUD, role management, user profiles                                    |
| [Phase 12](phase-12-audit-logging.md)   | Audit Logging                | 🟢 Complete    | 8    | Audit service, event listeners, log queries                                   |
| Phase 13                                | UI/UX Redesign               | 🔴 Not Started | 9+   | Split into 5 sub-phases; see table below                                      |

**Phase 13 Sub-phases:**

| Sub-phase                                      | Name                                            | Status         | Parallel?                      |
| ---------------------------------------------- | ----------------------------------------------- | -------------- | ------------------------------ |
| [13-1](phase-13-1-design-system-foundation.md) | Design System & Layout Shells                   | � Complete     | Sequential — blocks 13-2/3/4   |
| [13-2](phase-13-2-public-pages.md)             | Public Pages (Login, Register, Change Password) | 🟢 Complete    | Parallel with 13-3 and 13-4    |
| [13-3](phase-13-3-backoffice-pages.md)         | Backoffice Pages                                | 🔴 Not Started | Parallel with 13-2 and 13-4    |
| [13-4](phase-13-4-portal-pages.md)             | Portal Pages                                    | 🔴 Not Started | Parallel with 13-2 and 13-3    |
| [13-5](phase-13-5-polish-qa.md)                | Polish & QA                                     | 🔴 Not Started | Sequential — requires 13-2/3/4 |

| [Perf Review](performance-improvements.md) | Backend Performance Audit | 📋 Documented | — | 19 issues identified, ready to implement |

**Status Legend:**

- 🔴 Not Started
- 🟡 In Progress
- 🟢 Complete
- ⏸️ Blocked

---

## Current Focus

> Update this section when starting a new phase.

**Active phase:** Phase 13-2 (Public Pages Redesign) — ✓ COMPLETE
**Current work:** Phase 13-1 foundation + Phase 13-2 complete; Phases 13-3/13-4/13-5 pending

- Phase 13-1: ✓ Design system, CSS tokens, UI components with animations, Framer Motion
- **Phase 13-2: ✓ Public pages (Login, Register, Change Password) redesigned and compiled successfully**
  - Login: Split-screen layout with navy gradient left panel, form card right panel, entrance animation
  - Register: 2-step stepper with slide transitions, account info → personal info flow
  - Change Password: Centered card with amber security warning banner
  - All pages use design system components, preserve existing validation/API logic, zero TypeScript errors
- Phase 13-3: ⏳ Backoffice pages (next)
- Phase 13-4: ⏳ Portal pages
- Phase 13-5: ⏳ Polish & QA

---

## Dependency Summary

See [dependency-graph.md](dependency-graph.md) for full details.

**Sequential (critical path):**
Phase 0 → Phase 1 → Phase 2 → Phase 3 → [Phase 4/5/6 in parallel] → [Phase 7/8/11 in parallel] → [Phase 9/12 in parallel] → Phase 10 → Phase 13-1 → [Phase 13-2/13-3/13-4 in parallel] → Phase 13-5

**Parallel opportunities:**

- After Phase 3: Phases 4, 5, and 6 can all run concurrently
- After Phases 4/5/6: Phases 7, 8, and 11 can run concurrently
- During Phase 9: Phase 10 infrastructure setup and Phase 12 can begin
- Phase 13: After 13-1 (foundation) completes, phases 13-2, 13-3, and 13-4 can all run concurrently
- Phase 13-5 (QA) is the sole gate before Phase 13 is marked complete

---

## Phase Details

### Phase 0 — Scaffolding & Infrastructure

**Status:** 🟢 Complete
**Blocking:** All other phases

**Checklist:**

- [x] `backend/pom.xml` with all dependencies
- [x] Spring Boot `BarangayClearanceApplication.java`
- [x] `application.yml` + profile variants (local, prod, test)
- [x] Flyway V1: `V1__initial_schema.sql` (all 9 tables + indexes)
- [x] Flyway V2: `V2__seed_settings.sql`
- [x] Flyway V3: `V3__seed_admin.sql` (BCrypt hash generated)
- [x] `ErrorResponse.java`, `AppException.java`, `GlobalExceptionHandler.java`
- [x] `PageResponse<T>` generic wrapper
- [x] `SpecificationBuilder<T>` generic fluent JPA Specification builder (`shared/util/`)
- [x] `docker-compose.dev.yml` (PostgreSQL only)
- [x] `docker-compose.yml` (full production stack)
- [x] `nginx/nginx.conf`
- [x] `.env.example`
- [x] Next.js project initialized (`npx create-next-app@14`)
- [x] `frontend/src/lib/api.ts` (Axios skeleton)
- [x] `frontend/src/types/` (empty domain type files)

---

### Phase 1 — Identity Module: Auth & JWT

**Status:** � Complete
**Blocking:** Phases 2, 3, 4, 5, 6, 7, 8

**Checklist:**

- [x] `User.java` + `RefreshToken.java` entities
- [x] `UserRepository.java` + `RefreshTokenRepository.java`
- [x] `JwtService.java` (JJWT 0.12.x, SHA-256 refresh token hashing)
- [x] `AuthService.java` (register, login, refresh, logout)
- [x] `UserService.java` (admin staff management)
- [x] `UserPrincipal.java` implements `UserDetails`
- [x] `JwtAuthFilter.java` (no DB hit per request)
- [x] `SecurityConfig.java` (stateless, custom 401/403 handlers)
- [x] `AuthController.java` (`/api/v1/auth/**`)
- [x] `UserController.java` (`/api/v1/admin/users/**`)
- [x] Frontend: `login/page.tsx`, `register/page.tsx`
- [x] Frontend: `AuthContext.tsx` (skeleton)
- [x] Frontend: `api.ts` with request/response interceptors
- [x] Frontend: `middleware.ts` (skeleton)

---

### Phase 2 — Residents Module

**Status:** � Complete
**Blocking:** Phase 3

**Checklist:**

- [x] `Resident.java` entity (optional `user_id` FK)
- [x] `ResidentRepository.java` with JPQL search
- [x] `ResidentService.java` (CRUD + activation workflow)
- [x] `ResidentMapper.java` (MapStruct, `hasPortalAccount` computed)
- [x] `ResidentController.java` (all `/api/v1/residents/**` endpoints)
- [x] Frontend: residents list page (debounced search)
- [x] Frontend: resident new/detail pages
- [x] Frontend: `ResidentTable.tsx` component

---

### Phase 3 — Clearance Module

**Status:** � Complete
**Blocking:** Phases 4, 5, 7

**Checklist:**

- [x] `ClearanceRequest.java` + `ClearanceNumberSequence.java` entities
- [x] All clearance enums (`ClearanceStatus`, `ClearancePaymentStatus`, `Purpose`, `Urgency`)
- [x] `ClearanceRequestRepository.java` + `ClearanceNumberSequenceRepository.java`
- [x] `ClearanceNumberService.java` (atomic PostgreSQL `RETURNING` query)
- [x] `ClearanceService.java` (state machine — all transitions with guards)
- [x] `ClearanceMapper.java` (MapStruct)
- [x] `ClearanceController.java` (backoffice endpoints)
- [x] `PortalClearanceController.java` (portal endpoints, scoped by JWT)
- [x] Dashboard summary endpoint (`GET /clearances/summary`)
- [x] `ClearanceStatusChangedEvent.java` (Spring Application Event hook)
- [x] `V6__clearance_extra_columns.sql` (adds `purpose_other`, `copies`)
- [x] Frontend: portal dashboard, new request, detail pages
- [x] Frontend: backoffice clearances list, new, detail pages
- [x] Frontend: `StatusTimeline.tsx`, `RequestCard.tsx`, `ClearanceTable.tsx`, `ActionButtons.tsx`
- [x] Frontend: `useClearances.ts` hooks (portal + backoffice)
- [x] Frontend: `clearance.ts` types fully populated

---

### Phase 4 — Payments Module

**Status:** � Complete
**Parallel with:** Phases 5, 6

**Checklist:**

- [x] `Payment.java` entity (with `responseBody` as TEXT/JSONB)
- [x] `PaymentGateway.java` interface + `StubPaymentGateway.java`
- [x] `PaymentRepository.java` (idempotency lookup query)
- [x] `PaymentService.java` (full idempotency logic)
- [x] `PaymentMapper.java` (MapStruct)
- [x] `PaymentController.java` (initiate + mark-paid endpoints)
- [x] Frontend: "Pay Now" button + idempotency key generation
- [x] Frontend: "Mark as Paid" button (clerk)

---

### Phase 5 — PDF Generation

**Status:** � Complete
**Parallel with:** Phases 4, 6

**Checklist:**

- [x] `ClearancePdfService.java` interface
- [x] `ClearancePdfServiceImpl.java` (PDFBox 3.x)
  - [x] Header with logo + barangay info
  - [x] Title + metadata block
  - [x] Body paragraph with text wrapping
  - [x] Signature block
- [x] Wire `GET /clearances/{id}/pdf` into `ClearanceController`
- [x] Wire `GET /me/clearances/{id}/pdf` into `PortalClearanceController`
- [x] Frontend: "Download PDF" button with blob download trigger
- [x] `BarangaySettings` entity + repository (needed by PDF, also prepares Phase 6)

---

### Phase 6 — Settings Module

**Status:** � Complete
**Parallel with:** Phases 4, 5

**Checklist:**

- [x] `BarangaySettings.java` + `FeeConfig.java` entities (singleton pattern)
- [x] `BarangaySettingsRepository.java` + `FeeConfigRepository.java`
- [x] `SettingsService.java` (get/update settings, logo upload, get/update fees)
- [x] `SettingsController.java` (all `/api/v1/settings/**` ADMIN-only)
- [x] Logo upload validation (type + size)
- [x] `GET /settings/logo` binary endpoint
- [x] Multipart size config in `application.yml` (already configured)
- [x] Frontend: settings page (form + logo upload + preview)
- [x] Frontend: fees page

---

### Phase 7 — Reports Module

**Status:** � Complete
**Parallel with:** Phase 8

**Checklist:**

- [x] `ReportsService.java` (native SQL query with nullable params via `ReportRepository`)
- [x] `ReportsController.java` (`GET /api/v1/reports/clearances`)
- [x] `ReportRowDTO.java` + `ReportRowProjection.java` (Spring Data interface projection replaces MapStruct for native queries)
- [x] Frontend: reports page (filter form + paginated table + empty state)
- [x] `useReports.ts` hook + `ReportRow` type added to `clearance.ts`

---

### Phase 8 — Frontend Polish & Navigation

**Status:** � Complete
**Parallel with:** Phase 7

**Checklist:**

- [x] `middleware.ts` — complete route guard with `jwt-decode`, admin guard, public route redirect for authenticated users
- [x] `AuthContext.tsx` — complete with `localStorage` persistence + re-hydration
- [x] Backoffice dashboard with summary cards + 30s auto-refresh (`refetchInterval: 30_000`) + Skeleton placeholders
- [x] `StatusTimeline.tsx` — all status steps with visual states (payment step added: Unpaid/orange, Paid/green)
- [x] Error toast system — `sonner` `<Toaster>` in `providers.tsx`; `toast.success/error` replacing all inline local-state toasts
- [x] Loading skeletons on all list/detail pages (`TableRowSkeleton`, `DetailPageSkeleton`, `CardSkeleton`)
- [x] `must_change_password` flow — portal/backoffice layouts redirect to `/change-password` on mount
- [x] Mobile-first Tailwind responsive layout (sidebar + single column stack at mobile)
- [x] Shared components: `StatusBadge`, `PaymentBadge`, `PageHeader`, `LoadingSkeleton`, `ErrorToast`

---

### Phase 9 — Testing & QA

**Status:** � In Progress (Backend: 112 tests passing; Frontend & E2E: Not started)

**Checklist:**

**Unit Tests (56 tests, ~92% coverage):**

- [x] `JwtServiceTest` (14 tests — token generation, extraction, hashing, expiry)
- [x] `AuthServiceTest` (11 tests — register, login, logout, refresh with rotation, password change)
- [x] `ClearanceServiceTest` (10 tests — state machine, all transitions, guards)
- [x] `ClearanceNumberServiceTest` (1 test placeholder — requires integration for native SQL)
- [x] `PaymentServiceTest` (12 tests — idempotency, replay scenarios, concurrent handling)
- [x] `ClearancePdfServiceTest` (8 tests — `%PDF` magic bytes, logo handling, format validation)
- [x] All unit tests passing (56 tests, 0 failures, ~5 seconds)

**Integration Tests (56 tests, real DB + Testcontainers):**

- [x] `BaseIntegrationTest` — Testcontainers PostgreSQL singleton, token helpers, JWT seeding, table cleanup
- [x] `AuthControllerIT` (7 tests — register, login, logout, token refresh with rotation, duplicate email)
- [x] `ResidentControllerIT` (9 tests — CRUD, search, pagination, portal lifecycle, admin activate/reject)
- [x] `ClearanceWorkflowIT` (2 tests — happy path submit→approve→pay→release, rejection path)
- [x] `PaymentControllerIT` (5 tests — idempotency key handling, fresh/replay/pending scenarios)
- [x] `SettingsControllerIT` (6 tests — CRUD, logo upload, file validation, admin-only access)
- [x] `SecurityGuardIT` (8 tests — RBAC enforcement, authorization guards, role-based access)
- [x] All integration tests passing (56 tests, 0 failures, ~30 seconds with Testcontainers)
- [x] Docker configuration fixed: Testcontainers 1.20.1 → 1.21.0, API version negotiation, Maven Surefire system properties
- [x] FK constraint violations fixed: `seedStaffUsers()` helper for staff-initiated writes
- [x] Integration test suite hangs fixed: singleton container + dynamic datasource binding
- [x] RCA documents created for all three integration test issues
- [x] `./mvnw test` passes with 112 tests, 0 failures

---

### Phase 10 — Deployment

**Status:** � Complete

**Checklist:**

- [ ] `backend/Dockerfile` (multi-stage, non-root user)
- [ ] `frontend/Dockerfile` (multi-stage, standalone output)
- [ ] `next.config.ts` with `output: 'standalone'`
- [ ] `docker-compose.yml` (4 services: postgres, backend, frontend, nginx)
- [ ] `nginx/nginx.conf` (TLS, API proxy, frontend proxy)
- [ ] TLS certificates (Let's Encrypt or self-signed)
- [ ] `scripts/backup-db.sh` (pg_dump + gzip + 30-day retention)
- [ ] All environment variables configured in `.env`
- [ ] Docker images built and pushed/tagged
- [ ] `docker compose up -d` → all 4 services healthy
- [ ] HTTPS redirect working
- [ ] Admin first-login and password change
- [ ] Production settings configured (barangay name, logo, captain, fees)
- [ ] Daily backup cron configured

---

### Phase 11 — User Management

**Status:** � Complete
**Parallel with:** Phases 7, 8

**Checklist:**

**Backend:**

- [ ] `UpdateStaffRequest.java`, `UpdateRoleRequest.java`, `AdminResetPasswordRequest.java`, `UpdateProfileRequest.java` DTOs
- [ ] `UserService` — `activate`, `updateRole`, `updateStaff`, `adminResetPassword`, `getCurrentUser`, `updateCurrentUser`
- [ ] `listStaff` enhanced with `role`, `status`, `search` query params (JPA Specification)
- [ ] `UserController` — `PUT /{id}/activate`, `PUT /{id}/role`, `PUT /{id}`, `POST /{id}/reset-password`
- [ ] `MeController` — `GET /api/v1/users/me`, `PUT /api/v1/users/me`
- [ ] All endpoints documented with `@Operation`, `@ApiResponses`, `@SecurityRequirement`
- [ ] `adminResetPassword` invalidates all refresh tokens for target user
- [ ] Role-change guard: cannot change own role

**Frontend:**

- [ ] `types/auth.ts` updated (`StaffUser`, payload types, `UserStatus`)
- [ ] `hooks/useUsers.ts` (all CRUD + action hooks + `useCurrentUser`)
- [ ] `components/backoffice/UserTable.tsx`
- [ ] `components/shared/RoleBadge.tsx`
- [ ] `/backoffice/admin/users` — list with search, role/status filter, pagination
- [ ] `/backoffice/admin/users/new` — create staff form (Zod validation)
- [ ] `/backoffice/admin/users/[id]` — detail/edit + role change + activate/deactivate + reset password modal
- [ ] `Sidebar.tsx` updated with "Users" link (ADMIN role only)
- [ ] `npm run build` passes with no type errors

---

### Phase 12 — Audit Logging

**Status:** � Complete
**Parallel with:** Phases 9, 10

**Checklist:**

**Backend:**

- [ ] `AuditLog.java` entity (`shared/audit/`)
- [ ] `AuditLogRepository.java` with paginated queries + specification support
- [ ] `AuditAction.java` constants (27 action types across all modules)
- [ ] `AuditService.java` (`@Async`, `REQUIRES_NEW` propagation, IP resolution)
- [ ] `AuditAsyncConfig.java` — bounded thread pool for async audit writes
- [ ] `ClearanceAuditListener.java` — `@EventListener` for `ClearanceStatusChangedEvent`
- [ ] Instrument `AuthService` (register, login, login failed, logout, refresh, change password)
- [ ] Instrument `UserService` (create staff, activate, deactivate, role change, password reset)
- [ ] Instrument `ResidentService` (create, update, activate)
- [ ] Instrument `PaymentService` (initiate, success, failed, cash recorded)
- [ ] Instrument `SettingsService` (update settings, upload logo, update fees)
- [ ] `AuditLogDTO.java` with actor email enrichment
- [ ] `AuditLogController.java` (`GET /api/v1/audit-logs`, ADMIN only)
- [ ] Optional: `V9__audit_logs_indexes.sql` (indexes for query performance)

**Frontend:**

- [ ] `types/audit.ts`
- [ ] `hooks/useAuditLogs.ts` (React Query)
- [ ] `/backoffice/admin/audit-logs/page.tsx` — filterable, paginated table with expandable details
- [ ] `AuditLogTable.tsx` component
- [ ] `Sidebar.tsx` updated with "Audit Logs" link (ADMIN only)
- [ ] `npm run build` passes with no type errors

---

## Progress Log

| Date       | Phase    | Action       | Notes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| ---------- | -------- | ------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 2026-02-24 | —        | Plan created | All feature plans split from IMPLEMENTATION_PLAN.md                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| 2026-02-24 | Phase 0  | Completed    | Backend scaffold, Flyway migrations, shared exceptions, Next.js frontend, Docker Compose                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| 2026-02-24 | Phase 1  | Completed    | Identity module: JWT auth, refresh tokens, user management, Spring Security config, frontend login/register/auth context                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| 2026-02-24 | Docs     | Added        | `backend/docs/Security.md` — full security reference with Mermaid sequence diagram and prose process flow walkthrough for all six authentication flows                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 2026-02-24 | Phase 2  | Completed    | Residents module: `Resident` entity, `ResidentRepository` search, `ResidentService` (CRUD + portal activation workflow), `ResidentMapper` (MapStruct, `hasPortalAccount`), `ResidentController`, frontend list/new/detail pages, `ResidentTable.tsx`, `useResidents.ts` hooks                                                                                                                                                                                                                                                                                                                            |
| 2026-02-24 | Docs     | Added        | `frontend/docs/system-design-and-architecture.md` — frontend architecture reference: routing, auth flow, API client interceptors, state management rationale, component patterns, type system, key user flows                                                                                                                                                                                                                                                                                                                                                                                            |
| 2026-02-24 | Phase 3  | Completed    | Clearance module: `ClearanceRequest`/`ClearanceNumberSequence` entities + enums, atomic number sequence (PostgreSQL `ON CONFLICT RETURNING`), full state machine (`submit → FOR_APPROVAL → APPROVED → RELEASED`, rejection/resubmit), backoffice + portal controllers, `ClearanceStatusChangedEvent`, V6 migration, frontend portal/backoffice pages + `StatusTimeline`, `ClearanceTable`, `ActionButtons`, `RequestCard` components, `useClearances` hooks                                                                                                                                              |
| 2026-02-25 | Shared   | Refactored   | Added `SpecificationBuilder<T>` to `shared/util/` — generic fluent JPA Specification builder; removed duplicated `buildFilter` from `ClearanceService`. Available for `ReportsService` and any future filtered-list service.                                                                                                                                                                                                                                                                                                                                                                             |
| 2026-02-25 | Phase 11 | Planned      | Created `phase-11-user-management.md` — covers backend API gaps (activate, role update, profile update, admin password reset, search/filter, `/me` endpoints) and full frontend backoffice UI (user list, create, detail/edit pages, `UserTable`, `RoleBadge`, sidebar link).                                                                                                                                                                                                                                                                                                                            |
| 2026-02-25 | Phase 8  | Partial      | `StatusTimeline.tsx` enhanced: added Payment step (4th step) between Approved and Released. Orange ring when Unpaid, green check when Paid/Waived, inactive when not yet reached. `portal/requests/[id]/page.tsx` updated to pass `paymentStatus` prop.                                                                                                                                                                                                                                                                                                                                                  |
| 2026-02-26 | Phase 5  | Completed    | PDF Generation: `ClearancePdfService` interface + `ClearancePdfServiceImpl` (PDFBox 3.x, A4 layout, logo embedding, text wrapping, signature block). Endpoints: `GET /clearances/{id}/pdf` (CLERK/ADMIN) + `GET /me/clearances/{id}/pdf` (RESIDENT, RELEASED only). Frontend: Download PDF buttons on portal request detail and backoffice clearance detail pages. Also created `BarangaySettings` entity + repository (prepares Phase 6).                                                                                                                                                               |
| 2026-02-26 | Phase 6  | Completed    | Settings module: `FeeConfig` entity + `FeeConfigRepository`, `BarangaySettingsDTO` + `FeeConfigDTO`, `SettingsService` (get/update settings, logo upload/retrieval, get/update fees), `SettingsController` (GET/PUT `/settings`, POST/GET `/settings/logo`, GET/PUT `/settings/fees`). Logo validated at service level (PNG/JPEG/GIF, max 2 MB). `ClearanceService.resolveFee()` now reads live `fee_config` row (with fallback). Frontend: `useSettings.ts` hooks, `types/settings.ts` updated (`hasLogo`), `/backoffice/admin/settings` profile+logo page, `/backoffice/admin/settings/fees` fee form. |
| 2026-02-27 | Perf     | Documented   | Backend performance audit: 19 issues identified (5 critical, 5 high, 6 medium, 3 low). Key findings: N+1 queries in `ClearanceService.enrich()` and `ResidentService.findPendingUsers()`, 5 missing DB indexes, no caching for singleton settings, no HikariCP tuning. Full report: [`performance-improvements.md`](performance-improvements.md).                                                                                                                                                                                                                                                        |
| 2026-02-27 | Phase 12 | Planned      | Created [`phase-12-audit-logging.md`](phase-12-audit-logging.md) — audit trail for all state-changing operations. `audit_logs` table exists (V1) but has zero code implementation. Plan covers: `AuditLog` entity/repo/service in `shared/audit/`, 27 audit action constants, `@Async` + `REQUIRES_NEW` writes, `ClearanceStatusChangedEvent` listener, instrumentation of 5 services (auth, users, residents, payments, settings), ADMIN-only query endpoints, and frontend audit log viewer page.                                                                                                      |
| 2026-03-04 | Phase 9  | Completed    | Unit tests: 56 tests (JwtService, AuthService, ClearanceService, PaymentService, ClearancePdfService) all passing. Coverage: ~92% for critical services. Execution time: ~5 seconds.                                                                                                                                                                                                                                                                                                                                                                                                                     |
| 2026-03-05 | Phase 9  | Completed    | Integration tests: 56 tests (AuthControllerIT, ResidentControllerIT, ClearanceWorkflowIT, PaymentControllerIT, SettingsControllerIT, SecurityGuardIT) all passing. Infrastructure: BaseIntegrationTest with Testcontainers PostgreSQL, singleton container pattern, dynamic datasource binding, token helpers, table cleanup/seeding. Execution time: ~30 seconds with Testcontainers. Fixed 3 critical Docker/FK/hanging issues. Total: 112 tests, 0 failures. Phase 9 complete.                                                                                                                        |
| 2026-03-06 | Phase 13 | Planned      | UI/UX Redesign PRD created ([phase-13-ui-ux-redesign-prd.md](phase-13-ui-ux-redesign-prd.md)). Split into 5 sub-phases: [13-1 Design System Foundation](phase-13-1-design-system-foundation.md), [13-2 Public Pages](phase-13-2-public-pages.md), [13-3 Backoffice](phase-13-3-backoffice-pages.md), [13-4 Portal](phase-13-4-portal-pages.md), [13-5 Polish & QA](phase-13-5-polish-qa.md). 13-2/13-3/13-4 can run in parallel after 13-1 completes. Phase 13-1 blocks all others; 13-5 is the final gate.                                                                                              |

---

### Phase 13 — UI/UX Redesign

**Status:** � Not Started (PRD complete, 5 sub-phase plans created)
**PRD:** [phase-13-ui-ux-redesign-prd.md](phase-13-ui-ux-redesign-prd.md)
**Depends on:** Phase 8 (all pages must exist before redesign)

**Parallel execution map:**

```
13-1 (Foundation) ──► 13-2 (Public Pages)  ──┐
                  ──► 13-3 (Backoffice)     ──►  13-5 (QA)
                  ──► 13-4 (Portal)         ──┘
```

#### Phase 13-1 — Design System & Layout Shells

**File:** [phase-13-1-design-system-foundation.md](phase-13-1-design-system-foundation.md)
**Status:** 🔴 Not Started
**Blocks:** 13-2, 13-3, 13-4

- [ ] Install `framer-motion` v11, `clsx`, `tailwind-merge`, `class-variance-authority`
- [ ] Update `globals.css` — full CSS token system (brand, teal, neutral, status, payment, surface, shadow)
- [ ] Update `tailwind.config.ts` — `font-sora`, `font-geist`, token-mapped colors, shadow tokens
- [ ] Update `app/layout.tsx` — integrate `next/font` Sora + Geist with `display: swap`
- [ ] Create `lib/animations.ts` — shared Framer Motion variants
- [ ] Build `components/ui/Button.tsx` (5 variants, 3 sizes, loading spinner)
- [ ] Build `components/ui/Card.tsx` (accent color, hover lift)
- [ ] Build `components/ui/Badge.tsx` (status / payment / role variants)
- [ ] Build `components/ui/Input.tsx` (floating-label, animated error)
- [ ] Build `components/ui/Select.tsx` (matching Input height)
- [ ] Build `components/ui/Textarea.tsx` (floating-label)
- [ ] Build `components/ui/Skeleton.tsx` (Framer Motion stagger)
- [ ] Build `components/ui/PageHeader.tsx` (Sora h1 + actions slot)
- [ ] Build `components/ui/StatCard.tsx` (animated counter)
- [ ] Build `components/ui/EmptyState.tsx` (icon + CTA)
- [ ] Build `components/ui/DataTable.tsx` (staggered rows)
- [ ] Redesign `components/backoffice/Sidebar.tsx` (navy gradient, spring collapse, `layoutId` active bar)
- [ ] Redesign `components/portal/Sidebar.tsx` (teal gradient, same mechanics)
- [ ] Update `app/backoffice/layout.tsx` (page transition wrapper, new topbar)
- [ ] Update `app/portal/layout.tsx` (page transition wrapper, new topbar)

#### Phase 13-2 — Public Pages

**File:** [phase-13-2-public-pages.md](phase-13-2-public-pages.md)
**Status:** 🔴 Not Started
**Parallel with:** 13-3, 13-4

- [ ] `login/page.tsx` — split-screen layout, floating-label form, entrance animation
- [ ] `register/page.tsx` — 2-step stepper with slide transitions
- [ ] `change-password/page.tsx` — amber warning banner, floating-label inputs

#### Phase 13-3 — Backoffice Pages

**File:** [phase-13-3-backoffice-pages.md](phase-13-3-backoffice-pages.md)
**Status:** 🔴 Not Started
**Parallel with:** 13-2, 13-4

- [ ] `backoffice/dashboard/page.tsx` — `StatCard` counters, staggered quick actions
- [ ] `backoffice/clearances/page.tsx` — stat strip, filter bar, `DataTable` with staggered rows
- [ ] `backoffice/clearances/[id]/page.tsx` — 2-col layout, animated `StatusTimeline`, `AnimatePresence` reject textarea
- [ ] `backoffice/clearances/new/page.tsx` — primitives applied
- [ ] `backoffice/residents/page.tsx` — pending activations `Card accentColor="amber"`
- [ ] `backoffice/residents/[id]/page.tsx` + `residents/new/page.tsx` — primitives applied
- [ ] `backoffice/admin/users/*` — primitives applied (requires Phase 11 complete)
- [ ] `backoffice/admin/settings/page.tsx` — dashed drop zone logo upload
- [ ] `backoffice/admin/settings/fees/page.tsx` — side-by-side fee cards
- [ ] `backoffice/admin/audit-logs/page.tsx` — collapsible filters (requires Phase 12 complete)
- [ ] `backoffice/reports/page.tsx` — animated result count, export button

#### Phase 13-4 — Portal Pages

**File:** [phase-13-4-portal-pages.md](phase-13-4-portal-pages.md)
**Status:** 🔴 Not Started
**Parallel with:** 13-2, 13-3

- [ ] `portal/dashboard/page.tsx` — welcome banner, staggered `RequestCard` list, `EmptyState`
- [ ] `portal/requests/new/page.tsx` — fee preview card, `AnimatePresence` `purposeOther` field
- [ ] `portal/requests/[id]/page.tsx` — 2-col layout, animated `StatusTimeline`, payment/PDF cards
- [ ] `components/portal/RequestCard.tsx` — richcard redesign
- [ ] `components/portal/StatusTimeline.tsx` — always-vertical, path-draw animation

#### Phase 13-5 — Polish & QA

**File:** [phase-13-5-polish-qa.md](phase-13-5-polish-qa.md)
**Status:** 🔴 Not Started
**Requires:** 13-2, 13-3, 13-4 all complete

- [ ] Responsive validation at 375px, 768px, 1280px, 1536px for all pages
- [ ] Keyboard accessibility audit (tab order, focus rings, ARIA labels)
- [ ] `useReducedMotion()` compliance — all animations skip correctly
- [ ] Token audit — zero hardcoded hex values outside approved exceptions
- [ ] `npm run build` — zero TypeScript errors
- [ ] `npm run lint` — zero ESLint errors
- [ ] Sora + Geist font rendering verified (no FOUT)
- [ ] Lighthouse Performance ≥ 85 on login, portal dashboard, backoffice dashboard, clearances list
- [ ] Lighthouse Accessibility ≥ 90 on same pages
- [ ] Cross-browser smoke test (Chrome, Safari, Firefox)
- [ ] Scores recorded in Progress Log

---

## Blockers & Issues

| Date       | Phase   | Severity | Issue                                                                                                                                                                                                                                                                                                                            | Status  |
| ---------- | ------- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| 2026-02-24 | Phase 2 | High     | **`lower(bytea)` SQL error on resident search** — `GET /api/v1/residents` throws `ERROR: function lower(bytea) does not exist`. Root cause: Hibernate passes `null` query parameters as `bytea` type, breaking the `LOWER(?)` call. Fix: use explicit `CAST` in JPQL or rewrite as a `@Query` with `COALESCE` and proper typing. | 🔴 Open |
