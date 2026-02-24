# Barangay Clearance System — Project Status

**Last Updated:** 2026-02-24
**Current Phase:** Phase 2 — Residents Module
**Overall Progress:** 2 / 11 phases complete

---

## Phase Overview

| Phase                                  | Name                         | Status         | Week | Notes                            |
| -------------------------------------- | ---------------------------- | -------------- | ---- | -------------------------------- |
| [Phase 0](phase-00-scaffolding.md)     | Scaffolding & Infrastructure | 🟢 Complete    | 1    | Foundation — must complete first |
| [Phase 1](phase-01-auth.md)            | Identity Module: Auth & JWT  | � Complete     | 2    | Blocks all other phases          |
| [Phase 2](phase-02-residents.md)       | Residents Module             | 🔴 Not Started | 2–3  | Blocks Phase 3                   |
| [Phase 3](phase-03-clearance.md)       | Clearance Module             | 🔴 Not Started | 3–4  | Core business logic              |
| [Phase 4](phase-04-payments.md)        | Payments Module              | 🔴 Not Started | 4    | Parallel with Phase 5 & 6        |
| [Phase 5](phase-05-pdf.md)             | PDF Generation               | 🔴 Not Started | 5    | Parallel with Phase 4 & 6        |
| [Phase 6](phase-06-settings.md)        | Settings Module              | 🔴 Not Started | 5    | Parallel with Phase 4 & 5        |
| [Phase 7](phase-07-reports.md)         | Reports Module               | 🔴 Not Started | 6    | Parallel with Phase 8            |
| [Phase 8](phase-08-frontend-polish.md) | Frontend Polish & Navigation | 🔴 Not Started | 6    | Parallel with Phase 7            |
| [Phase 9](phase-09-testing.md)         | Testing & QA                 | 🔴 Not Started | 7    | Requires all phases complete     |
| [Phase 10](phase-10-deployment.md)     | Deployment                   | 🔴 Not Started | 7–8  | Requires Phase 9                 |

**Status Legend:**

- 🔴 Not Started
- 🟡 In Progress
- 🟢 Complete
- ⏸️ Blocked

---

## Current Focus

> Update this section when starting a new phase.

**Active phase:** Phase 2 — Residents Module

---

## Dependency Summary

See [dependency-graph.md](dependency-graph.md) for full details.

**Sequential (critical path):**
Phase 0 → Phase 1 → Phase 2 → Phase 3 → [Phase 4/5/6 in parallel] → [Phase 7/8 in parallel] → Phase 9 → Phase 10

**Parallel opportunities:**

- After Phase 3: Phases 4, 5, and 6 can all run concurrently
- After Phases 4/5/6: Phases 7 and 8 can run concurrently
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

**Status:** 🔴 Not Started
**Blocking:** Phase 3

**Checklist:**

- [ ] `Resident.java` entity (optional `user_id` FK)
- [ ] `ResidentRepository.java` with JPQL search
- [ ] `ResidentService.java` (CRUD + activation workflow)
- [ ] `ResidentMapper.java` (MapStruct, `hasPortalAccount` computed)
- [ ] `ResidentController.java` (all `/api/v1/residents/**` endpoints)
- [ ] Frontend: residents list page (debounced search)
- [ ] Frontend: resident new/detail pages
- [ ] Frontend: `ResidentTable.tsx` component

---

### Phase 3 — Clearance Module

**Status:** 🔴 Not Started
**Blocking:** Phases 4, 5, 7

**Checklist:**

- [ ] `ClearanceRequest.java` + `ClearanceNumberSequence.java` entities
- [ ] All clearance enums (`ClearanceStatus`, `ClearancePaymentStatus`, `Purpose`, `Urgency`)
- [ ] `ClearanceRequestRepository.java` + `ClearanceNumberSequenceRepository.java`
- [ ] `ClearanceNumberService.java` (atomic PostgreSQL `RETURNING` query)
- [ ] `ClearanceService.java` (state machine — all transitions with guards)
- [ ] `ClearanceMapper.java` (MapStruct)
- [ ] `ClearanceController.java` (backoffice endpoints)
- [ ] `PortalClearanceController.java` (portal endpoints, scoped by JWT)
- [ ] Dashboard summary endpoint (`GET /clearances/summary`)
- [ ] Frontend: portal dashboard, new request, detail pages
- [ ] Frontend: backoffice clearances list, new, detail pages
- [ ] Frontend: `StatusTimeline.tsx`, `ClearanceTable.tsx`, `ActionButtons.tsx`

