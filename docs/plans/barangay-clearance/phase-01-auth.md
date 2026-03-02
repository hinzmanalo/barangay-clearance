# Phase 1 — Identity Module: Auth & JWT

**Status:** Not Started
**Estimated Timeline:** Week 2
**Priority:** Critical

---

## Goal

Complete user authentication: self-registration for residents, login, JWT access + refresh token issuance, silent token refresh, logout, and admin-managed staff account operations. After this phase, every subsequent module has working auth to build against.

---

## Dependencies

**Depends on:** Phase 0 (Scaffolding & Infrastructure)
**Required by:**
- Phase 2 (Residents) — activation flow needs auth
- Phase 3 (Clearance) — portal/backoffice endpoints need JWT
- Phase 4 (Payments) — payment endpoints need auth
- Phase 5 (PDF) — PDF endpoints need auth
- Phase 6 (Settings) — admin-only settings need auth
- Phase 7 (Reports) — clerk/admin reports need auth
- Phase 8 (Frontend Polish) — route guards need AuthContext

---

## Deliverables

### Backend
**Entities:**
- `identity/entity/User.java` — JPA entity mapped to `users` table
- `identity/entity/RefreshToken.java` — JPA entity mapped to `refresh_tokens` table

**Repositories:**
- `identity/repository/UserRepository.java` — `findByEmail`, `findByRole`, `findByStatus`
- `identity/repository/RefreshTokenRepository.java` — `findByTokenHash`, `deleteByUserId`, `deleteByExpiresAtBefore`

**Services:**
- `identity/service/JwtService.java` — generate/validate access token; hash/compare refresh token
- `identity/service/AuthService.java` — register, login, refresh, logout business logic
- `identity/service/UserService.java` — admin user management (list staff, create staff, deactivate)

**Security Infrastructure:**
- `shared/security/UserPrincipal.java` — implements `UserDetails`; holds `userId` and `role`
- `shared/security/JwtAuthFilter.java` — extends `OncePerRequestFilter`
- `shared/security/SecurityConfig.java` — `SecurityFilterChain` bean

**Controllers:**
- `identity/controller/AuthController.java` — `/api/v1/auth/**` (all public)
- `identity/controller/UserController.java` — `/api/v1/admin/users/**` (ADMIN only)

**DTOs:**
- `LoginRequest`, `RegisterRequest`, `TokenResponse`, `RefreshRequest`, `UserDTO`, `CreateStaffRequest`

### Frontend
- `src/app/login/page.tsx` — login form (React Hook Form + Zod)
- `src/app/register/page.tsx` — registration form
- `src/lib/api.ts` — Axios instance with request/response interceptors (401 → refresh → retry)
- `src/context/AuthContext.tsx` — `AuthProvider`, `useAuth()` hook
- `src/middleware.ts` — skeleton (permit `/login`, `/register`; redirect others)
- `src/types/auth.ts`

---

## Key Implementation Notes

### `JwtService` (JJWT 0.12.x)
```java
// Key
SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
// Build token: embed userId + role as claims
// Parse: Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
```
Refresh tokens: opaque UUIDs stored as SHA-256 hashes (`DigestUtils.sha256Hex`). Return raw token to client, store only the hash.

### `JwtAuthFilter`
1. Extract `Authorization: Bearer <token>`
2. If absent → continue chain (public endpoints handled by SecurityConfig)
3. Parse/validate JWT
4. Build `UserPrincipal` from JWT claims (no DB hit per request)
5. Set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
6. On `JwtException` → clear context, let 401 handler respond

### `SecurityConfig`
- CSRF disabled, stateless session
- Custom `authenticationEntryPoint` (401) and `accessDeniedHandler` (403) writing `ErrorResponse`
- Permit: `/api/v1/auth/**`, `/swagger-ui/**`, `/api-docs/**`
- Roles: `ROLE_RESIDENT`, `ROLE_CLERK`, `ROLE_APPROVER`, `ROLE_ADMIN`
- `hasRole("X")` requires authority `ROLE_X` — set via `new SimpleGrantedAuthority("ROLE_" + role.name())`

### `POST /auth/register` Flow
1. Validate email uniqueness → `ConflictException` if taken
2. Hash password with BCrypt strength 12
3. Create `User` with role `RESIDENT`, status `PENDING_VERIFICATION`
4. Create linked `Resident` profile (atomically in same transaction via `ResidentService`)
5. Audit: `USER_REGISTERED`
6. Return 201 — no token (resident must be activated by clerk first)

### `POST /auth/login` Flow
1. Look up user by email
2. Verify `passwordEncoder.matches(raw, hash)`
3. Check status: `PENDING_VERIFICATION` / `REJECTED` / `DEACTIVATED` → 403
4. If `mustChangePassword` → token includes `mustChangePassword: true` claim
5. Generate access token (15 min) + refresh token (7 days), save hashed refresh
6. Audit: `USER_LOGIN`
7. Return `TokenResponse`

### `POST /auth/refresh`
1. Hash incoming token; find by hash
2. Validate: not revoked, not expired
3. Issue new access token (no refresh token rotation in MVP)
4. Return new `accessToken` only

### `POST /auth/logout`
1. Hash incoming refresh token; find; set `revoked = true`
2. Return 200

### Token Storage (MVP)
Use `localStorage` for both tokens (simpler). Document XSS risk. Note to migrate to `httpOnly` cookies post-MVP.

### Axios Interceptors
- **Request:** Attach `Authorization: Bearer <accessToken>`
- **Response:** On 401 + not already retried → call refresh → retry original request; if refresh fails → clear tokens, redirect to `/login`

### `must_change_password` Flow
Frontend intercepts JWT claim; redirects to `/change-password`. Backend endpoint: `PUT /api/v1/auth/change-password` validates current password, sets new, clears flag, issues new tokens.

---

## API Endpoints

| Method | Path | Role |
|--------|------|------|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Public |
| POST | `/api/v1/auth/logout` | Any |
| PUT | `/api/v1/auth/change-password` | Any |
| GET | `/api/v1/admin/users` | ADMIN |
| POST | `/api/v1/admin/users` | ADMIN |
| PUT | `/api/v1/admin/users/{id}/deactivate` | ADMIN |

---

## Definition of Done

- [ ] `POST /auth/register` with valid body → 201; duplicate email → 409
- [ ] `POST /auth/login` → 200 with tokens; wrong credentials → 401; `PENDING_VERIFICATION` → 403
- [ ] `POST /auth/refresh` → 200 with new access token; expired refresh → 401
- [ ] `POST /auth/logout` → 200; subsequent refresh with same token → 401
- [ ] `GET /admin/users` with RESIDENT token → 403; with ADMIN token → 200
- [ ] Frontend: RESIDENT login → `/portal/dashboard`; ADMIN/CLERK/APPROVER → `/backoffice/dashboard`
- [ ] Silent refresh works on token expiry
- [ ] Logout clears tokens
