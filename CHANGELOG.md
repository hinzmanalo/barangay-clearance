# Changelog

All notable changes to the Barangay Clearance System are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).  
Versions map to implementation phases. Unreleased sections track work-in-progress.

---

## [Unreleased]

> Work planned but not yet started (Phases 2–10).

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

[Unreleased]: https://github.com/your-org/barangay-clearance/compare/v0.2.1...HEAD
[0.2.1]: https://github.com/your-org/barangay-clearance/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/your-org/barangay-clearance/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/your-org/barangay-clearance/releases/tag/v0.1.0
