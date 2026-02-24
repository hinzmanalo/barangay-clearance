# Phase 2 — Residents Module

**Status:** Not Started
**Estimated Timeline:** Week 2–3
**Priority:** High

---

## Goal

Enable clerk-managed resident registry with search, plus the pending-registration review workflow that unblocks residents from submitting clearance requests.

---

## Dependencies

**Depends on:**
- Phase 0 (Scaffolding) — database schema, shared infrastructure
- Phase 1 (Auth) — `User` entity, `AuthService.createFromRegistration`, JWT for endpoint security

**Required by:**
- Phase 3 (Clearance) — clearances reference `Resident` entities
- Phase 7 (Reports) — report rows include resident name/purok

**Can run in parallel with:** Nothing — must follow Phase 1.

---

## Deliverables

### Backend
**Entity:**
- `residents/entity/Resident.java` — UUID PK, optional FK `user_id → users(id)`

**Repository:**
- `residents/repository/ResidentRepository.java`
  - Custom JPQL search: `Page<Resident> search(String q, String purok, Pageable pageable)`
  - Uses `LOWER(CONCAT(...))` to match `idx_residents_name` functional index

**Service:**
- `residents/service/ResidentService.java`
  - `create`, `update`, `search`
  - `createFromRegistration(RegisterRequest, User)` — called atomically from `AuthService`
  - `findPendingUsers()`, `activateUser(userId)`, `rejectUser(userId)`

**MapStruct Mapper:**
- `residents/service/mapper/ResidentMapper.java` — `Resident ↔ ResidentDTO`
  - Computed `hasPortalAccount`: `expression = "java(resident.getUserId() != null)"`

**Controller:**
- `residents/controller/ResidentController.java` — all `/api/v1/residents/**` endpoints

**DTOs:**
- `ResidentDTO.java` — includes `hasPortalAccount` computed field
- `ResidentSearchRequest.java`

### Frontend
- `src/app/backoffice/residents/page.tsx` — list with debounced search (300ms `setTimeout`)
- `src/app/backoffice/residents/new/page.tsx` — create form
- `src/app/backoffice/residents/[id]/page.tsx` — detail + edit + pending user actions
- `src/components/backoffice/ResidentTable.tsx`
- `src/types/resident.ts`

---

## Key Implementation Notes

### Resident Creation from Registration Flow
`AuthService` calls `ResidentService.createFromRegistration(RegisterRequest, User)` inside the same `@Transactional` method. Both `User` and `Resident` are created atomically. The resident's `userId` is linked immediately so the clerk's pending list shows their profile data.

### Walk-in Residents
Clerks can create a `Resident` with no `user_id`. Can be linked later via `PUT /residents/{id}` or used directly for walk-in clearance requests.

### Activating a Resident
`activateUser(userId)` sets `user.status = ACTIVE`. Use `@PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")`.

### Search Performance
The `idx_residents_name` index is functional: `lower(last_name), lower(first_name)`. JPQL uses `LOWER()` to match. No special index needed for `purok_zone` partial match at MVP scale.

### Audit Events
Log: `RESIDENT_CREATED`, `RESIDENT_UPDATED`, `USER_ACCOUNT_ACTIVATED`, `USER_ACCOUNT_REJECTED`

---

## API Endpoints

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/residents` | CLERK, ADMIN |
| POST | `/api/v1/residents` | CLERK, ADMIN |
| GET | `/api/v1/residents/{id}` | CLERK, ADMIN |
| PUT | `/api/v1/residents/{id}` | CLERK, ADMIN |
| GET | `/api/v1/residents/pending-users` | CLERK, ADMIN |
| POST | `/api/v1/residents/users/{userId}/activate` | CLERK, ADMIN |
| POST | `/api/v1/residents/users/{userId}/reject` | CLERK, ADMIN |

---

## Definition of Done

- [ ] `POST /residents` creates a resident; duplicate email on linked account → 409
- [ ] `GET /residents?q=dela+Cruz` returns paginated matching results
- [ ] `GET /residents/pending-users` returns users with `status = PENDING_VERIFICATION`
- [ ] `POST /residents/users/{userId}/activate` changes status to `ACTIVE`; user can log in
- [ ] Frontend: search filters debounced 300ms. Pending list shows Activate/Reject buttons. Success toast on action.
