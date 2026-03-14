# PRD: List Filtering, Sorting & Pagination Framework

- **Version**: 1.0
- **Last updated**: March 7, 2026
- **Status**: Approved for Development
- **Author**: Barangay Clearance Engineering Team

---

## 1. Product Overview

### 1.1 Product Summary

The Barangay Clearance System currently supports basic pagination across its list endpoints but lacks a standardized, client-controlled approach to filtering and sorting. Staff members who manage clearance requests, residents, and user accounts must scroll through all records or rely on limited, hardcoded filters baked into each endpoint. This creates friction in day-to-day operations — for example, a clerk who wants to see only approved-but-unpaid clearances for a specific date range must either scan all pages or request a developer-assisted database query.

This PRD defines a **List Filtering, Sorting & Pagination Framework** — a standardized, convention-based approach for the three highest-traffic list endpoints in the system: clearances, residents, and users. The framework applies **exclusively to list-retrieval (collection GET) endpoints**. Single-resource detail endpoints (`GET /{id}`) and all mutation endpoints (POST, PATCH, DELETE) are explicitly out of scope and remain unchanged.

The backend provides server-side validated, safelist-controlled filtering and sorting via a consistent query parameter convention (`filter[field]=value`, `sort=field,direction`). The frontend delivers a reusable `FilterBuilder` component and `SortFacet` component for all four list pages in the portal and backoffice.

### 1.2 Tech Stack Summary

This feature is built on the existing Barangay Clearance System stack: **Spring Boot 3.3.4 / Java 21** for the backend, extending the existing `SpecificationBuilder` utility and `JpaSpecificationExecutor` repositories. The frontend is **Next.js 14 / TypeScript / Tailwind / shadcn/ui** using TanStack React Query and centralized Axios API client. No new libraries are introduced — the framework is built entirely on existing infrastructure.

---

## 2. Goals

### 2.1 Business Goals

- Enable barangay staff to quickly locate specific clearance records without scrolling through all pages, reducing time-on-task by an estimated 60%.
- Allow clerks and approvers to filter by workflow state (e.g., `FOR_APPROVAL`, `APPROVED + UNPAID`) to manage their daily queues efficiently.
- Provide a consistent API contract across all list endpoints, reducing developer onboarding time when adding new list features.
- Improve data discoverability for the admin managing users and residents.

### 2.2 User Goals

- **Clerks & Approvers**: Filter clearance lists by status, payment status, and date range. Sort by creation date or issue date to manage workload queues.
- **Admins**: Filter user lists by role and status. Sort residents by last name or registration date for quick lookup.
- **Residents (Portal)**: Filter their own clearance history by status. Sort by date to find the most recent request.
- All users: Receive immediate, clear feedback (error message + list of valid options) if they use an invalid filter field — no silent data loss from mistyped params.

### 2.3 Non-Goals

- **Advanced filter types**: IN clauses, nested entity joins (e.g., filter residents by clearance count), full-text search beyond existing substring match — deferred to Phase 2.
- **Filter metadata API** (`GET /api/v1/meta/{entity}/filters`): Frontend will use hardcoded filter constants for now. A dynamic metadata discovery endpoint is a future enhancement.
- **Database index optimization**: DB indexes for filterable/sortable columns are tracked but intentionally deferred to a performance-tuning phase.
- **Saved/named filters**: Persisting filter presets per user is out of scope for this iteration.
- **Export with filters applied**: Report generation with filtered results is handled by the separate `/reports` module and is out of scope.
- **Audit logs, payments, reports filtering**: Only clearance, residents, and users endpoints are in scope.
- **Single-resource endpoints**: `GET /clearances/{id}`, `GET /residents/{id}`, `GET /users/{id}` — no changes.
- **Mutation endpoints**: POST, PATCH, DELETE endpoints across all modules — untouched.

---

## 3. User Personas

### 3.1 Key User Types

- **CLERK** — Staff member who processes day-to-day clearance requests.
- **APPROVER** — Staff member authorized to approve or reject clearance requests.
- **ADMIN** — Full-access staff who manages users, residents, and system settings.
- **RESIDENT** — Citizen with a portal account who submits and tracks their own clearance requests.

### 3.2 Basic Persona Details

- **Maria (Clerk)**: Processes 20–30 clearance requests per day. Needs to quickly filter to `FOR_APPROVAL` status, sort by oldest-first to work through her queue in order. Not technically savvy — relies entirely on the UI filter forms; does not interact with raw API params.

- **Jose (Approver)**: Reviews batches of clearances. Wants to see `APPROVED + UNPAID` clearances to follow up on payment. Sorts by `issuedAt` descending to see recently approved items first.

