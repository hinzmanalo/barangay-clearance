# Phase 11 — User Management

**Status:** Not Started
**Estimated Timeline:** Week 7 (parallel with Phase 8 or before Phase 9)
**Priority:** High

---

## Goal

Extend the existing identity module with a complete user management feature: backend API gaps (activate, role update, profile update, admin password reset, search/filter, own-profile endpoint), and a full backoffice UI so administrators can view, create, edit, and manage all staff accounts.

---

## Dependencies

**Depends on:**

- Phase 0 (Scaffolding) — project structure
- Phase 1 (Auth) — `UserController`, `UserService`, `User` entity, `UserDTO`

**Required by:**

- Phase 9 (Testing) — admin user management flows must be testable

**Can run in parallel with:** Phases 4, 5, 6, 7, 8

---

## Gap Analysis

### What exists (Phase 1)

| Endpoint                                  | Description                        |
| ----------------------------------------- | ---------------------------------- |
| `GET /api/v1/admin/users`                 | List staff — paginated, ADMIN only |
| `GET /api/v1/admin/users/{id}`            | Get user by ID                     |
| `POST /api/v1/admin/users`                | Create staff account               |
| `PUT /api/v1/admin/users/{id}/deactivate` | Deactivate user                    |

### What is missing

**Backend:**

- No activate/reactivate endpoint
- No role-change endpoint
- No profile-update endpoint (name, email)
- No admin-triggered password reset
- No search/filter on list endpoint (role, status, keyword)
- No `GET /api/v1/users/me` for any authenticated user to fetch their own profile
- No `PUT /api/v1/users/me` for any user to update their own profile

**Frontend:**

- No `/backoffice/admin/users` page exists — the backoffice has no user management UI at all
- No sidebar link for "Users" in the backoffice navigation
- No React Query hook for users

---

## Deliverables

### Backend

#### New DTOs (`identity/dto/`)

- `UpdateStaffRequest.java` — `firstName`, `lastName`, `email` (all optional via `@Nullable`)
- `UpdateRoleRequest.java` — `role` (must not be `RESIDENT`)
- `AdminResetPasswordRequest.java` — `newPassword` (validated strength)
- `UpdateProfileRequest.java` — `firstName`, `lastName` (used for `PUT /me`)

#### `UserService.java` — new methods

```
activate(UUID userId)                  → UserDTO
updateRole(UUID userId, Role role)     → UserDTO
updateStaff(UUID userId, UpdateStaffRequest req) → UserDTO
adminResetPassword(UUID userId, String newPassword) → void
getCurrentUser(UUID userId)            → UserDTO
updateCurrentUser(UUID userId, UpdateProfileRequest req) → UserDTO
```

Business rules:

- `activate`: must be `DEACTIVATED`; set status to `ACTIVE`
- `updateRole`: cannot set `RESIDENT`; cannot demote own account (guard: `userId != callerUserId`)
- `adminResetPassword`: BCrypt-encodes new password, sets `mustChangePassword = true`, invalidates all refresh tokens for that user (`refreshTokenRepository.deleteByUserId(userId)`)
- `updateStaff`: if email changes, check uniqueness
- `listStaff` (enhance existing): accept `role`, `status`, `search` (keyword matched against `firstName`, `lastName`, `email`) query params

#### `UserController.java` — new endpoints (all `ADMIN` only unless noted)

| Method | Path                                      | Description                                          | Auth  |
| ------ | ----------------------------------------- | ---------------------------------------------------- | ----- |
| `PUT`  | `/api/v1/admin/users/{id}/activate`       | Reactivate a deactivated user                        | ADMIN |
| `PUT`  | `/api/v1/admin/users/{id}/role`           | Change role                                          | ADMIN |
| `PUT`  | `/api/v1/admin/users/{id}`                | Update profile (name, email)                         | ADMIN |
| `POST` | `/api/v1/admin/users/{id}/reset-password` | Force-reset password + set `mustChangePassword=true` | ADMIN |
| `GET`  | `/api/v1/admin/users` (enhanced)          | Add `role`, `status`, `search` filter params         | ADMIN |

#### New controller: `MeController.java` (`/api/v1/users/me`)

| Method | Path               | Description       | Auth                   |
| ------ | ------------------ | ----------------- | ---------------------- |
| `GET`  | `/api/v1/users/me` | Fetch own profile | Any authenticated user |
| `PUT`  | `/api/v1/users/me` | Update own name   | Any authenticated user |

> `PUT /me` must NOT allow email or role changes — those are admin-only operations.

#### Swagger annotations

All new endpoints must include `@Operation`, `@ApiResponses` (200, 400, 403, 404, 409), and `@SecurityRequirement(name = "bearer-jwt")`.

---

### Frontend

#### New pages

| Route                          | Component                                  | Description                                                             |
| ------------------------------ | ------------------------------------------ | ----------------------------------------------------------------------- |
| `/backoffice/admin/users`      | `app/backoffice/admin/users/page.tsx`      | Paginated user list with search + role/status filter                    |
| `/backoffice/admin/users/new`  | `app/backoffice/admin/users/new/page.tsx`  | Create staff form                                                       |
| `/backoffice/admin/users/[id]` | `app/backoffice/admin/users/[id]/page.tsx` | User detail + edit + role change + activate/deactivate + reset password |

