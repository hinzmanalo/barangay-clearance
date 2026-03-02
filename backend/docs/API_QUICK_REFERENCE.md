# API Quick Reference — Barangay Clearance System

**Base URL:** `http://localhost:8080/api/v1`  
**Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

## General Conventions

| Convention        | Detail                                                         |
| ----------------- | -------------------------------------------------------------- |
| Auth header       | `Authorization: Bearer <access_token>`                         |
| Content-Type      | `application/json` (except logo upload: `multipart/form-data`) |
| Pagination params | `?page=0&size=20` (0-indexed)                                  |
| Timestamps        | ISO-8601 UTC strings                                           |
| IDs               | UUIDs                                                          |

### Standard Error Response

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Email is required",
  "timestamp": "2026-02-24T10:00:00Z",
  "path": "/api/v1/auth/login",
  "details": {}
}
```

### Paginated Response Wrapper

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "page": 0,
  "size": 20
}
```

---

## Status Key

| Symbol | Meaning                           |
| ------ | --------------------------------- |
| ✅     | Implemented (Phases 0–3 complete) |
| 🔲     | Planned (Phase 4+, not yet built) |

---

## 1. Auth `PUBLIC`

> All `/auth/**` routes are public — no token required.

### POST `/auth/register` ✅

Register a new resident account. Creates both a `User` (status `PENDING_VERIFICATION`) and a linked `Resident` record atomically.

**Request body:**

```json
{
  "email": "juan@example.com",
  "password": "password123",
  "firstName": "Juan",
  "lastName": "dela Cruz"
}
```

**Responses:**

| Code              | Meaning                  |
| ----------------- | ------------------------ |
| `201 Created`     | Account created          |
| `409 Conflict`    | Email already registered |
| `400 Bad Request` | Validation failure       |

---

### POST `/auth/login` ✅

**Request body:**

```json
{
  "email": "juan@example.com",
  "password": "password123"
}
```

