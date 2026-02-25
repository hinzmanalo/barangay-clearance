# Barangay Clearance System — Project Status

**Last Updated:** 2026-02-26
**Current Phase:** Phase 8 / 11 — Parallel
**Overall Progress:** 8 / 12 phases complete

---

## Phase Overview

| Phase                                   | Name                         | Status         | Week | Notes                            |
| --------------------------------------- | ---------------------------- | -------------- | ---- | -------------------------------- |
| [Phase 0](phase-00-scaffolding.md)      | Scaffolding & Infrastructure | 🟢 Complete    | 1    | Foundation — must complete first |
| [Phase 1](phase-01-auth.md)             | Identity Module: Auth & JWT  | � Complete     | 2    | Blocks all other phases          |
| [Phase 2](phase-02-residents.md)        | Residents Module             | � Complete     | 2–3  | Blocks Phase 3                   |
| [Phase 3](phase-03-clearance.md)        | Clearance Module             | � Complete     | 3–4  | Core business logic              |
| [Phase 4](phase-04-payments.md)         | Payments Module              | � Complete     | 4    | Parallel with Phase 5 & 6        |
| [Phase 5](phase-05-pdf.md)              | PDF Generation               | � Complete     | 5    | Parallel with Phase 4 & 6        |
| [Phase 6](phase-06-settings.md)         | Settings Module              | � Complete     | 5    | Parallel with Phase 4 & 5        |
| [Phase 7](phase-07-reports.md)          | Reports Module               | � Complete     | 6    | Parallel with Phase 8            |
| [Phase 8](phase-08-frontend-polish.md)  | Frontend Polish & Navigation | 🔴 Not Started | 6    | Parallel with Phase 7            |
| [Phase 9](phase-09-testing.md)          | Testing & QA                 | 🔴 Not Started | 7    | Requires all phases complete     |
| [Phase 10](phase-10-deployment.md)      | Deployment                   | 🔴 Not Started | 7–8  | Requires Phase 9                 |
| [Phase 11](phase-11-user-management.md) | User Management              | 🔴 Not Started | 6–7  | Parallel with Phase 7 & 8        |

**Status Legend:**

- 🔴 Not Started
- 🟡 In Progress
- 🟢 Complete
- ⏸️ Blocked

---

## Current Focus

> Update this section when starting a new phase.

**Active phase:** Phase 8 / 11 (parallel)

---

## Dependency Summary

See [dependency-graph.md](dependency-graph.md) for full details.

**Sequential (critical path):**
Phase 0 → Phase 1 → Phase 2 → Phase 3 → [Phase 4/5/6 in parallel] → [Phase 7/8/11 in parallel] → Phase 9 → Phase 10

**Parallel opportunities:**

- After Phase 3: Phases 4, 5, and 6 can all run concurrently
- After Phases 4/5/6: Phases 7, 8, and 11 can run concurrently
- During Phase 9: Phase 10 infrastructure setup can begin

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

**Status:** 🔴 Not Started
**Parallel with:** Phase 7

**Checklist:**

- [ ] `middleware.ts` — complete route guard with `jwt-decode`
- [ ] `AuthContext.tsx` — complete with `localStorage` persistence + re-hydration
- [ ] Backoffice dashboard with summary cards + 30s auto-refresh
- [x] `StatusTimeline.tsx` — all status steps with visual states (payment step added: Unpaid/orange, Paid/green)
- [ ] Error toast system (Radix UI Toast / shadcn/ui)
- [ ] Loading skeletons on all list/detail pages
- [ ] `must_change_password` flow + `/change-password` page
- [ ] Mobile-first Tailwind responsive layout
- [ ] Shared components: `StatusBadge`, `PaymentBadge`, `PageHeader`

---

### Phase 9 — Testing & QA

**Status:** 🔴 Not Started

**Checklist:**

- [ ] `JwtServiceTest` (unit)
- [ ] `AuthServiceTest` (unit)
- [ ] `ClearanceServiceTest` (unit — all state transitions)
- [ ] `ClearanceNumberServiceTest` (unit — concurrency test)
- [ ] `PaymentServiceTest` (unit — idempotency scenarios)
- [ ] `ClearancePdfServiceTest` (unit — `%PDF` magic bytes)
- [ ] `BaseIntegrationTest` with Testcontainers
- [ ] `AuthControllerIT`
- [ ] `ResidentControllerIT`
- [ ] `ClearanceWorkflowIT` (happy path + rejection path)
- [ ] `PaymentControllerIT`
- [ ] `SettingsControllerIT`
- [ ] `SecurityGuardIT`
- [ ] Manual QA: full clearance workflow (5 checklists)
- [ ] `./mvnw test` passes with 0 failures

---

### Phase 10 — Deployment

**Status:** 🔴 Not Started

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

**Status:** 🔴 Not Started
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

## Progress Log

