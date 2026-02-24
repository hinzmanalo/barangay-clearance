# Phase 0 — Project Scaffolding & Infrastructure

**Status:** Not Started
**Estimated Timeline:** Week 1
**Priority:** Critical (Foundation for all other phases)

---

## Goal

Establish the skeleton that all subsequent phases build upon: an empty but runnable Spring Boot backend, an empty but runnable Next.js frontend, Docker Compose for all environments, the complete database schema via Flyway, and the shared error-handling infrastructure.

By the end of this phase, `docker compose -f docker-compose.dev.yml up -d && ./mvnw spring-boot:run` runs cleanly and Flyway migrations apply all 9 tables successfully.

---

## Dependencies

**Depends on:** None — this is the foundation phase.
**Required by:** All other phases (Phase 1–10)

---

## Deliverables

### Backend
- `backend/pom.xml` — all dependencies declared
- `backend/mvnw` + `backend/.mvn/wrapper/maven-wrapper.properties`
- `backend/src/main/java/com/barangay/clearance/BarangayClearanceApplication.java`
- `backend/src/main/resources/application.yml` — base config
- `backend/src/main/resources/application-local.yml` — local dev overrides
- `backend/src/main/resources/application-prod.yml` — prod overrides (no secrets)
- `backend/src/main/resources/application-test.yml` — test overrides
- `backend/src/main/resources/db/migration/V1__initial_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_settings.sql`
- `backend/src/main/resources/db/migration/V3__seed_admin.sql`
- `backend/src/main/java/com/barangay/clearance/shared/exception/ErrorResponse.java`
- `backend/src/main/java/com/barangay/clearance/shared/exception/AppException.java`
- `backend/src/main/java/com/barangay/clearance/shared/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/barangay/clearance/shared/util/PageResponse.java`

### Frontend
- `frontend/` — initialized via `npx create-next-app@14 frontend --typescript --tailwind --app`
- `frontend/src/app/layout.tsx` — root layout
- `frontend/src/app/page.tsx` — root redirect placeholder
- `frontend/src/lib/api.ts` — Axios instance skeleton (no interceptors yet)
- `frontend/src/types/` — empty TypeScript type files for each domain

### Infrastructure
- `docker-compose.dev.yml` — PostgreSQL only (port 5432 exposed to host)
- `docker-compose.yml` — production: postgres + backend + frontend + nginx
- `nginx/nginx.conf` — HTTP→HTTPS redirect, `/api/` proxy to backend, `/` proxy to frontend
- `.env.example`

---

## Key Implementation Notes

### Flyway V1 — `V1__initial_schema.sql`
Create all 9 tables with UUID PKs (`DEFAULT gen_random_uuid()`):
- `clearance_number_sequence.year_month` is `CHAR(7)` (e.g., `'2025-02'`)
- `payments` has composite unique index on `(idempotency_key, initiated_by_user_id)`
- `barangay_settings.id` has `CHECK (id = 1)` — singleton enforcement
- `fee_config.id` has `CHECK (id = 1)` — singleton enforcement
- `audit_logs`: `@PrePersist` only (no updates/deletes)
- Indexes: `idx_users_email`, `idx_users_role_status`, `idx_residents_name` (functional: `lower(last_name), lower(first_name)`), `idx_cr_status`, `idx_cr_issued_at`, `idx_payments_idempotency`

### Flyway V2 — `V2__seed_settings.sql`
Seed `barangay_settings` and `fee_config` singleton rows with `ON CONFLICT DO NOTHING`.

### Flyway V3 — `V3__seed_admin.sql`
Insert admin user with BCrypt-hashed password (strength 12). Document temp password in `docs/ADMIN_SETUP.md`. Set `must_change_password = true`.

### `ErrorResponse.java`
Fields: `status`, `error`, `message`, `timestamp` (Instant), `path`, `details` (Map for field-level validation errors).

### `PageResponse<T>`
Generic wrapper: `{ content, totalElements, totalPages, page, size }`. Static factory: `PageResponse.of(Page<T> page)`.

### `GlobalExceptionHandler`
Handle: `MethodArgumentNotValidException` → 400, `ConstraintViolationException` → 400, `NotFoundException` → 404, `BadRequestException` → 400, `ConflictException` → 409, `ForbiddenException` → 403, `AccessDeniedException` → 403, `AuthenticationException` → 401, `RuntimeException` → 500.

### Annotation Processor Order
`lombok` → `mapstruct-processor` (enforced via `lombok-mapstruct-binding`)

---

## Definition of Done

- [ ] `docker compose -f docker-compose.dev.yml up -d` starts PostgreSQL on port 5432 with no errors
- [ ] `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` starts cleanly; Flyway log shows V1, V2, V3 applied
- [ ] `./mvnw test` build succeeds (no test files yet)
- [ ] `http://localhost:8080/swagger-ui.html` opens (nearly empty) Swagger UI
- [ ] `npm run dev` inside `frontend/` starts Next.js on port 3000 with no errors
- [ ] `POST http://localhost:8080/api/v1/nonexistent` returns `ErrorResponse` JSON with `status: 404`