#### New hooks (`hooks/useUsers.ts`)

```ts
useUsers(params); // GET /admin/users — paginated, filtered
useUser(id); // GET /admin/users/:id
useCreateUser(); // POST /admin/users
useUpdateUser(id); // PUT /admin/users/:id
useUpdateUserRole(id); // PUT /admin/users/:id/role
useActivateUser(id); // PUT /admin/users/:id/activate
useDeactivateUser(id); // PUT /admin/users/:id/deactivate
useAdminResetPassword(id); // POST /admin/users/:id/reset-password
useCurrentUser(); // GET /users/me
useUpdateCurrentUser(); // PUT /users/me
```

#### New shared components

- `components/backoffice/UserTable.tsx` — table with columns: Name, Email, Role, Status, Created, Actions
- `components/shared/RoleBadge.tsx` — color-coded role chip (ADMIN=red, APPROVER=purple, CLERK=blue, RESIDENT=gray)

#### Sidebar update (`components/backoffice/Sidebar.tsx`)

- Add "Users" nav link under an "Admin" section, visible only when `user.role === 'ADMIN'`
- Route: `/backoffice/admin/users`

#### Types update (`types/auth.ts`)

```ts
export type UserStatus = "ACTIVE" | "INACTIVE" | "DEACTIVATED";

export interface StaffUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  status: UserStatus;
  mustChangePassword: boolean;
  createdAt: string;
}

export interface CreateStaffPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: "ADMIN" | "CLERK" | "APPROVER";
}

export interface UpdateStaffPayload {
  firstName?: string;
  lastName?: string;
  email?: string;
}

export interface UpdateRolePayload {
  role: "ADMIN" | "CLERK" | "APPROVER";
}

export interface AdminResetPasswordPayload {
  newPassword: string;
}
```

---

## Key Implementation Notes

### Password reset flow

1. Admin submits new password via `POST /admin/users/{id}/reset-password`
2. Backend: hash password → save → delete all refresh tokens for that user → log event
3. On next login, `mustChangePassword = true` forces the change-password page
4. The `/change-password` page already exists at `app/change-password/page.tsx`

### Role guard on role change

The admin cannot demote their own account — `UserService.updateRole` must compare `userId` against the calling user's ID (injected from `@AuthenticationPrincipal UserPrincipal`). Return `400 Cannot change your own role`.

### Search/filter implementation pattern

Follow the same `SpecificationBuilder<T>` pattern used in `ResidentService`. Add JPA Specification predicates for `role`, `status`, and `ILIKE` keyword against `firstName || ' ' || lastName` or `email`.

### `listStaff` scoping

The list endpoint should filter to non-RESIDENT users only (existing behavior). Residents managed through the Residents module, not here.

### Frontend create form fields and validation (Zod)

```ts
const createStaffSchema = z.object({
  email: z.string().email(),
  firstName: z.string().min(1).max(100),
  lastName: z.string().min(1).max(100),
  role: z.enum(["ADMIN", "CLERK", "APPROVER"]),
  password: z.string().min(8).regex(/[A-Z]/).regex(/[0-9]/),
});
```

---

## Definition of Done

### Backend

- [ ] `UpdateStaffRequest.java`, `UpdateRoleRequest.java`, `AdminResetPasswordRequest.java`, `UpdateProfileRequest.java` DTOs created
- [ ] `UserService` extended with `activate`, `updateRole`, `updateStaff`, `adminResetPassword`, `getCurrentUser`, `updateCurrentUser`
- [ ] `listStaff` accepts `role`, `status`, `search` query params (JPA Specification)
- [ ] `UserController` has `PUT /{id}/activate`, `PUT /{id}/role`, `PUT /{id}`, `POST /{id}/reset-password`
- [ ] `MeController` created with `GET /me`, `PUT /me` at `/api/v1/users/me`
- [ ] All new endpoints documented with `@Operation`, `@ApiResponses`, `@SecurityRequirement`
- [ ] `adminResetPassword` invalidates refresh tokens
- [ ] Role-change guard: cannot change own role
- [ ] `./mvnw test` passes with no regressions

### Frontend

- [ ] `types/auth.ts` updated with `StaffUser`, payload types
- [ ] `hooks/useUsers.ts` created with all hooks
- [ ] `UserTable.tsx` component implemented
- [ ] `RoleBadge.tsx` component implemented
- [ ] `/backoffice/admin/users` list page — search, filter, pagination, create button
- [ ] `/backoffice/admin/users/new` create form — validation with Zod, shows API errors
- [ ] `/backoffice/admin/users/[id]` detail page — view + edit profile, change role, activate/deactivate toggle, reset password modal
- [ ] Backoffice `Sidebar.tsx` updated with "Users" link (ADMIN only)
- [ ] `npm run build` passes with no type errors
