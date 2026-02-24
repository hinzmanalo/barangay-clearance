# Phase 3 — Clearance Module

**Status:** Not Started
**Estimated Timeline:** Week 3–4
**Priority:** Critical (Core business logic)

---

## Goal

Implement the core state machine: submit, approve, reject, and release clearance requests, plus the atomic clearance number sequence and audit trail for all transitions.

---

## Dependencies

**Depends on:**
- Phase 0 (Scaffolding) — database schema, shared infrastructure
- Phase 1 (Auth) — JWT security, `UserPrincipal` for portal scoping
- Phase 2 (Residents) — `Resident` entity referenced by clearance requests

**Required by:**
- Phase 4 (Payments) — payment attaches to a `ClearanceRequest`
- Phase 5 (PDF) — PDF generated from a released `ClearanceRequest`
- Phase 7 (Reports) — reports query `ClearanceRequest` table
- Phase 8 (Frontend Polish) — dashboard counts, status timeline, action buttons

**Can run in parallel with:** Phase 6 (Settings) — no dependency in either direction.

---

## Deliverables

### Backend
**Entities:**
- `clearance/entity/ClearanceRequest.java` — all status/paymentStatus/purpose/urgency enums
- `clearance/entity/ClearanceNumberSequence.java` — `year_month` (PK CHAR 7), `lastSeq`

**Enums:**
- `ClearanceStatus` — `DRAFT`, `FOR_APPROVAL`, `APPROVED`, `REJECTED`, `RELEASED`
- `ClearancePaymentStatus` — `UNPAID`, `PAID`, `WAIVED`
- `Purpose`, `Urgency`

**Repositories:**
- `ClearanceRequestRepository.java`
  - `findByResidentUserId(UUID userId, Pageable)` — portal scoping
  - `findAllWithFilters(status, paymentStatus, from, to, Pageable)` — backoffice list
  - `countByStatus(ClearanceStatus)` — dashboard cards
- `ClearanceNumberSequenceRepository.java` — atomic sequence increment via native query

**Services:**
- `clearance/service/ClearanceNumberService.java` — atomic PostgreSQL `ON CONFLICT DO UPDATE RETURNING`
- `clearance/service/ClearanceService.java` — state machine, all transition methods

**MapStruct Mapper:**
- `clearance/service/mapper/ClearanceMapper.java` — `ClearanceRequest ↔ ClearanceRequestDTO`

**Controllers:**
- `clearance/controller/ClearanceController.java` — backoffice `/api/v1/clearances/**`
- `clearance/controller/PortalClearanceController.java` — resident portal `/api/v1/me/clearances/**`

**DTOs:**
- `ClearanceRequestDTO.java` — includes denormalized resident name
- `CreateClearanceRequest.java` — `{ purpose, purposeOther, urgency, copies, notes }`
- `RejectRequest.java` — `{ reason }`

### Frontend
- `src/app/portal/dashboard/page.tsx` — My Requests list
- `src/app/portal/requests/new/page.tsx` — submit form
- `src/app/portal/requests/[id]/page.tsx` — detail + status timeline
- `src/app/backoffice/clearances/page.tsx` — list with filters
- `src/app/backoffice/clearances/new/page.tsx` — walk-in request form
- `src/app/backoffice/clearances/[id]/page.tsx` — detail + action buttons
- `src/components/portal/StatusTimeline.tsx`
- `src/components/portal/RequestCard.tsx`
- `src/components/backoffice/ClearanceTable.tsx`
- `src/components/backoffice/ActionButtons.tsx`
- `src/types/clearance.ts`

---

## Key Implementation Notes

### State Machine Transitions
Enforce explicit guards — no library:

| From | Action | To | Guard |
|------|--------|----|-------|
| — | Submit (portal) | `FOR_APPROVAL` | resident `ACTIVE` |
| — | Create (walk-in) | `FOR_APPROVAL` | clerk |
| `FOR_APPROVAL` | approve | `APPROVED` | APPROVER/ADMIN |
| `FOR_APPROVAL` | reject | `REJECTED` | APPROVER/ADMIN, reason required |
| `REJECTED` | resubmit | `FOR_APPROVAL` | same resident owner |
| `APPROVED` + `PAID` | release | `RELEASED` | CLERK/ADMIN |

Each method validates current status before mutating and throws `BadRequestException` on invalid transition.

### Atomic Clearance Number Sequence
```sql
INSERT INTO clearance_number_sequence (year_month, last_seq) VALUES (:yearMonth, 1)
ON CONFLICT (year_month) DO UPDATE SET last_seq = clearance_number_sequence.last_seq + 1
RETURNING last_seq
```
Format: `YYYY-MM-NNNN` (e.g., `2025-02-0001`). Assigned **only at release** — never earlier (ADR-008).

### `release()` Guard
Must validate: `status == APPROVED` AND `paymentStatus == PAID`. Otherwise → `BadRequestException`.

### Portal Scoping (Security Critical)
`PortalClearanceController` always resolves `residentId` from JWT principal — never from request params. Prevents horizontal privilege escalation.

### Walk-in Requests
`POST /clearances` (backoffice): clerk selects resident from registry. Request starts at `FOR_APPROVAL` immediately (no DRAFT step). `created_by_user_id` = clerk's userId.

### `resubmit()` Ownership Check
Validate `clearance.resident.userId == principal.userId` before allowing edit/resubmit.

### Spring Application Events (Phase 2 prep)
Publish `ClearanceStatusChangedEvent` on every transition. No listener needed in MVP — zero-cost preparation hook.

### Dashboard Summary
`GET /api/v1/clearances/summary` returns:
```json
{ "pendingApproval": N, "approvedAwaitingPayment": N, "releasedToday": N }
```

---

## API Endpoints

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/me/clearances` | RESIDENT |
| POST | `/api/v1/me/clearances` | RESIDENT |
| GET | `/api/v1/me/clearances/{id}` | RESIDENT |
| PUT | `/api/v1/me/clearances/{id}` | RESIDENT (resubmit) |
| GET | `/api/v1/clearances` | CLERK, APPROVER, ADMIN |
| POST | `/api/v1/clearances` | CLERK, ADMIN |
| GET | `/api/v1/clearances/{id}` | CLERK, APPROVER, ADMIN |
| POST | `/api/v1/clearances/{id}/approve` | APPROVER, ADMIN |
| POST | `/api/v1/clearances/{id}/reject` | APPROVER, ADMIN |
| POST | `/api/v1/clearances/{id}/release` | CLERK, ADMIN |
| GET | `/api/v1/clearances/summary` | CLERK, APPROVER, ADMIN |

---

## Definition of Done

- [ ] `POST /me/clearances` with ACTIVE resident → 201 with `FOR_APPROVAL`
- [ ] `POST /clearances/{id}/approve` with APPROVER token → 200 with `APPROVED`; with CLERK token → 403
- [ ] `POST /clearances/{id}/reject` without reason → 400
- [ ] `POST /clearances/{id}/release` when `paymentStatus = UNPAID` → 400
- [ ] `POST /clearances/{id}/release` when `APPROVED + PAID` → 200; `clearanceNumber` is `YYYY-MM-NNNN`
- [ ] Concurrent release of 10 requests for same month assigns 10 unique sequential numbers
- [ ] Portal: resident only sees their own requests; unauthorized access → 403