**Response `200`:**

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-uuid>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "mustChangePassword": true
}
```

> `mustChangePassword` is `true` for the seeded admin and newly created staff. Redirect to change-password flow before allowing any other action.

| Code               | Meaning                    |
| ------------------ | -------------------------- |
| `200 OK`           | Login successful           |
| `401 Unauthorized` | Invalid credentials        |
| `403 Forbidden`    | Account inactive / pending |

---

### POST `/auth/refresh` ✅

Exchange a valid refresh token for a new access token. Old refresh token is rotated (invalidated).

**Request body:**

```json
{
  "refreshToken": "<opaque-uuid>"
}
```

**Response `200`:** Same structure as `/auth/login` (no `mustChangePassword` field).

| Code               | Meaning                              |
| ------------------ | ------------------------------------ |
| `200 OK`           | New tokens issued                    |
| `401 Unauthorized` | Token expired, revoked, or not found |

---

### POST `/auth/logout` ✅

Revoke a refresh token server-side.

**Request body:**

```json
{
  "refreshToken": "<opaque-uuid>"
}
```

**Response `200 OK`** (empty body)

---

### PUT `/auth/change-password` ✅

🔐 Requires valid `Authorization` header (any role).

**Request body:**

```json
{
  "currentPassword": "oldPassword1",
  "newPassword": "newPassword1"
}
```

**Response `200`:** New `TokenResponse` (re-issued tokens with `mustChangePassword: false`).

| Code              | Meaning                             |
| ----------------- | ----------------------------------- |
| `200 OK`          | Password changed, new tokens issued |
| `400 Bad Request` | Current password incorrect          |

---

## 2. Admin — Users `ADMIN`

> All routes require `ADMIN` role.

### GET `/admin/users` ✅

List all staff accounts (paginated). Does not include residents.

**Query params:** `page`, `size`

**Response `200`:** `PageResponse<UserDTO>`

```json
{
  "content": [
    {
      "id": "uuid",
      "email": "clerk@barangay.gov.ph",
      "firstName": "Maria",
      "lastName": "Santos",
      "role": "CLERK",
      "status": "ACTIVE",
      "mustChangePassword": false,
      "createdAt": "2026-02-01T08:00:00Z"
    }
  ],
  ...
}
```

---

### GET `/admin/users/{id}` ✅

**Response `200`:** Single `UserDTO`

| Code            | Meaning      |
| --------------- | ------------ |
| `200 OK`        | User found   |
| `404 Not Found` | No such user |

---

### POST `/admin/users` ✅

Create a staff account (`CLERK`, `APPROVER`, or `ADMIN`). Sets `mustChangePassword = true`.

**Request body:**

```json
{
  "email": "clerk@barangay.gov.ph",
  "password": "TempPass123",
  "firstName": "Maria",
  "lastName": "Santos",
  "role": "CLERK"
}
```

> Valid `role` values: `CLERK`, `APPROVER`, `ADMIN`

**Response `201 Created`:** `UserDTO`

| Code           | Meaning               |
| -------------- | --------------------- |
| `201 Created`  | Staff account created |
| `409 Conflict` | Email already in use  |

---

### PUT `/admin/users/{id}/deactivate` ✅

Deactivate a user account. Deactivated users cannot log in.

**Response `200`:** Updated `UserDTO` with `status: "INACTIVE"`

| Code            | Meaning          |
| --------------- | ---------------- |
| `200 OK`        | User deactivated |
| `404 Not Found` | No such user     |

---

## 3. Residents `CLERK, ADMIN`

> All routes require `CLERK` or `ADMIN` role unless noted.

### GET `/residents` ✅

Search the resident registry. Supports full-text search and purok filter.

> ⚠️ **Known bug:** when `q` is `null`, Hibernate infers the parameter as `bytea` instead of `text`, causing `ERROR: function lower(bytea) does not exist`. Workaround: always pass an empty string or use explicit `CAST`. Fix tracked in blockers.

**Query params:**

| Param   | Type     | Description                    |
| ------- | -------- | ------------------------------ |
| `q`     | `string` | Name search (case-insensitive) |
| `purok` | `string` | Purok/Zone filter              |
| `page`  | `int`    | Default `0`                    |
| `size`  | `int`    | Default `20`                   |

**Response `200`:** `PageResponse<ResidentDTO>`

```json
{
  "content": [
    {
      "id": "uuid",
      "firstName": "Juan",
      "lastName": "dela Cruz",
      "birthdate": "1990-05-15",
      "address": "123 Rizal St",
      "purokZone": "Purok 3",
      "contactNumber": "09171234567",
      "userId": "uuid or null",
      "hasPortalAccount": true,
      "createdAt": "2026-02-01T08:00:00Z"
    }
  ],
  ...
}
```

---

### POST `/residents` ✅

Create a walk-in resident record (no portal account).

**Request body:**

```json
{
  "firstName": "Pedro",
  "lastName": "Reyes",
  "birthdate": "1985-03-20",
  "address": "456 Mabini Ave",
  "purokZone": "Purok 5",
  "contactNumber": "09181234567"
}
```

**Response `201 Created`:** `ResidentDTO`

| Code           | Meaning                         |
| -------------- | ------------------------------- |
| `201 Created`  | Resident created                |
| `409 Conflict` | Linked email already registered |

---

### GET `/residents/{id}` ✅

**Response `200`:** Single `ResidentDTO`

---

### PATCH `/residents/{id}` ✅

Partially update resident details. Only fields present in the request body are applied; absent fields are left unchanged.

**Request body:** Same fields as `POST /residents` (all optional — partial update).

**Response `200`:** Updated `ResidentDTO`

---

### GET `/residents/pending-users` ✅

List residents whose linked portal account has `status = PENDING_VERIFICATION`. Used by clerks to review new registrations.

**Response `200`:** `PageResponse<ResidentDTO>`

---

### PATCH `/residents/{id}/activate-user` ✅

Activate a pending resident portal account. Sets `user.status = ACTIVE`.

**Response `200`:** Updated `ResidentDTO`

| Code              | Meaning                                      |
| ----------------- | -------------------------------------------- |
| `200 OK`          | Account activated                            |
| `404 Not Found`   | Resident not found                           |
| `400 Bad Request` | Account not in `PENDING_VERIFICATION` status |

---

### PATCH `/residents/{id}/reject-user` ✅

Reject a pending registration. Sets `user.status = REJECTED`. Resident record is preserved.

**Response `200`:** Updated `ResidentDTO`

---

## 4. Clearances — Back-office `CLERK, APPROVER, ADMIN`

### GET `/clearances` ✅

List all clearance requests with optional filters. Filters are combined with AND; omitted params match all values.

**Query params:**

| Param           | Type              | Values                                             |
| --------------- | ----------------- | -------------------------------------------------- |
| `status`        | `string`          | `FOR_APPROVAL`, `APPROVED`, `REJECTED`, `RELEASED` |
| `paymentStatus` | `string`          | `UNPAID`, `PAID`                                   |
| `from`          | `ISO-8601 string` | Inclusive start of `createdAt` range               |
| `to`            | `ISO-8601 string` | Inclusive end of `createdAt` range                 |
| `page`          | `int`             | Default `0`                                        |
| `size`          | `int`             | Default `20`                                       |

**Response `200`:** `PageResponse<ClearanceRequestDTO>`

---

### POST `/clearances` ✅

Create a walk-in clearance request. Immediately enters `FOR_APPROVAL` state.

**Roles:** `CLERK`, `ADMIN`

**Request body:**

```json
{
  "residentId": "uuid",
  "purpose": "EMPLOYMENT",
  "purposeOther": null,
  "urgency": "REGULAR",
  "copies": 1,
  "notes": "For employment application"
}
```

> `purpose` values: `EMPLOYMENT`, `BUSINESS`, `TRAVEL`, `LOAN`, `SCHOLARSHIP`, `OTHER`  
> `urgency` values: `REGULAR`, `RUSH`  
> If `purpose = "OTHER"`, `purposeOther` is required.

> **Fee:** `REGULAR` → ₱50.00, `RUSH` → ₱100.00 (stub values until Phase 6 wires in live settings)

**Response `201 Created`:** `ClearanceRequestDTO`

---

### GET `/clearances/{id}` ✅

**Response `200`:** Single `ClearanceRequestDTO`

```json
{
  "id": "uuid",
  "clearanceNumber": "2026-020001",
  "residentId": "uuid",
  "residentName": "dela Cruz, Juan",
  "requestedBy": "uuid",
  "status": "APPROVED",
  "paymentStatus": "UNPAID",
  "purpose": "EMPLOYMENT",
  "purposeOther": null,
  "urgency": "REGULAR",
  "feeAmount": "50.00",
  "copies": 1,
  "notes": "...",
  "reviewedBy": null,
  "reviewedAt": null,
  "issuedAt": null,
  "createdAt": "2026-02-24T08:00:00Z",
  "updatedAt": "2026-02-24T09:00:00Z"
}
```

> `residentName` is formatted `lastName, firstName` (denormalized at query time). `clearanceNumber` follows the format `YYYY-MMNNNN` and is `null` until the request is `RELEASED`.

---

### POST `/clearances/{id}/approve` ✅

Approve a `FOR_APPROVAL` clearance.

**Roles:** `APPROVER`, `ADMIN`

**Response `200`:** Updated `ClearanceRequestDTO` with `status: "APPROVED"`

| Code              | Meaning                  |
| ----------------- | ------------------------ |
| `200 OK`          | Approved                 |
| `400 Bad Request` | Invalid state transition |
| `403 Forbidden`   | Insufficient role        |

---

### POST `/clearances/{id}/reject` ✅

Reject a `FOR_APPROVAL` clearance. Reason is mandatory.

**Roles:** `APPROVER`, `ADMIN`

**Request body:**

```json
{
  "reason": "Incomplete supporting documents"
}
```

**Response `200`:** Updated `ClearanceRequestDTO` with `status: "REJECTED"`

| Code              | Meaning                         |
| ----------------- | ------------------------------- |
| `200 OK`          | Rejected                        |
| `400 Bad Request` | Missing reason or invalid state |

---

### POST `/clearances/{id}/release` ✅

Release an `APPROVED` + `PAID` clearance. Assigns the clearance number at this point.

**Roles:** `CLERK`, `ADMIN`

**Response `200`:** Updated `ClearanceRequestDTO` with `status: "RELEASED"` and `clearanceNumber: "YYYY-MM-NNNN"`

| Code              | Meaning                              |
| ----------------- | ------------------------------------ |
| `200 OK`          | Released; clearance number assigned  |
| `400 Bad Request` | Not `APPROVED` or payment not `PAID` |

---

### GET `/clearances/summary` ✅

Dashboard count cards broken down by status.

**Roles:** `CLERK`, `APPROVER`, `ADMIN`

**Response `200`:**

```json
{
  "pendingApproval": 12,
  "approvedAwaitingPayment": 5,
  "releasedToday": 3
}
```

---

### GET `/clearances/{id}/pdf` 🔲 _(Phase 5)_

Download the clearance certificate PDF. Only available for `RELEASED` clearances.

**Roles:** `CLERK`, `ADMIN`

**Response `200`:**

- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="clearance-2026-02-0001.pdf"`

