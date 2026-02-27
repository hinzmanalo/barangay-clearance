# Barangay Clearance System — API Reference

> **Base URL:** `http://localhost:8080/api/v1`
> **Content-Type:** `application/json` (unless noted)
> **Auth:** `Authorization: Bearer <accessToken>` (except public endpoints)
> **Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [User Management](#2-user-management)
3. [Residents](#3-residents)
4. [Clearances — Backoffice](#4-clearances--backoffice)
5. [Clearances — Resident Portal](#5-clearances--resident-portal)
6. [Payments](#6-payments)
7. [Settings](#7-settings)
8. [Reports](#8-reports)
9. [Error Responses](#9-error-responses)
10. [Enumerations](#10-enumerations)

---

## 1. Authentication

### POST /auth/register
**Visibility:** Public (no token required)

Registers a new resident account. Creates a `User` (status: `PENDING_VERIFICATION`) and a linked `Resident` profile atomically. Staff must activate the account before the user can log in.

**Request Body:**
```json
{
  "email": "juan.delacruz@email.com",
  "password": "SecurePass123!",
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "birthDate": "1990-05-15",
  "gender": "MALE",
  "address": "123 Rizal Street, Barangay Example",
  "contactNumber": "09171234567"
}
```

**Response: 201 Created**
```json
{
  "message": "Registration successful. Please wait for account activation."
}
```

**Errors:**
- `400` — Validation failure (missing fields, invalid email format)
- `409` — Email already registered

---

### POST /auth/login
**Visibility:** Public

Authenticates a user and issues access + refresh tokens.

**Request Body:**
```json
{
  "email": "juan.delacruz@email.com",
  "password": "SecurePass123!"
}
```

**Response: 200 OK**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "mustChangePassword": false
}
```

**Errors:**
- `401` — Invalid credentials
- `403` — Account not active (`PENDING_VERIFICATION`, `REJECTED`, `DEACTIVATED`)

---

### POST /auth/refresh
**Visibility:** Public

Issues a new access token from a valid refresh token. Refresh token is **not rotated**.

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response: 200 OK**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "mustChangePassword": false
}
```

**Errors:**
- `401` — Token invalid, expired, or revoked

---

### POST /auth/logout
**Auth:** Any authenticated user

Revokes the provided refresh token.

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response: 204 No Content**

---

### PUT /auth/change-password
**Auth:** Any authenticated user

Changes the authenticated user's password. Clears `mustChangePassword` flag and **revokes all existing refresh tokens**. Issues new access + refresh tokens.

**Request Body:**
```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewSecurePass456!"
}
```

**Response: 200 OK**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "new-uuid-token",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "mustChangePassword": false
}
```

**Errors:**
- `400` — Current password incorrect; new password fails validation

---

## 2. User Management

### POST /users/staff
**Auth:** ADMIN only

Creates a staff account (CLERK, APPROVER, or ADMIN role) that is immediately active (no verification required).

**Request Body:**
```json
{
  "email": "clerk@barangay.gov.ph",
  "password": "TempPass123!",
  "firstName": "Maria",
  "lastName": "Santos",
  "role": "CLERK"
}
```

**Response: 201 Created**
```json
{
  "id": "uuid",
  "email": "clerk@barangay.gov.ph",
  "firstName": "Maria",
  "lastName": "Santos",
  "role": "CLERK",
  "status": "ACTIVE",
  "mustChangePassword": true,
  "createdAt": "2025-02-26T08:00:00Z"
}
```

**Errors:**
- `400` — Validation failure
- `403` — Caller is not ADMIN
- `409` — Email already in use

---

## 3. Residents

**Base path:** `/residents`
**Auth:** CLERK or ADMIN

### GET /residents
Lists residents with optional search and pagination.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `q` | string | Full-text search on name (case-insensitive) |
| `purok` | string | Filter by address substring |
| `page` | integer | Page index (0-based, default: 0) |
| `size` | integer | Page size (default: 20) |
| `sort` | string | Sort field and direction (e.g., `lastName,asc`) |

**Response: 200 OK**
```json
{
  "content": [
    {
      "id": "uuid",
      "userId": "uuid-or-null",
      "firstName": "Juan",
      "middleName": "Cruz",
      "lastName": "Dela Cruz",
      "birthDate": "1990-05-15",
      "gender": "MALE",
      "address": "123 Rizal Street",
      "contactNumber": "09171234567",
      "email": "juan@email.com",
      "status": "ACTIVE",
      "createdAt": "2025-02-01T08:00:00Z",
      "updatedAt": "2025-02-01T08:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "last": false
}
```

---

### GET /residents/{id}
Retrieves a single resident by ID.

**Response: 200 OK** — `ResidentDTO` object

**Errors:**
- `404` — Resident not found

---

### POST /residents
Creates a walk-in resident (no user account linked).

**Request Body:**
```json
{
  "firstName": "Pedro",
  "middleName": "Cruz",
  "lastName": "Santos",
  "birthDate": "1985-03-20",
  "gender": "MALE",
  "address": "456 Mabini Street, Purok 3",
  "contactNumber": "09181234567",
  "email": "pedro@email.com"
}
```

**Response: 201 Created** — `ResidentDTO` object

---

### PUT /residents/{id}
Partially updates a resident. Null fields are skipped.

**Request Body:** Same shape as `POST /residents` (all fields optional)

**Response: 200 OK** — Updated `ResidentDTO`

**Errors:**
- `404` — Resident not found

---

### GET /residents/pending-users
Lists residents whose linked portal accounts are in `PENDING_VERIFICATION` status.

**Response: 200 OK** — Array of `ResidentDTO`

---

### POST /residents/users/{userId}/activate
Activates a pending user account (`PENDING_VERIFICATION` → `ACTIVE`).

**Response: 204 No Content**

**Errors:**
- `404` — User not found
- `409` — User is not in PENDING_VERIFICATION state

---

### POST /residents/users/{userId}/reject
Rejects a pending user account (`PENDING_VERIFICATION` → `REJECTED`).

**Response: 204 No Content**

**Errors:**
- `404` — User not found
- `409` — User is not in PENDING_VERIFICATION state

---

## 4. Clearances — Backoffice

**Base path:** `/clearances`
**Auth:** CLERK, APPROVER, or ADMIN (specific actions have tighter requirements — see matrix)

### GET /clearances
Lists clearance requests with optional filters and pagination.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by `ClearanceStatus` enum |
| `paymentStatus` | string | Filter by `PaymentStatus` enum |
| `from` | date (ISO 8601) | Filter by `createdAt >= from` |
| `to` | date (ISO 8601) | Filter by `createdAt <= to` |
| `page` | integer | Page index (0-based) |
| `size` | integer | Page size |
| `sort` | string | Sort field and direction |

**Response: 200 OK**
```json
{
  "content": [
    {
      "id": "uuid",
      "clearanceNumber": "2025-02-0001",
      "residentId": "uuid",
      "residentName": "Juan Dela Cruz",
      "requestedBy": "uuid",
      "purpose": "EMPLOYMENT",
      "purposeOther": null,
      "urgency": "STANDARD",
      "feeAmount": 50.00,
      "copies": 1,
      "status": "FOR_APPROVAL",
      "paymentStatus": "UNPAID",
      "notes": null,
      "reviewedBy": null,
      "reviewedAt": null,
      "issuedAt": null,
      "createdAt": "2025-02-26T08:00:00Z",
      "updatedAt": "2025-02-26T08:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3,
  "last": false
}
```

---

### POST /clearances
**Auth:** CLERK or ADMIN

Creates a walk-in clearance request on behalf of a resident. Sets status to `FOR_APPROVAL`.

**Request Body:**
```json
{
  "residentId": "uuid-of-existing-resident",
  "purpose": "EMPLOYMENT",
  "purposeOther": null,
  "urgency": "STANDARD",
  "copies": 1
}
```

**Response: 201 Created** — `ClearanceRequestDTO`

**Notes:**
- `feeAmount` is automatically calculated from `FeeConfig` based on `urgency`
- If `purpose = OTHER`, `purposeOther` is required

---

### GET /clearances/{id}
Retrieves a single clearance request.

**Response: 200 OK** — `ClearanceRequestDTO`

**Errors:**
- `404` — Clearance not found

---

### POST /clearances/{id}/approve
**Auth:** APPROVER or ADMIN

Transitions clearance from `FOR_APPROVAL` → `APPROVED`.

**Response: 200 OK** — Updated `ClearanceRequestDTO`

**Errors:**
- `404` — Clearance not found
- `409` — Invalid state transition (not in `FOR_APPROVAL`)
- `403` — Caller lacks APPROVER or ADMIN role

---

### POST /clearances/{id}/reject
**Auth:** APPROVER or ADMIN

Transitions clearance from `FOR_APPROVAL` → `REJECTED`. Appends rejection reason to `notes`.

**Request Body:**
```json
{
  "reason": "Incomplete documentation submitted"
}
```

**Response: 200 OK** — Updated `ClearanceRequestDTO`

**Errors:**
- `404` — Clearance not found
- `409` — Invalid state transition
- `400` — Reason is blank

---

### POST /clearances/{id}/release
**Auth:** CLERK or ADMIN

Transitions `APPROVED + PAID` → `RELEASED`. Atomically assigns clearance number and sets `issuedAt`.

**Response: 200 OK** — Updated `ClearanceRequestDTO` (with `clearanceNumber` populated)

**Errors:**
- `404` — Clearance not found
- `409` — Invalid state transition (not `APPROVED`) or payment not yet `PAID`
- `403` — Caller lacks CLERK or ADMIN role

---

### GET /clearances/{id}/pdf
**Auth:** CLERK or ADMIN

Downloads the clearance certificate as a PDF.

**Response: 200 OK**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="clearance-2025-02-0001.pdf"
Body: <binary PDF>
```

**Errors:**
- `404` — Clearance not found
- `409` — Clearance is not yet `RELEASED`

---

### GET /clearances/summary
**Auth:** CLERK, APPROVER, or ADMIN

Returns dashboard counts for the clearance module.

**Response: 200 OK**
```json
{
  "pending": 12,
  "approved": 5,
  "awaitingPayment": 3,
  "releasedToday": 8
}
```

---

## 5. Clearances — Resident Portal

**Base path:** `/me/clearances`
**Auth:** RESIDENT only

All portal endpoints resolve the authenticated resident's identity from the JWT. The resident cannot access another resident's data.

### GET /me/clearances
Lists the authenticated resident's clearance requests.

**Query Parameters:** `page`, `size`, `sort`

**Response: 200 OK** — `PageResponse<ClearanceRequestDTO>`

---

### POST /me/clearances
Submits a new clearance request. Sets status directly to `FOR_APPROVAL`.

**Request Body:**
```json
{
  "purpose": "SCHOLARSHIP",
  "purposeOther": null,
  "urgency": "RUSH",
  "copies": 2
}
```

**Notes:**
- `residentId` is resolved from JWT — not provided in request body
- `feeAmount` is calculated server-side from `FeeConfig` × `copies`

**Response: 201 Created** — `ClearanceRequestDTO`

**Errors:**
- `403` — Resident account not ACTIVE
- `400` — `purpose = OTHER` requires `purposeOther`

---

### GET /me/clearances/{id}
Retrieves a specific clearance. Returns `403` if the clearance belongs to a different resident.

**Response: 200 OK** — `ClearanceRequestDTO`

**Errors:**
- `404` — Clearance not found
- `403` — Clearance does not belong to authenticated resident

---

### PUT /me/clearances/{id}
Resubmits a rejected clearance. Transitions `REJECTED` → `FOR_APPROVAL`.

**Request Body:** Same as `POST /me/clearances`

**Response: 200 OK** — Updated `ClearanceRequestDTO`

**Errors:**
- `409` — Clearance is not in `REJECTED` state
- `403` — Clearance does not belong to authenticated resident

---

### GET /me/clearances/{id}/pdf
Downloads the resident's own clearance PDF (ownership validated).

**Response: 200 OK**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="clearance-2025-02-0001.pdf"
```

**Errors:**
- `403` — Not the owner
- `409` — Clearance not yet `RELEASED`

---

## 6. Payments

### POST /clearances/{id}/payments
**Auth:** RESIDENT (portal online payment)
**Required Header:** `Idempotency-Key: <UUID v4>`

Initiates an online payment for an approved clearance.

**Request:**
```
POST /api/v1/clearances/{clearanceId}/payments
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

**Response: 200 OK** (success) or **200 OK** with `idempotent: true` (duplicate)
```json
{
  "id": "uuid",
  "clearanceRequestId": "uuid",
  "amount": 100.00,
  "status": "SUCCESS",
  "paymentMethod": "STUB",
  "provider": "STUB",
  "idempotent": false,
  "createdAt": "2025-02-26T10:00:00Z"
}
```

**Errors:**
- `400` — `Idempotency-Key` header missing or invalid format
- `404` — Clearance not found
- `409` — Clearance not in `APPROVED` state; or a PENDING payment already exists for this key (concurrent request)

**Idempotency Behavior:**

| Existing Record Status | Time Window | Response |
|------------------------|------------|---------|
| PENDING | < 24h | `409 Conflict` |
| SUCCESS or FAILED | < 24h | `200 OK` + `idempotent: true` + cached response |
| Any | > 24h | Treated as new request |

---

### POST /clearances/{id}/mark-paid
**Auth:** CLERK or ADMIN

Records a cash payment collected in-person. Idempotent — returns existing payment if already paid.

**Response: 200 OK** — `PaymentDTO`

**Errors:**
- `404` — Clearance not found
- `409` — Clearance not in `APPROVED` state (if not already paid)

---

### GET /clearances/{id}/payments
**Auth:** CLERK, APPROVER, or ADMIN

Lists all payment records for a clearance (most recent first).

**Response: 200 OK** — Array of `PaymentDTO`

---

### POST /me/clearances/{id}/pay
**Auth:** RESIDENT
**Required Header:** `Idempotency-Key: <UUID v4>`

Portal variant of payment initiation. Validates resident ownership before processing.

**Response:** Same as `POST /clearances/{id}/payments`

**Errors:**
- `403` — Clearance does not belong to authenticated resident

---

## 7. Settings

**Auth:** ADMIN only

### GET /settings/barangay
Retrieves barangay settings (logo returned as Base64).

**Response: 200 OK**
```json
{
  "barangayName": "Barangay San Isidro",
  "municipality": "Cabanatuan City",
  "province": "Nueva Ecija",
  "captainName": "Hon. Maria Santos",
  "logoBase64": "data:image/png;base64,...",
  "logoMimeType": "image/png",
  "updatedAt": "2025-02-01T00:00:00Z"
}
```

---

### PUT /settings/barangay
Updates barangay settings. Accepts logo as multipart or Base64.

**Request Body:**
```json
{
  "barangayName": "Barangay San Isidro",
  "municipality": "Cabanatuan City",
  "province": "Nueva Ecija",
  "captainName": "Hon. Maria Santos",
  "logoBase64": "data:image/png;base64,..."
}
```

**Response: 200 OK** — Updated `BarangaySettingsDTO`

---

### GET /settings/fee-config
Retrieves the current fee configuration.

**Response: 200 OK**
```json
{
  "standardFee": 50.00,
  "rushFee": 100.00,
  "updatedAt": "2025-02-01T00:00:00Z"
}
```

---

### PUT /settings/fee-config
Updates fee configuration.

**Request Body:**
```json
{
  "standardFee": 75.00,
  "rushFee": 150.00
}
```

**Response: 200 OK** — Updated `FeeConfigDTO`

**Errors:**
- `400` — Fees must be positive numbers

---

## 8. Reports

**Base path:** `/reports`
**Auth:** ADMIN or APPROVER

### GET /reports
Generates a filtered clearance report.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by clearance status |
| `purpose` | string | Filter by clearance purpose |
| `urgency` | string | Filter by urgency (STANDARD/RUSH) |
| `from` | date | Start date (ISO 8601) |
| `to` | date | End date (ISO 8601) |
| `page` | integer | Page index |
| `size` | integer | Page size |

**Response: 200 OK**
```json
{
  "content": [
    {
      "clearanceNumber": "2025-02-0001",
      "residentName": "Juan Dela Cruz",
      "purpose": "EMPLOYMENT",
      "urgency": "STANDARD",
      "feeAmount": 50.00,
      "status": "RELEASED",
      "paymentStatus": "PAID",
      "issuedAt": "2025-02-26T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 80,
  "totalPages": 4,
  "last": false
}
```

---

## 9. Error Responses

All error responses follow a consistent schema:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Clearance request not found",
  "timestamp": "2025-02-26T10:30:00Z",
  "path": "/api/v1/clearances/nonexistent-id",
  "details": []
}
```

For validation errors, `details` contains field-level errors:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2025-02-26T10:30:00Z",
  "path": "/api/v1/auth/register",
  "details": [
    { "field": "email", "message": "must be a well-formed email address" },
    { "field": "password", "message": "must not be blank" }
  ]
}
```

**Standard HTTP Status Codes:**

| Status | Meaning | Common Cause |
|--------|---------|--------------|
| 200 | OK | Successful GET, PUT, or action endpoint |
| 201 | Created | Successful POST (resource created) |
| 204 | No Content | Successful action with no body |
| 400 | Bad Request | Validation failure, business rule violation |
| 401 | Unauthorized | Missing or invalid JWT |
| 403 | Forbidden | Insufficient role; ownership violation |
| 404 | Not Found | Resource does not exist |
| 409 | Conflict | Duplicate resource; invalid state transition; payment conflict |
| 500 | Internal Server Error | Unexpected error |

---

## 10. Enumerations

### User Role
| Value | Description |
|-------|-------------|
| `ADMIN` | Full system access |
| `CLERK` | Creates requests, collects cash, releases clearances |
| `APPROVER` | Reviews and approves/rejects requests |
| `RESIDENT` | Portal user — submits own requests |

### User Status
| Value | Description |
|-------|-------------|
| `ACTIVE` | Account is operational |
| `INACTIVE` | Manually deactivated |
| `PENDING_VERIFICATION` | Registered; awaiting staff activation |
| `REJECTED` | Registration was rejected by staff |
| `DEACTIVATED` | Account deactivated by admin |

### Clearance Status
| Value | Description |
|-------|-------------|
| `DRAFT` | Walk-in initial state or re-submitted (not submitted yet for portal) |
| `FOR_APPROVAL` | Submitted; awaiting staff review |
| `APPROVED` | Approved; awaiting payment |
| `REJECTED` | Rejected by approver |
| `RELEASED` | Paid and released; clearance number assigned |

### Payment Status
| Value | Description |
|-------|-------------|
| `UNPAID` | No successful payment |
| `PAID` | Payment confirmed |
| `WAIVED` | Fee waived (reserved for future use) |

### Clearance Purpose
| Value | Description |
|-------|-------------|
| `EMPLOYMENT` | Job application |
| `TRAVEL_ABROAD` | Overseas travel / OFW |
| `SCHOLARSHIP` | Educational scholarship |
| `LOAN` | Bank loan or financial institution |
| `BUSINESS_PERMIT` | Business registration |
| `LEGAL` | Legal proceedings |
| `CEDULA` | Community Tax Certificate |
| `OTHER` | Custom purpose (requires `purposeOther`) |

### Urgency
| Value | Fee Applied | Description |
|-------|------------|-------------|
| `STANDARD` | `standardFee` | Normal processing |
| `RUSH` | `rushFee` | Expedited processing |

### Payment Method
| Value | Description |
|-------|-------------|
| `STUB` | Simulated online payment (development/testing) |
| `CASH` | Cash collected by clerk |

### Payment Record Status
| Value | Description |
|-------|-------------|
| `PENDING` | Gateway call in progress |
| `SUCCESS` | Payment confirmed |
| `FAILED` | Gateway returned failure |

---

## Appendix: Request/Response DTOs

### ClearanceRequestDTO (full response shape)
```json
{
  "id": "uuid",
  "clearanceNumber": "2025-02-0001",
  "residentId": "uuid",
  "residentName": "Juan Dela Cruz",
  "requestedBy": "uuid",
  "purpose": "EMPLOYMENT",
  "purposeOther": null,
  "urgency": "STANDARD",
  "feeAmount": 50.00,
  "copies": 1,
  "status": "RELEASED",
  "paymentStatus": "PAID",
  "notes": null,
  "reviewedBy": "uuid",
  "reviewedAt": "2025-02-26T09:00:00Z",
  "issuedAt": "2025-02-26T10:00:00Z",
  "createdAt": "2025-02-26T08:00:00Z",
  "updatedAt": "2025-02-26T10:00:00Z"
}
```

### ResidentDTO (full response shape)
```json
{
  "id": "uuid",
  "userId": "uuid-or-null",
  "firstName": "Juan",
  "middleName": "Cruz",
  "lastName": "Dela Cruz",
  "birthDate": "1990-05-15",
  "gender": "MALE",
  "address": "123 Rizal Street, Barangay Example",
  "contactNumber": "09171234567",
  "email": "juan@email.com",
  "status": "ACTIVE",
  "createdAt": "2025-02-01T08:00:00Z",
  "updatedAt": "2025-02-01T08:00:00Z"
}
```

### PageResponse (pagination wrapper)
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "last": false
}
```