---

### Phase 4 — Payments Module

**Status:** 🔴 Not Started
**Parallel with:** Phases 5, 6

**Checklist:**

- [ ] `Payment.java` entity (with `responseBody` as TEXT/JSONB)
- [ ] `PaymentGateway.java` interface + `StubPaymentGateway.java`
- [ ] `PaymentRepository.java` (idempotency lookup query)
- [ ] `PaymentService.java` (full idempotency logic)
- [ ] `PaymentMapper.java` (MapStruct)
- [ ] `PaymentController.java` (initiate + mark-paid endpoints)
- [ ] Frontend: "Pay Now" button + idempotency key generation
- [ ] Frontend: "Mark as Paid" button (clerk)

---

### Phase 5 — PDF Generation

**Status:** 🔴 Not Started
**Parallel with:** Phases 4, 6

**Checklist:**

- [ ] `ClearancePdfService.java` interface
- [ ] `ClearancePdfServiceImpl.java` (PDFBox 3.x)
  - [ ] Header with logo + barangay info
  - [ ] Title + metadata block
  - [ ] Body paragraph with text wrapping
  - [ ] Signature block
- [ ] Wire `GET /clearances/{id}/pdf` into `ClearanceController`
- [ ] Wire `GET /me/clearances/{id}/pdf` into `PortalClearanceController`
- [ ] Frontend: "Download PDF" button with blob download trigger

---

### Phase 6 — Settings Module

**Status:** 🔴 Not Started
**Parallel with:** Phases 4, 5

**Checklist:**

- [ ] `BarangaySettings.java` + `FeeConfig.java` entities (singleton pattern)
- [ ] `BarangaySettingsRepository.java` + `FeeConfigRepository.java`
- [ ] `SettingsService.java` (get/update settings, logo upload, get/update fees)
- [ ] `SettingsController.java` (all `/api/v1/settings/**` ADMIN-only)
- [ ] Logo upload validation (type + size)
- [ ] `GET /settings/logo` binary endpoint
- [ ] Multipart size config in `application.yml`
- [ ] Frontend: settings page (form + logo upload + preview)
- [ ] Frontend: fees page

---

### Phase 7 — Reports Module

**Status:** 🔴 Not Started
**Parallel with:** Phase 8

**Checklist:**

- [ ] `ReportsService.java` (dynamic JPQL with nullable params)
- [ ] `ReportsController.java` (`GET /api/v1/reports/clearances`)
- [ ] `ReportRowDTO.java` + `ReportMapper.java`
- [ ] Frontend: reports page (filter form + paginated table + empty state)

---

### Phase 8 — Frontend Polish & Navigation

**Status:** 🔴 Not Started
**Parallel with:** Phase 7

**Checklist:**

- [ ] `middleware.ts` — complete route guard with `jwt-decode`
- [ ] `AuthContext.tsx` — complete with `localStorage` persistence + re-hydration
- [ ] Backoffice dashboard with summary cards + 30s auto-refresh
- [ ] `StatusTimeline.tsx` — all status steps with visual states
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

## Progress Log

| Date       | Phase   | Action       | Notes                                                                                                                                                  |
| ---------- | ------- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 2026-02-24 | —       | Plan created | All feature plans split from IMPLEMENTATION_PLAN.md                                                                                                    |
| 2026-02-24 | Phase 0 | Completed    | Backend scaffold, Flyway migrations, shared exceptions, Next.js frontend, Docker Compose                                                               |
| 2026-02-24 | Phase 1 | Completed    | Identity module: JWT auth, refresh tokens, user management, Spring Security config, frontend login/register/auth context                               |
| 2026-02-24 | Docs    | Added        | `backend/docs/Security.md` — full security reference with Mermaid sequence diagram and prose process flow walkthrough for all six authentication flows |

---

## Blockers & Issues

> Record any blockers here as they arise.

None currently.