| Code              | Meaning                  |
| ----------------- | ------------------------ |
| `200 OK`          | PDF bytes                |
| `400 Bad Request` | Clearance not `RELEASED` |

---

## 5. Clearances — Resident Portal `RESIDENT`

> All routes resolve the resident from the JWT principal. The `residentId` is **never** accepted as a request parameter — this prevents horizontal privilege escalation.

### GET `/me/clearances` ✅

List the authenticated resident's own clearance requests.

**Query params:** `page`, `size`

**Response `200`:** `PageResponse<ClearanceRequestDTO>`

---

### POST `/me/clearances` ✅

Submit a new clearance request. Must have `ACTIVE` account status.

**Request body:**

```json
{
  "purpose": "EMPLOYMENT",
  "purposeOther": null,
  "urgency": "REGULAR",
  "copies": 1,
  "notes": "For job application at BPO"
}
```

**Response `201 Created`:** `ClearanceRequestDTO` with `status: "FOR_APPROVAL"`

| Code            | Meaning              |
| --------------- | -------------------- |
| `201 Created`   | Request submitted    |
| `403 Forbidden` | Account not `ACTIVE` |

---

### GET `/me/clearances/{id}` ✅

Get a single clearance request. Returns `404` if the request does not belong to the authenticated resident (ownership enforced, not exposed as 403).