- **Admin (Barangay Captain's Office)**: Manages user accounts and resident profiles. Filters users by `RESIDENT` role to audit resident portal access. Sorts residents by `lastName` ascending alphabetically.

- **Ana (Resident)**: Views her own clearance request history in the portal. Wants to filter by `APPROVED` to confirm recent completions. Sorts by `createdAt` descending to see latest request first.

### 3.3 Role-Based Access

- **ADMIN**: Full filter/sort/page on `/clearances`, `/residents`, `/admin/users`.
- **CLERK, APPROVER**: Full filter/sort/page on `/clearances`, read-only on `/residents`.
- **RESIDENT**: Filter/sort/page on `/portal/clearances` — scoped to their own records only (JWT-enforced). Cannot filter other residents' data.

---

## 4. Functional Requirements

### 4.1 List Endpoint Filter Support (Priority: High)

Each in-scope list endpoint must accept filter parameters using the convention `filter[fieldName]=value` as URL query parameters.

- The system validates each filter field name against a per-entity safelist of allowed fields.
- The system validates filter values against the field's type (e.g., enum values for status fields, ISO-8601 for date fields).
- If an unknown filter field is provided, the system returns `400 Bad Request` with an error message listing the allowed fields.
- If a filter value is invalid (e.g., `filter[status]=INVALID_ENUM`), the system returns `400 Bad Request` with a descriptive message.
- Unknown (non-filter) query params not matching the `filter[*]` or `sort` convention are ignored.
- Multiple filters can be combined in a single request (AND logic — all conditions must match).
- Filter fields with null/absent values are ignored (not applied as constraints).

### 4.2 Per-Entity Allowed Filter Fields (Priority: High)

**Clearance Request (`GET /api/v1/clearances`, `GET /api/v1/portal/clearances`)**:

| Filter Field            | Type | Values / Format                                             | Notes                                  |
| ----------------------- | ---- | ----------------------------------------------------------- | -------------------------------------- |
| `filter[status]`        | Enum | `DRAFT`, `FOR_APPROVAL`, `APPROVED`, `REJECTED`, `RELEASED` | Exact match                            |
| `filter[paymentStatus]` | Enum | `UNPAID`, `PAID`, `WAIVED`                                  | Exact match                            |
| `filter[createdAtFrom]` | Date | ISO-8601 `YYYY-MM-DD`                                       | Inclusive start of creation date range |
| `filter[createdAtTo]`   | Date | ISO-8601 `YYYY-MM-DD`                                       | Inclusive end of creation date range   |
| `filter[residentId]`    | UUID | Valid UUID v4                                               | Staff-only; portal scoped by JWT       |

**Resident (`GET /api/v1/residents`)**:

| Filter Field     | Type   | Values / Format           | Notes                                              |
| ---------------- | ------ | ------------------------- | -------------------------------------------------- |
| `filter[q]`      | String | Free text                 | Case-insensitive substring on firstName + lastName |
| `filter[purok]`  | String | Free text                 | Exact match on purok/address field                 |
| `filter[status]` | Enum   | `ACTIVE`, `INACTIVE`      | Exact match                                        |
| `filter[gender]` | Enum   | `MALE`, `FEMALE`, `OTHER` | Exact match                                        |

**User (`GET /api/v1/admin/users`)**:

| Filter Field     | Type   | Values / Format                          | Notes                                                      |
| ---------------- | ------ | ---------------------------------------- | ---------------------------------------------------------- |
| `filter[role]`   | Enum   | `ADMIN`, `CLERK`, `APPROVER`, `RESIDENT` | Exact match                                                |
| `filter[status]` | Enum   | `ACTIVE`, `INACTIVE`, `LOCKED`           | Exact match                                                |
| `filter[search]` | String | Free text                                | Case-insensitive substring on firstName + lastName + email |

### 4.3 Client-Controlled Sorting (Priority: High)

Each in-scope list endpoint must accept a `sort` query parameter in the format `sort=fieldName,direction`.

- `direction` is either `asc` or `desc` (case-insensitive).
- If `sort` is absent, the endpoint applies the current server-defined default sort (backward-compatible).
- The system validates the sort field against a per-entity safelist of sortable fields.
- The system validates the direction is `asc` or `desc`; any other value returns `400 Bad Request`.
- If an unknown sort field is provided, the system returns `400 Bad Request` with the list of allowed sort fields.
- Only one sort field is supported per request (multi-column sort is out of scope).

**Per-Entity Allowed Sort Fields:**

| Entity           | Allowed Sort Fields                           | Default Sort     |
| ---------------- | --------------------------------------------- | ---------------- |
| ClearanceRequest | `createdAt`, `updatedAt`, `issuedAt`          | `createdAt,desc` |
| Resident         | `lastName`, `firstName`, `createdAt`          | `lastName,asc`   |
| User             | `lastName`, `firstName`, `createdAt`, `email` | `lastName,asc`   |

### 4.4 Pagination (Priority: High)

Existing pagination behavior is retained and standardized:

- `page` — zero-indexed page number (default: `0`).
- `size` — records per page (default: `20`, max: `100`).
- If `size` exceeds `100`, the system returns `400 Bad Request`.
- Response always includes `PageResponse<T>` wrapper with `content`, `totalElements`, `totalPages`, `page`, `size`.

### 4.5 Strict Validation & Error Responses (Priority: High)

- All validation errors return `400 Bad Request` with the standard `ErrorResponse` format.
- Error message specifies which field was invalid and lists all allowed values/fields.
- The `details` field of `ErrorResponse` contains an array of individual validation failures when multiple filters are invalid simultaneously.
- No silent data loss: invalid filters are never silently ignored.

### 4.6 Frontend FilterBuilder Component (Priority: High)

A reusable `FilterBuilder` component must be provided for all list pages:

- Renders the appropriate filter controls for each entity (based on hardcoded filter metadata constants).
- Filter types: text input (for string fields), single-select dropdown (for enum fields), date range picker (for date range pairs).
- Applying filters triggers an immediate React Query refetch and resets the page to `0`.
- A "Clear all filters" action resets all filters and reverts to default sort.
- Active filters are visually indicated (e.g., highlight, badge count).
- Filter state is reflected in the URL query string (`?filter[status]=APPROVED&sort=createdAt,desc`) for bookmarkability and browser back/forward support.

### 4.7 Frontend SortFacet Component (Priority: High)

A reusable `SortFacet` component must be provided for all list pages:

- Renders a sort field selector and direction toggle (asc/desc arrow button).
- Only exposes allowed sort fields for the current entity.
- Changing sort field or direction triggers immediate React Query refetch.
- Active sort is visually indicated.

### 4.8 Query Hook Updates (Priority: High)

The following React Query hooks must be updated to accept and pass filter/sort params:

- `useClearances(filters, sort, page, size)` — staff clearance list.
- `usePortalClearances(filters, sort, page, size)` — resident portal list.
- `useResidents(filters, sort, page, size)` — staff resident list.
- `useUsers(filters, sort, page, size)` — admin user list.

---

## 5. User Experience

### 5.1 Entry Points

The `FilterBuilder` and `SortFacet` components appear in the page header area of all four list pages:

- `/backoffice/clearances` — Clearance Queue (staff)
- `/portal/my-clearances` — My Clearances (resident)
- `/backoffice/residents` — Residents (staff)
- `/backoffice/users` — Users (admin)

The filters are collapsed by default on mobile, expanded on desktop. A "Filter" button with a badge count reveals the filter panel on mobile.

### 5.2 Core Experience — Filtering a Clearance List

1. Clerk opens `/backoffice/clearances`. Default view: all clearances sorted by `createdAt` descending.
2. Clerk opens filter panel → selects **Status = FOR_APPROVAL** from dropdown.
3. System fires `GET /api/v1/clearances?filter[status]=FOR_APPROVAL&sort=createdAt,desc&page=0&size=20`.
4. List updates instantly (React Query refetch). Pagination reflects filtered total count.
5. Clerk further selects **Sort = createdAt, asc** to work oldest-first.
6. System fires `GET /api/v1/clearances?filter[status]=FOR_APPROVAL&sort=createdAt,asc&page=0&size=20`.
7. Clerk bookmarks the URL — on revisit, filters are restored from URL params.

### 5.3 Core Experience — Sorting a Resident List

1. Admin opens `/backoffice/residents`. Default: sorted by `lastName asc`.
2. Admin clicks `SortFacet` → selects "Registration Date" → direction "desc".
3. System fires `GET /api/v1/residents?sort=createdAt,desc&page=0&size=20`.
4. List reorders immediately.

### 5.4 Edge Cases

- **No results**: Empty `content: []` returned. Frontend renders "No records found" empty state with a "Clear filters" CTA.
- **Filter + sort combination invalid**: Server returns `400` — frontend displays field-level error toasts (not a full page error).
- **Page out of bounds**: If `page` exceeds `totalPages - 1`, server returns `400`. Frontend auto-resets to page `0`.
- **Resident portal with residentId filter**: `filter[residentId]` is not in the portal allowed safelist — returns `400`. JWT scope always overrides.
- **URL param tampering**: Users who manually edit the URL with an invalid filter field receive a `400` error displayed as a toast notification.
- **Large result sets**: `size` capped at `100` — requests above this return `400`.

### 5.5 Error States and Messaging

| Error Condition        | API Response                                                                            | Frontend Display     |
| ---------------------- | --------------------------------------------------------------------------------------- | -------------------- |
| Unknown filter field   | `400` + `"Invalid filter field: 'staus'. Allowed: status, paymentStatus, ..."`          | Toast: error message |
| Invalid enum value     | `400` + `"Invalid value for filter[status]: 'DONE'. Allowed: DRAFT, FOR_APPROVAL, ..."` | Toast: error message |
| Invalid date format    | `400` + `"Invalid date format for filter[createdAtFrom]. Expected: YYYY-MM-DD"`         | Toast: error message |
| Invalid sort field     | `400` + `"Invalid sort field: 'name'. Allowed: createdAt, updatedAt, issuedAt"`         | Toast: error message |
| Invalid sort direction | `400` + `"Invalid sort direction: 'random'. Allowed: asc, desc"`                        | Toast: error message |
| Size exceeds max       | `400` + `"Page size 200 exceeds maximum allowed size of 100"`                           | Toast: error message |

---

## 6. Non-Functional Requirements

### 6.1 Performance Requirements

- **API Response Time**: p95 < 300ms for filtered/sorted list queries on datasets up to 10,000 records per entity.
- **Filter parsing overhead**: < 5ms additional overhead vs. unfiltered equivalent query.
- **Frontend filter apply**: React Query refetch initiates within 100ms of user interaction.
- **No N+1 queries**: Filter/sort specifications must not introduce N+1 fetch patterns. All conditions applied at query level.

### 6.2 Scalability Requirements

- Framework must scale to all three modules without per-module code duplication.
- Adding filter support to a new endpoint must require only: defining the allowed field safelist + wiring one `FilterRequestDTO` parameter.
- Support up to 10 simultaneous filter/sort params per request without performance degradation.

### 6.3 Reliability & Availability

- Filter/sort logic is stateless; no additional shared state introduced.
- All changes are backward-compatible — no existing params removed.
- Existing clients using current param names continue to work.

### 6.4 Security Requirements

- **Safelist enforcement**: Filter field names and sort fields are validated against a server-side whitelist. Users cannot reference arbitrary entity fields or nested joins, preventing schema probing and SQL injection via field names.
- **JPA parameterized queries**: All filter values are passed as JPA parameters (never string-concatenated). `SpecificationBuilder` already ensures this — must be maintained for all new filter predicates.
- **Role enforcement unchanged**: Filter parameters do not bypass existing role-based access. Resident portal `listForResident()` always applies JWT-scoped `residentId` regardless of filter params.
- **Input length limits**: String filter values capped at 255 characters; values exceeding this return `400`.

### 6.5 Maintainability

- **Shared infrastructure**: `FilterRequestDTO`, `SortRequestDTO`, and `FilterMetadata` in `shared/filter/` — no per-module duplication.
- **Entity-specific config**: Each module defines its allowed fields as a static constant. Changes require only updating this constant.
- **Backward compatibility**: Existing specific query params (e.g., `q=`, `purok=`) migrated to `filter[*]` convention; old params supported during transition.
- **Code coverage**: Filter/sort parsing and specification composition must have ≥ 85% unit test coverage.

### 6.6 Usability Requirements

- FilterBuilder is fully responsive: works on screens from 375px (mobile) to 1920px (desktop).
- Active filter count badge visible at all times so users know filters are applied.
- URL reflects current filter+sort state for bookmarkability.
- "Clear all filters" is always accessible — one-click to reset to defaults.

### 6.7 Compatibility

- Existing API clients are unaffected. No current query params are removed.
- `PageResponse<T>` response wrapper structure is unchanged.
- All Swagger/OpenAPI docs updated with `@Parameter` annotations for new filter/sort params.

---

## 7. High-Level Implementation Estimates

### 7.1 Estimation Methodology

- Single backend developer working 6–8 productive hours/day.
- Includes design, development, unit tests, Swagger docs, code review.
- Excludes: QA, deployment, meetings.
- Buffer: 20% contingency included in totals.

### 7.2 Development Phases

#### Phase 1: Backend Foundation (4–5 days)

- **Shared filter infrastructure** (1.5 days)
  - Create `FilterRequestDTO` — parse `filter[field]=value` map from `@RequestParam` (0.5 day)
  - Create `SortRequestDTO` — parse `sort=field,direction` with validation (0.5 day)
  - Create `FilterMetadata` — per-entity safelist registry with validation logic (0.5 day)

- **Clearance list update** (1 day)
  - Update `ClearanceController.list()` — add `FilterRequestDTO` + `SortRequestDTO` params
  - Update `ClearanceService.list()` — apply `FilterRequestDTO` via `SpecificationBuilder`
  - Update `PortalClearanceController.listMyClearances()` — filter/sort, JWT scope preserved
  - Unit tests: `ClearanceServiceListFilterTest`

- **Resident list update** (0.75 day)
  - Update `ResidentController.search()` — migrate `q`, `purok` to `filter[*]`; add `filter[status]`, `filter[gender]`, `SortRequestDTO`
  - Replace custom `@Query` with `SpecificationBuilder` in `ResidentService.search()`
  - Backward-compatible alias for old `q=`, `purok=` params
  - Unit tests: `ResidentServiceListFilterTest`

- **User list update** (0.75 day)
  - Update `AdminUserController.list()` — add `FilterRequestDTO` + `SortRequestDTO`
  - Update `UserService.list()` — apply `SpecificationBuilder`
  - Unit tests: `UserServiceListFilterTest`

#### Phase 2: Frontend Components (3–4 days)

- **Shared components** (1.5 days)
  - Create `FilterBuilder.tsx` — entity-agnostic; accepts `FilterConfig[]` prop
  - Create `SortFacet.tsx` — field selector + direction toggle
  - Create `useFilter.ts` hook — manages filter state, URL sync via `useSearchParams`
  - Create `frontend/src/constants/filterMetadata.ts` — hardcoded filter field configs per entity

- **Query hooks update** (0.5 day)
  - Update `useClearances`, `usePortalClearances`, `useResidents`, `useUsers` to accept/pass filter/sort params

- **List page integration** (1 day)
  - Wire `FilterBuilder` + `SortFacet` into all 4 list pages

- **Error handling** (0.5 day)
  - API `400` responses display as toast notifications with the server's error message

#### Phase 3: Testing & Documentation (1–2 days)

- Integration tests: each filtered endpoint with valid + invalid params (0.5 day)
- Frontend component tests: FilterBuilder renders correct controls per entity config (0.5 day)
- Swagger/OpenAPI annotation updates for all 4 endpoints (0.5 day)

### 7.3 Total Effort Summary

| Phase                        | Estimated Days | Cumulative   |
| ---------------------------- | -------------- | ------------ |
| Phase 1: Backend Foundation  | 4–5            | 4–5          |
| Phase 2: Frontend Components | 3–4            | 7–9          |
| Phase 3: Testing & Docs      | 1–2            | 8–11         |
| **Total**                    | **8–11 days**  | **~2 weeks** |

### 7.4 Effort by Role

| Role                    | Days     |
| ----------------------- | -------- |
| Backend Development     | 5–6 days |
| Frontend Development    | 3–4 days |
| Testing + Documentation | 1–2 days |

### 7.5 Parallel Work Opportunities

- Phase 1 (backend) and Phase 2 frontend constants/hooks can begin in parallel once the API contract is agreed.
- `FilterBuilder` and `SortFacet` components can be built against mock data while backend is in progress.

---

## 8. Success Metrics

### 8.1 Technical Metrics (at launch)

- All 4 list endpoints return correct filtered/sorted results for all valid filter combinations.
- All 4 endpoints return `400` with correct error body for all invalid filter/sort combinations (see §5.5).
- Zero regression in existing list endpoint behavior for clients not using new filter params.
- ≥ 85% unit test coverage for `FilterRequestDTO`, `SortRequestDTO`, `FilterMetadata` classes.
- All 4 endpoints documented in Swagger with `@Parameter` on every filter/sort param.

### 8.2 UX Metrics (1 month post-launch)

- Clerk reports < 30 seconds to locate a specific clearance record using filters (down from ~2 minutes of manual scrolling).
- Zero usability bug reports related to filter state being lost on navigation.
- URL sharing of filtered views works correctly across browser sessions.

### 8.3 Quality Metrics

- Zero `500` errors caused by filter/sort framework after launch.
- `400` errors for invalid filter params display user-friendly messages (validated via QA session).
- Backend p95 response time for filtered queries ≤ 300ms on staging dataset.

---

## 9. User Stories

### US-001: Filter Clearances by Status

- **ID**: FSP-001
- **Description**: As a clerk, I want to filter the clearance list by status so that I can focus on requests that need my attention.
- **Acceptance Criteria**:
  - `GET /api/v1/clearances?filter[status]=FOR_APPROVAL` returns only `FOR_APPROVAL` records.
  - `filter[status]=INVALID` returns `400` with message listing valid enum values.
  - Combining `filter[status]=APPROVED&filter[paymentStatus]=UNPAID` returns only records matching both.
  - Response `PageResponse` reflects filtered `totalElements`.

### US-002: Filter Clearances by Date Range

- **ID**: FSP-002
- **Description**: As an approver, I want to filter clearances by creation date range so that I can review requests from a specific period.
- **Acceptance Criteria**:
  - `filter[createdAtFrom]=2025-01-01&filter[createdAtTo]=2025-01-31` returns records with `createdAt` within January 2025 (inclusive).
  - Single-sided range `filter[createdAtFrom]=2025-01-01` (no upper bound) returns all records from that date onward.
  - Invalid date format `filter[createdAtFrom]=01-2025-01` returns `400` with expected format message.

### US-003: Sort Clearances by Creation Date

- **ID**: FSP-003
- **Description**: As a clerk, I want to sort clearances from oldest to newest so that I can process them in order of submission.
- **Acceptance Criteria**:
  - `sort=createdAt,asc` returns records in ascending `createdAt` order.
  - Default sort (no `sort` param) behaves as `createdAt,desc` (existing behavior preserved).
  - `sort=InvalidField,asc` returns `400` listing allowed sort fields.
  - `sort=createdAt,sideways` returns `400` with message listing `asc` and `desc`.

### US-004: Filter Resident List by Name Search

- **ID**: FSP-004
- **Description**: As staff, I want to search residents by name so that I can quickly find a specific person.
- **Acceptance Criteria**:
  - `filter[q]=santos` returns all residents whose firstName or lastName contains "santos" (case-insensitive).
  - Combined `filter[q]=santos&filter[status]=ACTIVE` returns active residents with "santos" in their name.
  - `filter[q]` with only whitespace is treated as absent (no filter applied).
  - String filter values exceeding 255 characters return `400`.

### US-005: Filter Residents by Status

- **ID**: FSP-005
- **Description**: As an admin, I want to filter the resident list by status so that I can audit inactive accounts.
- **Acceptance Criteria**:
  - `filter[status]=INACTIVE` returns only inactive residents.
  - `filter[status]=UNKNOWN` returns `400` with allowed values.

### US-006: Sort Residents Alphabetically

- **ID**: FSP-006
- **Description**: As staff, I want to sort the resident list alphabetically by last name so I can quickly scan for a specific person.
- **Acceptance Criteria**:
  - `sort=lastName,asc` returns residents in A–Z order by last name.
  - Default sort (no `sort` param) behaves as `lastName,asc` (existing behavior preserved).
  - `sort=lastName,desc` returns Z–A order.

### US-007: Filter Users by Role

- **ID**: FSP-007
- **Description**: As an admin, I want to filter the user list by role so that I can manage staff accounts separately from residents.
- **Acceptance Criteria**:
  - `GET /api/v1/admin/users?filter[role]=CLERK` returns only users with CLERK role.
  - `filter[role]=SUPERADMIN` returns `400` with allowed role values.
  - Roles: `ADMIN`, `CLERK`, `APPROVER`, `RESIDENT`.

### US-008: Filter Users by Status

- **ID**: FSP-008
- **Description**: As an admin, I want to filter users by account status to find locked or inactive accounts.
- **Acceptance Criteria**:
  - `filter[status]=LOCKED` returns only locked user accounts.
  - `filter[role]=RESIDENT&filter[status]=ACTIVE` returns active resident-role users.

### US-009: Search Users by Name/Email

- **ID**: FSP-009
- **Description**: As an admin, I want to search users by name or email for quick lookup.
- **Acceptance Criteria**:
  - `filter[search]=juan` returns users with "juan" in firstName, lastName, or email (case-insensitive).
  - Combined with `filter[role]=CLERK` narrows to matching clerks only.

### US-010: Resident Portal — Filter My Clearances

- **ID**: FSP-010
- **Description**: As a resident, I want to filter my clearance history by status so that I can find recently approved requests.
- **Acceptance Criteria**:
  - `GET /api/v1/portal/clearances?filter[status]=APPROVED` returns only the authenticated resident's APPROVED clearances.
  - Resident cannot see other residents' clearances regardless of filter params — JWT scope applies always.
  - `filter[residentId]` is not in the portal allowed safelist — returns `400` if supplied.

### US-011: Resident Portal — Sort My Clearances

- **ID**: FSP-011
- **Description**: As a resident, I want to sort my clearance requests by date so that I can find the most recent one easily.
- **Acceptance Criteria**:
  - `sort=createdAt,desc` returns newest clearances first.
  - `sort=createdAt,asc` returns oldest first.
  - Default sort is `createdAt,desc`.

### US-012: Invalid Filter — Clear Error Message

- **ID**: FSP-012
- **Description**: As any API user, I want a clear error message when I provide an unknown filter field so that I can correct it immediately.
- **Acceptance Criteria**:
  - `filter[badField]=value` returns `400` with body: `{ "error": "Invalid filter field: 'badField'. Allowed fields: status, paymentStatus, ..." }`.
  - Frontend displays this message in a toast notification.
  - Multiple invalid filter fields in one request return all violations in the `details` array.

### US-013: Pagination Caps Respect Size Maximum

- **ID**: FSP-013
- **Description**: As a developer consuming the API, I want the system to return an error if I request too many items per page, preventing memory issues.
- **Acceptance Criteria**:
  - `size=100` is allowed.
  - `size=101` returns `400` with message: "Page size 101 exceeds maximum allowed size of 100."
  - `size=0` returns `400` with message about minimum size.

### US-014: Filter State Persists in URL

- **ID**: FSP-014
- **Description**: As a staff user, I want my active filters to be reflected in the browser URL so that I can share or bookmark a filtered view.
- **Acceptance Criteria**:
  - Applying `filter[status]=FOR_APPROVAL` updates URL to `?filter[status]=FOR_APPROVAL`.
  - Reloading the page or sharing the URL restores the same filtered list.
  - Clicking browser back/forward navigates filter states correctly.

### US-015: Clear All Filters

- **ID**: FSP-015
- **Description**: As a staff user, I want a one-click "Clear all filters" button so that I can return to the default unfiltered view.
- **Acceptance Criteria**:
  - Clicking "Clear all filters" removes all active `filter[*]` params from the URL.
  - List resets to page 0 with default sort.
  - Active filter badge count resets to 0.

---

# ENGINEERING DESIGN DOCUMENTATION

## 10. Technology Stack

### 10.1 Backend Technologies

- **Language**: Java 21 — existing project language; virtual threads available if needed.
- **Framework**: Spring Boot 3.3.4 — existing; Spring Data JPA + `JpaSpecificationExecutor` already in use.
- **ORM / Data Access**: Spring Data JPA with `SpecificationBuilder` utility (existing `shared/util/SpecificationBuilder.java`) — extended for metadata-driven validation.
- **Validation**: Jakarta Bean Validation (part of Spring Boot starter) — used for `page`, `size` bounds validation via `@Min` / `@Max`.
- **API Documentation**: SpringDoc OpenAPI (existing) — add `@Parameter` annotations to new query params.
- **No new dependencies added.**

### 10.2 Frontend Technologies

- **Framework**: Next.js 14 / TypeScript — existing.
- **UI Components**: shadcn/ui — existing component library; `Select`, `Input`, `DatePicker`, `Badge`, `Button` used in FilterBuilder.
- **State Management**: `useFilter.ts` custom hook backed by `useSearchParams` (Next.js 14). React Hook Form is not used here.
- **Data Fetching**: TanStack React Query — existing; updated hooks pass filter/sort params as query keys.
- **HTTP Client**: Axios via `frontend/src/lib/api.ts` — existing; no changes needed.
- **No new npm packages added.**

---

## 11. System Architecture

### 11.1 High-Level Architecture (unchanged)

```
┌──────────────────────────┐
│   Next.js Frontend SPA   │
│  FilterBuilder /SortFacet│
│  useFilter hook + hooks  │
└────────────┬─────────────┘
             │ HTTPS REST (filter[*] + sort params)
             ▼
┌──────────────────────────┐
│  Spring Boot Backend     │
│  Controllers             │
│  FilterRequestDTO        │
│  SortRequestDTO          │
│  FilterMetadata (safelist)│
│  SpecificationBuilder    │
│  Services + Repositories │
└────────────┬─────────────┘
             │ JPA + JDBC
             ▼
┌──────────────────────────┐
│       PostgreSQL         │
└──────────────────────────┘
```

### 11.2 New Shared Components (`shared/filter/`)

```
com.barangay.clearance.shared.filter/
├── FilterMetadata       — Per-entity allowed filter + sort field registry (static constants)
├── FilterRequestDTO     — Parse and validate filter[field]=value params against safelist
└── SortRequestDTO       — Parse and validate sort=field,direction against safelist
```

These are shared utilities consumed by all three module services. No cross-module JPA relationships are introduced.

### 11.3 Communication Pattern (List Requests)

```
User interaction
  → FilterBuilder/SortFacet state change
  → useFilter hook updates URL params
  → React Query detects new query key (includes filters/sort)
  → api.get('/clearances', { params: {...filters, sort, page, size} })
  → Controller receives @RequestParam Map<String,String> filterParams + sort
  → FilterRequestDTO.parse(filterParams, ClearanceFilterMetadata) → validated predicates
  → SortRequestDTO.parse(sort, ClearanceFilterMetadata) → Sort object
  → ClearanceService.list(filterSpec, sort, pageable)
  → SpecificationBuilder composes JPA Specification from predicates
  → ClearanceRequestRepository.findAll(spec, pageable)
  → PageResponse<ClearanceRequestDTO> returned
  → React Query updates cache, list re-renders
```

### 11.4 Design Patterns

- **Specification Pattern**: JPA `Specification<T>` composed dynamically from validated filter predicates. Already used in `ClearanceService`.
- **Safelist/Allowlist**: `FilterMetadata` acts as the whitelist per entity — field name string → field type + JPA path.
- **DTO for request parsing**: `FilterRequestDTO` / `SortRequestDTO` are request-scoped value objects — never leak raw strings into service or repository layers.
- **Convention over configuration**: Same `filter[field]=value` pattern across all list endpoints — frontend code and backend parsing logic are generic.

---

## 12. Data Models

### 12.1 No Schema Changes

This feature introduces no new database tables, columns, or migrations. All filtering, sorting, and pagination is handled at the application/JPA layer using existing columns.

### 12.2 Filterable / Sortable Columns (Existing)

**clearance_requests table** (filterable columns):

- `status` — VARCHAR enum column
- `payment_status` — VARCHAR enum column
- `created_at` — TIMESTAMP WITH TIME ZONE
- `resident_id` — UUID FK

**residents table** (filterable columns):

- `first_name`, `last_name` — VARCHAR, substring search via LOWER() + LIKE
- `purok` — VARCHAR, exact match
- `status` — VARCHAR enum
- `gender` — VARCHAR enum

**users table** (filterable columns):

- `first_name`, `last_name`, `email` — VARCHAR, substring search
- `role` — VARCHAR enum
- `status` — VARCHAR enum

### 12.3 Database Indexing (Deferred)

The following indexes are identified as high-value for Phase 3 performance tuning but are **not included in this phase**:

| Table                | Column(s)               | Index Type       | Rationale                  |
| -------------------- | ----------------------- | ---------------- | -------------------------- |
| `clearance_requests` | `status`                | B-tree           | High-cardinality filter    |
| `clearance_requests` | `created_at`            | B-tree           | Date range sorts/filters   |
| `clearance_requests` | `payment_status`        | B-tree           | Common combined filter     |
| `residents`          | `last_name, first_name` | B-tree           | Default sort + name search |
| `users`              | `role, status`          | B-tree composite | Common combined filter     |

---

## 13. API Design

### 13.1 API Conventions

- **Base URL**: `http://localhost:8080/api/v1`
- **Authentication**: Bearer JWT in `Authorization` header (existing).
- **Filter params**: `filter[fieldName]=value` (bracket notation in query string).
- **Sort param**: `sort=fieldName,direction` (comma-separated; direction is `asc` or `desc`).
- **Pagination params**: `page` (0-indexed, default 0), `size` (default 20, max 100).
- **Error format**: existing `ErrorResponse { status, error, message, timestamp, path, details }`.

### 13.2 Clearance List Endpoint

#### GET /api/v1/clearances

**Purpose**: List all clearance requests (staff view) with optional filtering, sorting, and pagination.

**Roles**: `ADMIN`, `CLERK`, `APPROVER`

| Parameter               | Type    | Required | Default          | Description                                                 |
| ----------------------- | ------- | -------- | ---------------- | ----------------------------------------------------------- |
| `filter[status]`        | Enum    | No       | —                | `DRAFT`, `FOR_APPROVAL`, `APPROVED`, `REJECTED`, `RELEASED` |
| `filter[paymentStatus]` | Enum    | No       | —                | `UNPAID`, `PAID`, `WAIVED`                                  |
| `filter[createdAtFrom]` | String  | No       | —                | ISO-8601 date `YYYY-MM-DD`                                  |
| `filter[createdAtTo]`   | String  | No       | —                | ISO-8601 date `YYYY-MM-DD`                                  |
| `filter[residentId]`    | UUID    | No       | —                | Filter by resident UUID                                     |
| `sort`                  | String  | No       | `createdAt,desc` | Format: `field,direction`                                   |
| `page`                  | Integer | No       | `0`              | Zero-indexed page                                           |
| `size`                  | Integer | No       | `20`             | Max 100                                                     |

**Request Example**:

```
GET /api/v1/clearances?filter[status]=APPROVED&filter[paymentStatus]=UNPAID&sort=createdAt,asc&page=0&size=20
Authorization: Bearer <token>
```

**Response (200 OK)**:

```json
{
  "content": [
    {
      "id": "uuid",
      "clearanceNumber": "2025-010001",
      "status": "APPROVED",
      "paymentStatus": "UNPAID",
      "createdAt": "2025-01-15T08:30:00Z"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

**Error Responses**:

- `400 Bad Request` — Invalid filter field, invalid enum value, invalid date format, invalid sort field/direction, size > 100.
- `401 Unauthorized` — Missing or expired JWT.
- `403 Forbidden` — `RESIDENT` role attempting to access staff endpoint.

#### GET /api/v1/portal/clearances

**Purpose**: List the authenticated resident's own clearance requests.

**Roles**: `RESIDENT`

**Key difference from staff endpoint**: `filter[residentId]` is **not** in the portal allowed safelist — returns `400` if supplied. JWT scope always overrides; the resident can only ever see their own records.

| Parameter        | Type    | Required | Default          | Description                                                 |
| ---------------- | ------- | -------- | ---------------- | ----------------------------------------------------------- |
| `filter[status]` | Enum    | No       | —                | `DRAFT`, `FOR_APPROVAL`, `APPROVED`, `REJECTED`, `RELEASED` |
| `sort`           | String  | No       | `createdAt,desc` | Format: `field,direction`                                   |
| `page`           | Integer | No       | `0`              | Zero-indexed page                                           |
| `size`           | Integer | No       | `20`             | Max 100                                                     |

---

### 13.3 Resident List Endpoint

#### GET /api/v1/residents

**Purpose**: Search and list residents with optional filtering and sorting.

**Roles**: `ADMIN`, `CLERK`, `APPROVER`

**Migration Note**: Existing `?q=value&purok=value` params are aliased to `filter[q]`/`filter[purok]` for backward compatibility during transition.

| Parameter        | Type    | Required | Default        | Description                                     |
| ---------------- | ------- | -------- | -------------- | ----------------------------------------------- |
| `filter[q]`      | String  | No       | —              | Case-insensitive firstName + lastName substring |
| `filter[purok]`  | String  | No       | —              | Exact purok/address match                       |
| `filter[status]` | Enum    | No       | —              | `ACTIVE`, `INACTIVE`                            |
| `filter[gender]` | Enum    | No       | —              | `MALE`, `FEMALE`, `OTHER`                       |
| `sort`           | String  | No       | `lastName,asc` | Format: `field,direction`                       |
| `page`           | Integer | No       | `0`            | Zero-indexed page                               |
| `size`           | Integer | No       | `20`           | Max 100                                         |

---

### 13.4 User List Endpoint

#### GET /api/v1/admin/users

**Purpose**: List system users with filtering and sorting for admin management.

**Roles**: `ADMIN`

| Parameter        | Type    | Required | Default        | Description                                              |
| ---------------- | ------- | -------- | -------------- | -------------------------------------------------------- |
| `filter[role]`   | Enum    | No       | —              | `ADMIN`, `CLERK`, `APPROVER`, `RESIDENT`                 |
| `filter[status]` | Enum    | No       | —              | `ACTIVE`, `INACTIVE`, `LOCKED`                           |
| `filter[search]` | String  | No       | —              | Case-insensitive substring on firstName, lastName, email |
| `sort`           | String  | No       | `lastName,asc` | Format: `field,direction`                                |
| `page`           | Integer | No       | `0`            | Zero-indexed page                                        |
| `size`           | Integer | No       | `20`           | Max 100                                                  |

---

### 13.5 Standard Error Response Format

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid filter field: 'badField'. Allowed fields for clearance: status, paymentStatus, createdAtFrom, createdAtTo, residentId",
  "timestamp": "2026-03-07T08:00:00Z",
  "path": "/api/v1/clearances",
  "details": []
}
```

Multiple violations:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid filter parameters",
  "timestamp": "2026-03-07T08:00:00Z",
  "path": "/api/v1/clearances",
  "details": [
    "Invalid filter field: 'badField'",
    "Invalid value for filter[status]: 'DONE'. Allowed: DRAFT, FOR_APPROVAL, APPROVED, REJECTED, RELEASED"
  ]
}
```

---

## 14. Security Architecture

### 14.1 Filter Field Safelist (Injection Prevention)

The primary security concern for dynamic filtering is that client-controlled field names could be used to probe the database schema or craft malicious queries. This is mitigated by:

1. `FilterMetadata` per entity defines a **closed set of allowed field names** as Java string constants mapping to typed `SingularAttribute<T>` (JPA metamodel) references.
2. `FilterRequestDTO.parse()` validates every `filter[key]` against this closed set before any JPA interaction. Unknown fields throw `AppException(400)` immediately — no query is executed.
3. **Field names are never string-interpolated** into JPQL or SQL. The JPA metamodel attribute reference is used, ensuring compile-time type safety.
4. Filter values are passed as JPA query parameters (`CriteriaBuilder.parameter()`) — standard parameterized query protection against SQL injection in values.

### 14.2 Authorization Unchanged

- All endpoints remain behind `JwtAuthFilter`. Filtering does not affect the authentication layer.
- `/portal/clearances` still enforces `residentId = JWT.userId` in `listForResident()` — this scope is applied **after** filter parsing and overrides any attempt to bypass it.
- `/admin/users` remains `@PreAuthorize("hasRole('ADMIN')")`.

### 14.3 Input Length Limits

All `String` filter values capped at 255 characters in `FilterRequestDTO.parse()`. Values exceeding this limit return `400` before hitting the database.

---

## 15. Infrastructure

### 15.1 No Infrastructure Changes

This feature requires no changes to Docker Compose, Nginx configuration, cloud deployment, or CI/CD pipeline. The changes are entirely within the application code.

### 15.2 Deployment Safety

- All changes are backward-compatible (no removed params, no response structure changes).
- Flyway: no new migration scripts needed.
- Rollback: reverting the controller/service changes is sufficient; no schema rollback needed.

### 15.3 Monitoring Considerations

- Log at `WARN` level when a `400` is returned for filter validation failures (existing `GlobalExceptionHandler` pattern).
- `400` error spikes on filter endpoints may indicate a misbehaving client.

---

## 16. Development Phases (Checklist)

### Phase 1: Backend Foundation

**Shared Infrastructure:**

- [ ] Create `backend/src/main/java/com/barangay/clearance/shared/filter/FilterMetadata.java` — per-entity allowed fields registry
- [ ] Create `backend/src/main/java/com/barangay/clearance/shared/filter/FilterRequestDTO.java` — parse and validate `filter[*]` params
- [ ] Create `backend/src/main/java/com/barangay/clearance/shared/filter/SortRequestDTO.java` — parse and validate `sort=field,direction`
- [ ] Unit tests for `FilterRequestDTO` (valid params, unknown field → 400, invalid enum → 400, invalid date → 400)
- [ ] Unit tests for `SortRequestDTO` (valid sort, unknown field → 400, invalid direction → 400)

**Clearance Module:**

- [ ] Define `ClearanceFilterMetadata` — allowed filter fields + sort fields constants
- [ ] Update `ClearanceController.list()` — add `@RequestParam Map<String,String> filterParams`, `@RequestParam(required=false) String sort`
- [ ] Update `ClearanceService.list()` — accept `FilterRequestDTO`, `SortRequestDTO`, apply via `SpecificationBuilder`
- [ ] Update `PortalClearanceController.listMyClearances()` — add filter/sort params (portal safelist excludes `residentId`)
- [ ] Update `ClearanceService.listForResident()` — same pattern; JWT scope remains primary constraint
- [ ] Update Swagger `@Parameter` annotations on clearance list endpoints
- [ ] Unit tests: `ClearanceServiceListFilterTest`

**Residents Module:**

- [ ] Define `ResidentFilterMetadata` — allowed filter fields + sort fields constants
- [ ] Update `ResidentController.search()` — migrate `q`, `purok` to `filter[q]`, `filter[purok]`; add sort
- [ ] Replace custom `@Query` with `SpecificationBuilder` approach in `ResidentService.search()`
- [ ] Backward-compatible alias for old `q=` and `purok=` params (service layer)
- [ ] Update Swagger `@Parameter` annotations on residents list endpoint
- [ ] Unit tests: `ResidentServiceListFilterTest`

**Identity / Users Module:**

- [ ] Define `UserFilterMetadata` — allowed filter fields + sort fields constants
- [ ] Update or create `AdminUserController.list()` — add filter/sort params
- [ ] Update `UserService.list()` — apply `SpecificationBuilder` for filter spec
- [ ] Update Swagger `@Parameter` annotations on users list endpoint
- [ ] Unit tests: `UserServiceListFilterTest`

### Phase 2: Frontend Components

- [ ] Create `frontend/src/constants/filterMetadata.ts` — hardcoded `FilterConfig[]` per entity
- [ ] Create `frontend/src/hooks/useFilter.ts` — manage filter state + URL sync via `useSearchParams`
- [ ] Create `frontend/src/components/FilterBuilder.tsx` — generic; renders controls from `FilterConfig[]`
- [ ] Create `frontend/src/components/SortFacet.tsx` — field selector + direction toggle
- [ ] Update `frontend/src/hooks/useClearances.ts` — accept `FilterState`, `sort` params
- [ ] Update `frontend/src/hooks/usePortalClearances.ts` (or equivalent) — accept `FilterState`, `sort` params
- [ ] Update `frontend/src/hooks/useResidents.ts` — accept `FilterState`, `sort` params
- [ ] Update `frontend/src/hooks/useUsers.ts` — accept `FilterState`, `sort` params
- [ ] Wire `FilterBuilder` + `SortFacet` into `/backoffice/clearances` list page
- [ ] Wire `FilterBuilder` + `SortFacet` into `/portal/my-clearances` list page
- [ ] Wire `FilterBuilder` + `SortFacet` into `/backoffice/residents` list page
- [ ] Wire `FilterBuilder` + `SortFacet` into `/backoffice/users` list page
- [ ] API `400` errors from filter validation displayed as toast notifications

### Phase 3: Testing & Documentation

- [ ] Integration test: `ClearanceListIntegrationTest` — valid filter combos, invalid filter → 400, sort combos, pagination
- [ ] Integration test: `ResidentListIntegrationTest` — same coverage
- [ ] Integration test: `UserListIntegrationTest` — same coverage
- [ ] Frontend component test: `FilterBuilder.test.tsx` — renders correct controls per config
- [ ] Frontend component test: `SortFacet.test.tsx` — direction toggle + field select
- [ ] Manual QA: URL state persistence across browser navigation
- [ ] Verify Swagger UI shows filter/sort params on all 4 endpoints
- [ ] Update `project_status.md`

---

## 17. Open Questions

- [ ] Should old-style `?q=value&purok=value` params on `/residents` be permanently deprecated in this iteration, or maintained indefinitely? **Recommendation**: Deprecate with `WARN` log; remove in next major phase.
- [ ] Should resident portal `filter[status]` include all clearance statuses or a subset hiding `DRAFT`? **Current assumption**: All statuses visible to resident.
- [ ] Should `sort=createdAt` without `,direction` default to `asc`? **Recommendation**: Yes, default to `asc` if direction is omitted.

---

## 18. Appendices

### 18.1 Glossary

- **Safelist**: A closed set of explicitly allowed values. Filter field names are validated against this list; anything not on the list is rejected.
- **JPA Specification**: A Spring Data JPA abstraction for composing dynamic WHERE clauses using the JPA Criteria API without raw SQL.
- **FilterMetadata**: Per-entity registry that maps allowed filter field name strings to their JPA metamodel attributes and allowed value types.
- **Convention-based params**: URL query parameter naming follows a predictable pattern (`filter[field]=value`, `sort=field,direction`) applied uniformly across all list endpoints.
- **Backward-compatible**: Existing API clients using current param names continue to work without modification.

### 18.2 References

- Existing `SpecificationBuilder`: `backend/src/main/java/com/barangay/clearance/shared/util/SpecificationBuilder.java`
- Existing `PageResponse`: `backend/src/main/java/com/barangay/clearance/shared/util/PageResponse.java`
- Existing `AppException`/`GlobalExceptionHandler`: `backend/src/main/java/com/barangay/clearance/shared/exception/`
- [Spring Data JPA Specifications](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications)
- [JPA Criteria API](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html#a8999)
- [TanStack React Query — Query Keys](https://tanstack.com/query/latest/docs/framework/react/guides/query-keys)
- [Next.js 14 — useSearchParams](https://nextjs.org/docs/app/api-reference/functions/use-search-params)

### 18.3 Version History

| Version | Date          | Author                              | Changes                                 |
| ------- | ------------- | ----------------------------------- | --------------------------------------- |
| 1.0     | March 7, 2026 | Barangay Clearance Engineering Team | Initial PRD — brainstormed and approved |
