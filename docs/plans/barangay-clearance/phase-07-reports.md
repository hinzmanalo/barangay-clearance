# Phase 7 — Reports Module

**Status:** Not Started
**Estimated Timeline:** Week 6
**Priority:** Medium

---

## Goal

Provide a filterable, paginated report of clearance issuances accessible to clerks and admins, meeting the log-book replacement requirement.

---

## Dependencies

**Depends on:**
- Phase 0 (Scaffolding) — database schema, shared `PageResponse`
- Phase 1 (Auth) — CLERK/ADMIN security
- Phase 3 (Clearance) — `ClearanceRequest` entity + `Resident` join for name/purok
- Phase 2 (Residents) — `Resident.purokZone` field used in filter

**Required by:** Nothing downstream.

**Can run in parallel with:** Phase 8 (Frontend Polish) and Phase 5 (PDF) — all are independent at this stage.

---

## Deliverables

### Backend
**Service:**
- `reports/service/ReportsService.java` — dynamic JPA query with nullable filter params

**Controller:**
- `reports/controller/ReportsController.java` — `GET /api/v1/reports/clearances`

**DTOs:**
- `ReportFilterRequest.java` — query params: `from`, `to`, `status`, `purok`, `purpose`, `paymentStatus`, `page`, `size`
- `ReportRowDTO.java` — `{ clearanceNumber, residentFullName, purpose, urgency, status, paymentStatus, issuedAt }`

**MapStruct Mapper:**
- `ReportMapper.java` — `ClearanceRequest → ReportRowDTO`
  - `residentFullName` computed: `expression = "java(cr.getResident().getFirstName() + ' ' + cr.getResident().getLastName())"`

### Frontend
- `src/app/backoffice/reports/page.tsx` — filter form + paginated table

---

## Key Implementation Notes

### Dynamic Query (JPQL with nullable params)
```jpql
SELECT cr FROM ClearanceRequest cr
JOIN cr.resident r
WHERE (:status IS NULL OR cr.status = :status)
  AND (:paymentStatus IS NULL OR cr.paymentStatus = :paymentStatus)
  AND (:purpose IS NULL OR cr.purpose = :purpose)
  AND (:purok IS NULL OR LOWER(r.purokZone) LIKE LOWER(CONCAT('%', :purok, '%')))
  AND (:from IS NULL OR cr.issuedAt >= :from)
  AND (:to IS NULL OR cr.issuedAt <= :to)
ORDER BY cr.issuedAt DESC
```
Test `NULL` comparisons in JPQL with Testcontainers (PostgreSQL-specific behavior).

### Controller Parameter Parsing
```java
@RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from
// Convert LocalDate → Instant:
Instant fromInstant = from != null ? from.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
```
`to` date is exclusive end — add 1 day to include the full day.

### Enum Parsing in Controller
```java
ClearanceStatus statusEnum = status != null ? ClearanceStatus.valueOf(status) : null;
```
Wrap in try/catch or use a `@ControllerAdvice` to handle `IllegalArgumentException` → 400.

### Default Pagination
```java
@PageableDefault(size = 20, sort = "issuedAt", direction = DESC)
```

### `ReportRowDTO` Resident Name
Denormalized at mapping time via MapStruct expression. The JPQL query fetches the full `ClearanceRequest` with the `resident` eagerly loaded (or use `JOIN FETCH`).

### Frontend Empty State
"No records found for the selected filters." — display when `content` is empty array.

---

## API Endpoints

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/reports/clearances` | CLERK, ADMIN |

**Query Parameters:**
- `status` — `ClearanceStatus` enum value
- `paymentStatus` — `ClearancePaymentStatus` enum value
- `purpose` — `Purpose` enum value
- `purok` — partial string match
- `from` / `to` — ISO date (`yyyy-MM-dd`)
- `page`, `size`, `sort`

---

## Definition of Done

- [ ] `GET /reports/clearances` without filters → 200, paginated list of all clearances
- [ ] With `status=RELEASED&from=2025-02-01&to=2025-02-28` → only RELEASED clearances in date range
- [ ] With `purok=3` → only residents with purok containing "3"
- [ ] With RESIDENT token → 403
- [ ] Frontend: filter form updates table on submission; empty state message shown