**Response `200`:** `ClearanceRequestDTO`

---

### PATCH `/me/clearances/{id}/resubmit` ✅

Resubmit a `REJECTED` clearance (moves it back to `FOR_APPROVAL`). Ownership is verified. All editable fields (purpose, urgency, copies, notes) are replaced with the new values.

**Request body:** Same as `POST /me/clearances` (updated fields).

**Response `200`:** Updated `ClearanceRequestDTO` with `status: "FOR_APPROVAL"`

| Code              | Meaning                           |
| ----------------- | --------------------------------- |
| `200 OK`          | Resubmitted                       |
| `400 Bad Request` | Clearance not in `REJECTED` state |
| `403 Forbidden`   | Not the owner                     |

---

### GET `/me/clearances/{id}/pdf` 🔲 _(Phase 5)_

Download the resident's own clearance certificate. Only available when `status = RELEASED`.

**Response `200`:**

- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="clearance-2026-02-0001.pdf"`

| Code            | Meaning                               |
| --------------- | ------------------------------------- |
| `200 OK`        | PDF bytes                             |
| `403 Forbidden` | Clearance not `RELEASED` or not owner |

---

## 6. Payments `RESIDENT, CLERK, ADMIN`

### POST `/clearances/{id}/payments` 🔲

Initiate a payment for an `APPROVED` clearance. Requires idempotency key header.

**Required header:** `Idempotency-Key: <uuid-v4>`  
**Roles:** `RESIDENT` (via portal), `CLERK`, `ADMIN`

**Request body:** _(empty — amount is resolved from `fee_config` at initiation time)_

**Response `201 Created`** (new payment) or **`200 OK`** (idempotent replay):

```json
{
  "id": "uuid",
  "clearanceRequestId": "uuid",
  "amount": "150.00",
  "method": "ONLINE",
  "status": "SUCCESS",
  "provider": "STUB",
  "idempotencyKey": "uuid",
  "idempotent": false,
  "createdAt": "2026-02-24T10:00:00Z"
}
```

> `idempotent: true` if this is a replayed response for the same key.

| Code              | Meaning                                               |
| ----------------- | ----------------------------------------------------- |
| `201 Created`     | New payment processed                                 |
| `200 OK`          | Idempotent replay (already processed)                 |
| `400 Bad Request` | Missing or invalid `Idempotency-Key`                  |
| `409 Conflict`    | Same key is currently `PENDING` (in-flight duplicate) |

---

### POST `/clearances/{id}/mark-paid` 🔲

Record a cash payment for an `APPROVED` clearance. Skips the payment gateway.

**Roles:** `CLERK`, `ADMIN`

**Response `200`:** `PaymentDTO` with `method: "CASH"`, `status: "SUCCESS"`

| Code              | Meaning                                          |
| ----------------- | ------------------------------------------------ |
| `200 OK`          | Marked as paid (idempotent — safe to call twice) |
| `400 Bad Request` | Clearance not in `APPROVED` state                |

---

### GET `/clearances/{id}/payments` 🔲

Get the payment record for a clearance request.

**Roles:** `CLERK`, `ADMIN`

**Response `200`:** `PaymentDTO`

---

## 7. Settings `ADMIN`

> All routes require `ADMIN` role except `GET /settings/logo` (accessible to `CLERK`).

### GET `/settings` 🔲

Retrieve current barangay profile settings.

**Response `200`:**

```json
{
  "barangayName": "Barangay San Isidro",
  "municipality": "Caloocan",
  "province": "Metro Manila",
  "captainName": "Hon. Roberto Cruz",
  "contactNumber": "02-1234567",
  "hasLogo": true,
  "updatedAt": "2026-02-01T08:00:00Z"
}
```

> `logoImage` bytes are excluded from this response. Use `GET /settings/logo` for the image.

---

### PUT `/settings` 🔲

Update barangay profile. Omitting a field leaves it unchanged.

**Request body:** Same fields as `GET /settings` response (all optional).

**Response `200`:** Updated `BarangaySettingsDTO`

---

### POST `/settings/logo` 🔲

Upload barangay logo. Replaces any existing logo.

**Content-Type:** `multipart/form-data`  
**Field name:** `logo`  
**Constraints:** PNG/JPEG/GIF only, max 2 MB

**Response `204 No Content`**

| Code              | Meaning                                |
| ----------------- | -------------------------------------- |
| `204 No Content`  | Logo saved                             |
| `400 Bad Request` | Invalid file type or file exceeds 2 MB |

---

### GET `/settings/logo` 🔲

Download the raw logo image bytes.

**Roles:** `ADMIN`, `CLERK`

**Response `200`:**

- `Content-Type: image/png` (or `image/jpeg`)
- Body: raw image bytes

| Code            | Meaning          |
| --------------- | ---------------- |
| `200 OK`        | Logo bytes       |
| `404 Not Found` | No logo uploaded |

---

### GET `/settings/fees` 🔲

Retrieve current clearance fees.

**Response `200`:**

```json
{
  "regularFee": "150.00",
  "expressFee": "300.00",
  "updatedAt": "2026-02-01T08:00:00Z"
}
```

---

### PUT `/settings/fees` 🔲

Update clearance fees. Takes effect on next payment initiation.

**Request body:**

```json
{
  "regularFee": "150.00",
  "expressFee": "300.00"
}
```

**Response `200`:** Updated `FeeConfigDTO`

---

## 8. Reports `CLERK, ADMIN`

### GET `/reports/clearances` 🔲

Filterable, paginated report of clearance issuances. Replaces the physical log book.

**Query params:**

| Param           | Type         | Description                                                 |
| --------------- | ------------ | ----------------------------------------------------------- |
| `from`          | `date` (ISO) | Filter by issued date (inclusive)                           |
| `to`            | `date` (ISO) | Filter by issued date (inclusive)                           |
| `status`        | `string`     | `DRAFT`, `FOR_APPROVAL`, `APPROVED`, `REJECTED`, `RELEASED` |
| `paymentStatus` | `string`     | `UNPAID`, `PAID`, `WAIVED`                                  |
| `purpose`       | `string`     | e.g. `EMPLOYMENT`, `BUSINESS`                               |
| `purok`         | `string`     | Partial purok/zone match                                    |
| `page`          | `int`        | Default `0`                                                 |
| `size`          | `int`        | Default `20`                                                |

**Response `200`:** `PageResponse<ReportRowDTO>`

```json
{
  "content": [
    {
      "clearanceNumber": "2026-02-0001",
      "residentFullName": "Juan dela Cruz",
      "purpose": "EMPLOYMENT",
      "urgency": "REGULAR",
      "status": "RELEASED",
      "paymentStatus": "PAID",
      "issuedAt": "2026-02-24T10:00:00Z"
    }
  ],
  ...
}
```

---

## Endpoint Summary

| Method | Path                                 | Auth                   | Phase |
| ------ | ------------------------------------ | ---------------------- | ----- |
| `POST` | `/auth/register`                     | Public                 | ✅ 1  |
| `POST` | `/auth/login`                        | Public                 | ✅ 1  |
| `POST` | `/auth/refresh`                      | Public                 | ✅ 1  |
| `POST` | `/auth/logout`                       | Public                 | ✅ 1  |
| `PUT`  | `/auth/change-password`              | Any role               | ✅ 1  |
| `GET`  | `/admin/users`                       | ADMIN                  | ✅ 1  |
| `POST` | `/admin/users`                       | ADMIN                  | ✅ 1  |
| `GET`  | `/admin/users/{id}`                  | ADMIN                  | ✅ 1  |
| `PUT`  | `/admin/users/{id}/deactivate`       | ADMIN                  | ✅ 1  |
| `GET`  | `/residents`                         | CLERK, ADMIN           | 🔲 2  |
| `POST` | `/residents`                         | CLERK, ADMIN           | 🔲 2  |
| `GET`  | `/residents/{id}`                    | CLERK, ADMIN           | 🔲 2  |
| `PUT`  | `/residents/{id}`                    | CLERK, ADMIN           | 🔲 2  |
| `GET`  | `/residents/pending-users`           | CLERK, ADMIN           | 🔲 2  |
| `POST` | `/residents/users/{userId}/activate` | CLERK, ADMIN           | 🔲 2  |
| `POST` | `/residents/users/{userId}/reject`   | CLERK, ADMIN           | 🔲 2  |
| `GET`  | `/me/clearances`                     | RESIDENT               | 🔲 3  |
| `POST` | `/me/clearances`                     | RESIDENT               | 🔲 3  |
| `GET`  | `/me/clearances/{id}`                | RESIDENT               | 🔲 3  |
| `PUT`  | `/me/clearances/{id}`                | RESIDENT               | 🔲 3  |
| `GET`  | `/me/clearances/{id}/pdf`            | RESIDENT               | 🔲 5  |
| `GET`  | `/clearances`                        | CLERK, APPROVER, ADMIN | 🔲 3  |
| `POST` | `/clearances`                        | CLERK, ADMIN           | 🔲 3  |
| `GET`  | `/clearances/{id}`                   | CLERK, APPROVER, ADMIN | 🔲 3  |
| `POST` | `/clearances/{id}/approve`           | APPROVER, ADMIN        | 🔲 3  |
| `POST` | `/clearances/{id}/reject`            | APPROVER, ADMIN        | 🔲 3  |
| `POST` | `/clearances/{id}/release`           | CLERK, ADMIN           | 🔲 3  |
| `GET`  | `/clearances/summary`                | CLERK, APPROVER, ADMIN | 🔲 3  |
| `GET`  | `/clearances/{id}/pdf`               | CLERK, ADMIN           | 🔲 5  |
| `POST` | `/clearances/{id}/payments`          | RESIDENT, CLERK, ADMIN | 🔲 4  |
| `POST` | `/clearances/{id}/mark-paid`         | CLERK, ADMIN           | 🔲 4  |
| `GET`  | `/clearances/{id}/payments`          | CLERK, ADMIN           | 🔲 4  |
| `GET`  | `/settings`                          | ADMIN                  | 🔲 6  |
| `PUT`  | `/settings`                          | ADMIN                  | 🔲 6  |
| `POST` | `/settings/logo`                     | ADMIN                  | 🔲 6  |
| `GET`  | `/settings/logo`                     | ADMIN, CLERK           | 🔲 6  |
| `GET`  | `/settings/fees`                     | ADMIN                  | 🔲 6  |
| `PUT`  | `/settings/fees`                     | ADMIN                  | 🔲 6  |
| `GET`  | `/reports/clearances`                | CLERK, ADMIN           | 🔲 7  |

---

## Clearance State Machine

```
DRAFT ─► FOR_APPROVAL ─► APPROVED ─► (payment) ─► RELEASED
                      └─► REJECTED ─► FOR_APPROVAL (resubmit)
```

| Transition         | Endpoint                          | Required Role          |
| ------------------ | --------------------------------- | ---------------------- |
| Submit             | `POST /me/clearances`             | RESIDENT               |
| Walk-in submit     | `POST /clearances`                | CLERK, ADMIN           |
| Approve            | `POST /clearances/{id}/approve`   | APPROVER, ADMIN        |
| Reject             | `POST /clearances/{id}/reject`    | APPROVER, ADMIN        |
| Resubmit           | `PUT /me/clearances/{id}`         | RESIDENT (owner only)  |
| Mark paid (online) | `POST /clearances/{id}/payments`  | RESIDENT, CLERK, ADMIN |
| Mark paid (cash)   | `POST /clearances/{id}/mark-paid` | CLERK, ADMIN           |
| Release            | `POST /clearances/{id}/release`   | CLERK, ADMIN           |

> **Release guard:** Both `status = APPROVED` **and** `paymentStatus = PAID` must be true.  
> **Clearance number** is assigned only at release — format `YYYY-MM-NNNN` (e.g. `2026-02-0001`).