| Date       | Phase    | Action       | Notes                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| ---------- | -------- | ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- | ---------- | ------- | --------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 2026-02-24 | —        | Plan created | All feature plans split from IMPLEMENTATION_PLAN.md                                                                                                                                                                                                                                                                                                                                                                                                         |
| 2026-02-24 | Phase 0  | Completed    | Backend scaffold, Flyway migrations, shared exceptions, Next.js frontend, Docker Compose                                                                                                                                                                                                                                                                                                                                                                    |
| 2026-02-24 | Phase 1  | Completed    | Identity module: JWT auth, refresh tokens, user management, Spring Security config, frontend login/register/auth context                                                                                                                                                                                                                                                                                                                                    |
| 2026-02-24 | Docs     | Added        | `backend/docs/Security.md` — full security reference with Mermaid sequence diagram and prose process flow walkthrough for all six authentication flows                                                                                                                                                                                                                                                                                                      |
| 2026-02-24 | Phase 2  | Completed    | Residents module: `Resident` entity, `ResidentRepository` search, `ResidentService` (CRUD + portal activation workflow), `ResidentMapper` (MapStruct, `hasPortalAccount`), `ResidentController`, frontend list/new/detail pages, `ResidentTable.tsx`, `useResidents.ts` hooks                                                                                                                                                                               |
| 2026-02-24 | Docs     | Added        | `frontend/docs/system-design-and-architecture.md` — frontend architecture reference: routing, auth flow, API client interceptors, state management rationale, component patterns, type system, key user flows                                                                                                                                                                                                                                               |
| 2026-02-24 | Phase 3  | Completed    | Clearance module: `ClearanceRequest`/`ClearanceNumberSequence` entities + enums, atomic number sequence (PostgreSQL `ON CONFLICT RETURNING`), full state machine (`submit → FOR_APPROVAL → APPROVED → RELEASED`, rejection/resubmit), backoffice + portal controllers, `ClearanceStatusChangedEvent`, V6 migration, frontend portal/backoffice pages + `StatusTimeline`, `ClearanceTable`, `ActionButtons`, `RequestCard` components, `useClearances` hooks |
| 2026-02-25 | Shared   | Refactored   | Added `SpecificationBuilder<T>` to `shared/util/` — generic fluent JPA Specification builder; removed duplicated `buildFilter` from `ClearanceService`. Available for `ReportsService` and any future filtered-list service.                                                                                                                                                                                                                                |
| 2026-02-25 | Phase 11 | Planned      | Created `phase-11-user-management.md` — covers backend API gaps (activate, role update, profile update, admin password reset, search/filter, `/me` endpoints) and full frontend backoffice UI (user list, create, detail/edit pages, `UserTable`, `RoleBadge`, sidebar link).                                                                                                                                                                               |
| 2026-02-25 | Phase 8  | Partial      | `StatusTimeline.tsx` enhanced: added Payment step (4th step) between Approved and Released. Orange ring when Unpaid, green check when Paid/Waived, inactive when not yet reached. `portal/requests/[id]/page.tsx` updated to pass `paymentStatus` prop.                                                                                                                                                                                                     |
| 2026-02-26 | Phase 5  | Completed    | PDF Generation: `ClearancePdfService` interface + `ClearancePdfServiceImpl` (PDFBox 3.x, A4 layout, logo embedding, text wrapping, signature block). Endpoints: `GET /clearances/{id}/pdf` (CLERK/ADMIN) + `GET /me/clearances/{id}/pdf` (RESIDENT, RELEASED only). Frontend: Download PDF buttons on portal request detail and backoffice clearance detail pages. Also created `BarangaySettings` entity + repository (prepares Phase 6).                  |     | 2026-02-26 | Phase 6 | Completed | Settings module: `FeeConfig` entity + `FeeConfigRepository`, `BarangaySettingsDTO` + `FeeConfigDTO`, `SettingsService` (get/update settings, logo upload/retrieval, get/update fees), `SettingsController` (GET/PUT `/settings`, POST/GET `/settings/logo`, GET/PUT `/settings/fees`). Logo validated at service level (PNG/JPEG/GIF, max 2 MB). `ClearanceService.resolveFee()` now reads live `fee_config` row (with fallback). Frontend: `useSettings.ts` hooks, `types/settings.ts` updated (`hasLogo`), `/backoffice/admin/settings` profile+logo page, `/backoffice/admin/settings/fees` fee form. |

---

## Blockers & Issues

| Date       | Phase   | Severity | Issue                                                                                                                                                                                                                                                                                                                            | Status  |
| ---------- | ------- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| 2026-02-24 | Phase 2 | High     | **`lower(bytea)` SQL error on resident search** — `GET /api/v1/residents` throws `ERROR: function lower(bytea) does not exist`. Root cause: Hibernate passes `null` query parameters as `bytea` type, breaking the `LOWER(?)` call. Fix: use explicit `CAST` in JPQL or rewrite as a `@Query` with `COALESCE` and proper typing. | 🔴 Open |
