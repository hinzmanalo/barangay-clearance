# Residents Domain

Manages the barangay resident registry. Staff (CLERK/ADMIN) can create and update resident profiles for both walk-in applicants and self-registered portal users. The module also handles the portal account activation/rejection workflow.

---

## Table of Contents

1. [Module Structure](#module-structure)
2. [Entity Model](#entity-model)
3. [DTOs](#dtos)
4. [API Endpoints](#api-endpoints)
5. [Service Behavior](#service-behavior)
6. [Repository Queries](#repository-queries)
7. [Mapper](#mapper)
8. [Integration with Auth Module](#integration-with-auth-module)
9. [Security](#security)
10. [Error Codes](#error-codes)

---

## Module Structure

```
residents/
├── controller/
│   └── ResidentController.java       # REST endpoints
├── dto/
│   ├── CreateResidentRequest.java    # POST body (walk-in creation)
│   ├── UpdateResidentRequest.java    # PUT body (partial update)
│   ├── ResidentDTO.java              # API response shape
│   └── ResidentSearchRequest.java    # Search filter params (unused directly; params come via @RequestParam)
├── entity/
│   └── Resident.java                 # JPA entity → "residents" table
├── repository/
│   └── ResidentRepository.java       # Spring Data JPA + custom JPQL search
└── service/
    ├── ResidentService.java          # Business logic
    └── mapper/
        └── ResidentMapper.java       # MapStruct entity ↔ DTO mappings
```

---

## Entity Model

**Table:** `residents`

| Column           | Type           | Constraints                | Notes                                  |
| ---------------- | -------------- | -------------------------- | -------------------------------------- |
| `id`             | `UUID`         | PK, generated              |                                        |
| `user_id`        | `UUID`         | nullable, FK → `users.id`  | Null for walk-in residents             |
| `first_name`     | `VARCHAR(100)` | NOT NULL                   |                                        |
| `middle_name`    | `VARCHAR(100)` | nullable                   |                                        |
| `last_name`      | `VARCHAR(100)` | NOT NULL                   |                                        |
| `birth_date`     | `DATE`         | NOT NULL                   |                                        |
| `gender`         | `VARCHAR(10)`  | NOT NULL, enum             | `MALE`, `FEMALE`, `OTHER`              |
| `address`        | `TEXT`         | NOT NULL                   | Includes purok/zone; searched via LIKE |
| `contact_number` | `VARCHAR(20)`  | nullable                   |                                        |
| `email`          | `VARCHAR(255)` | nullable                   |                                        |
| `status`         | `VARCHAR(20)`  | NOT NULL, default `ACTIVE` | `ACTIVE`, `INACTIVE`                   |
| `created_at`     | `TIMESTAMPTZ`  | NOT NULL, auto-set         |                                        |
| `updated_at`     | `TIMESTAMPTZ`  | NOT NULL, auto-updated     |                                        |

### Enums

```java
enum Gender        { MALE, FEMALE, OTHER }
enum ResidentStatus { ACTIVE, INACTIVE }
```

---

## DTOs

### `ResidentDTO` (Response)

Returned by all read and write endpoints.

| Field              | Type              | Notes                                          |
| ------------------ | ----------------- | ---------------------------------------------- |
| `id`               | `UUID`            |                                                |
| `userId`           | `UUID` / `null`   | Linked portal user; null for walk-in residents |
| `firstName`        | `String`          |                                                |
| `middleName`       | `String` / `null` |                                                |
| `lastName`         | `String`          |                                                |
| `birthDate`        | `LocalDate`       | ISO-8601 date, e.g. `1990-05-15`               |
| `gender`           | `Gender`          | `MALE`, `FEMALE`, `OTHER`                      |
| `address`          | `String`          |                                                |
| `contactNumber`    | `String` / `null` |                                                |
| `email`            | `String` / `null` |                                                |
| `status`           | `ResidentStatus`  | `ACTIVE`, `INACTIVE`                           |
| `hasPortalAccount` | `boolean`         | `true` when `userId` is non-null               |
| `createdAt`        | `Instant`         | ISO-8601 timestamp                             |
| `updatedAt`        | `Instant`         | ISO-8601 timestamp                             |

---

### `CreateResidentRequest` (POST body)

Used when staff manually creates a walk-in resident. No portal account is created.

| Field           | Type        | Required | Validation                 |
| --------------- | ----------- | -------- | -------------------------- |
| `firstName`     | `String`    | Yes      | Not blank, max 100 chars   |
| `middleName`    | `String`    | No       | Max 100 chars              |
| `lastName`      | `String`    | Yes      | Not blank, max 100 chars   |
| `birthDate`     | `LocalDate` | Yes      | Must be in the past        |
| `gender`        | `Gender`    | Yes      | `MALE`, `FEMALE`, `OTHER`  |
| `address`       | `String`    | Yes      | Not blank                  |
| `contactNumber` | `String`    | No       | Max 20 chars               |
| `email`         | `String`    | No       | Valid email, max 255 chars |

---

### `UpdateResidentRequest` (PUT body)

Partial update — every field is optional. Only non-null fields are applied.

| Field           | Type             | Validation                 | Notes                            |
| --------------- | ---------------- | -------------------------- | -------------------------------- |
| `firstName`     | `String`         | Max 100 chars              |                                  |
| `middleName`    | `String`         | Max 100 chars              |                                  |
| `lastName`      | `String`         | Max 100 chars              |                                  |
| `birthDate`     | `LocalDate`      | Must be in the past        |                                  |
| `gender`        | `Gender`         |                            |                                  |
| `address`       | `String`         |                            |                                  |
| `contactNumber` | `String`         | Max 20 chars               |                                  |
| `email`         | `String`         | Valid email, max 255 chars |                                  |
| `status`        | `ResidentStatus` |                            | `ACTIVE` or `INACTIVE`           |
| `userId`        | `UUID`           |                            | Link or re-link a portal account |

---

## API Endpoints

Base path: `/api/v1/residents`  
Required role: `CLERK` or `ADMIN` (unless noted otherwise)  
Authentication: `Authorization: Bearer <access_token>`

---

### `GET /api/v1/residents`

Search and paginate the resident registry.

**Query Parameters**

| Name    | Type     | Default | Description                               |
| ------- | -------- | ------- | ----------------------------------------- |
| `q`     | `String` | —       | Free-text search on first/last name       |
| `purok` | `String` | —       | Substring match against the address field |
| `page`  | `int`    | `0`     | Zero-based page index                     |
| `size`  | `int`    | `20`    | Page size                                 |

Results are sorted by `lastName` then `firstName` ascending.

**Response `200 OK`**

```json
{
  "content": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "userId": null,
      "firstName": "Maria",
      "middleName": "Santos",
      "lastName": "Dela Cruz",
      "birthDate": "1990-05-15",
      "gender": "FEMALE",
      "address": "Purok 3, Barangay San Jose",
      "contactNumber": "09171234567",
      "email": "maria@example.com",
      "status": "ACTIVE",
      "hasPortalAccount": false,
      "createdAt": "2025-02-01T08:00:00Z",
      "updatedAt": "2025-02-01T08:00:00Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "page": 0,
  "size": 20
}
```

---

### `POST /api/v1/residents`

Create a walk-in resident (no portal account).

**Request Body**

```json
{
  "firstName": "Juan",
  "middleName": "Reyes",
  "lastName": "Dela Cruz",
  "birthDate": "1985-03-20",
  "gender": "MALE",
  "address": "Purok 5, Barangay San Jose",
  "contactNumber": "09181234567",
  "email": "juan@example.com"
}
```

**Response `201 Created`** — returns the full `ResidentDTO`.

---

### `GET /api/v1/residents/{id}`

Retrieve a single resident by ID.

**Path Parameters**

| Name | Type   | Description |
| ---- | ------ | ----------- |
| `id` | `UUID` | Resident ID |

**Response `200 OK`** — returns the full `ResidentDTO`.  
**Response `404 Not Found`** — resident does not exist.

---

### `PUT /api/v1/residents/{id}`

Update an existing resident's profile. Send only the fields that need to change.

**Path Parameters**

| Name | Type   | Description |
| ---- | ------ | ----------- |
| `id` | `UUID` | Resident ID |

**Request Body** — any subset of `UpdateResidentRequest` fields.

**Response `200 OK`** — returns the updated `ResidentDTO`.  
**Response `404 Not Found`** — resident does not exist.

---

### `GET /api/v1/residents/pending-users`

List all resident portal accounts awaiting staff verification (`PENDING_VERIFICATION` status).

**Response `200 OK`** — array of `ResidentDTO`.

```json
[
  {
    "id": "...",
    "userId": "...",
    "firstName": "Ana",
    ...
    "hasPortalAccount": true
  }
]
```

---

### `POST /api/v1/residents/users/{userId}/activate`

Approve a pending portal registration. Sets the linked user's status to `ACTIVE`, granting login access.

**Path Parameters**

| Name     | Type   | Description    |
| -------- | ------ | -------------- |
| `userId` | `UUID` | Portal user ID |

**Response `204 No Content`** — account activated.  
**Response `400 Bad Request`** — user is not in `PENDING_VERIFICATION` state.  
**Response `404 Not Found`** — user does not exist.

---

### `POST /api/v1/residents/users/{userId}/reject`

Reject a pending portal registration. Sets the linked user's status to `REJECTED`.

**Path Parameters**

| Name     | Type   | Description    |
| -------- | ------ | -------------- |
| `userId` | `UUID` | Portal user ID |

**Response `204 No Content`** — account rejected.  
**Response `400 Bad Request`** — user is not in `PENDING_VERIFICATION` state.  
**Response `404 Not Found`** — user does not exist.

---

## Service Behavior

### `search(q, purok, pageable)`

- Trims and normalises both `q` and `purok` to empty strings when blank.
- Delegates name matching to the `ResidentRepository.search` JPQL query (see [Repository Queries](#repository-queries)).
- Returns `PageResponse<ResidentDTO>`.

### `getById(id)`

- Throws `AppException(404)` when the resident is not found.

### `create(request)`

- Maps `CreateResidentRequest` → `Resident` via `ResidentMapper.toEntity`.
- `userId` is left null (walk-in).
- `status` defaults to `ACTIVE`.
- Logged at INFO: `RESIDENT_CREATED id={} name={} {}`.

### `update(id, request)`

- Applies only non-null fields from `UpdateResidentRequest` (manual null-check pattern, not MapStruct).
- `userId` can be set here to link an existing walk-in resident to a portal account.
- Throws `AppException(404)` when the resident is not found.
- Logged at INFO: `RESIDENT_UPDATED id={}`.

### `createFromRegistration(request, user)`

- Called internally by `AuthService.register()` within the same transaction.
- Builds a `Resident` from the portal `RegisterRequest`, setting `userId` to the newly created `User.id`.
- Status is forced to `ACTIVE` (activation is controlled by the user account status, not the resident status).
- Logged at INFO: `RESIDENT_CREATED (via registration) id={} userId={}`.

### `findPendingUsers()`

- Queries all `User` records with `status = PENDING_VERIFICATION`.
- Filters to `role = RESIDENT` only.
- Looks up the linked `Resident` via `ResidentRepository.findByUserId(userId)`.
- Silently drops users whose resident profile cannot be found (returns `null`-filtered list).

### `activateUser(userId)` / `rejectUser(userId)`

- Both verify the user exists (`AppException 404`) and is in `PENDING_VERIFICATION` (`AppException 400`).
- `activateUser` → sets `User.status = ACTIVE`.
- `rejectUser` → sets `User.status = REJECTED`.
- Both log the result at INFO level.

---

## Repository Queries

### Custom JPQL: `search`

```sql
SELECT r FROM Resident r
WHERE (:q = '' OR
       LOWER(CONCAT(r.lastName, ' ', r.firstName)) LIKE LOWER(CONCAT('%', :q, '%')) OR
       LOWER(CONCAT(r.firstName, ' ', r.lastName)) LIKE LOWER(CONCAT('%', :q, '%')))
  AND (:purok = '' OR LOWER(r.address) LIKE LOWER(CONCAT('%', :purok, '%')))
```

- Matches `"lastName firstName"` **and** `"firstName lastName"` to support natural input order.
- Case-insensitive (`LOWER`).
- Leverages `idx_residents_name` functional index in PostgreSQL when possible.
- `purok` does an address substring match — useful for filtering by zone/sitio.

### Derived Finders

| Method                        | Description                              |
| ----------------------------- | ---------------------------------------- |
| `findByUserId(UUID userId)`   | Returns the resident linked to a user ID |
| `existsByUserId(UUID userId)` | Quick existence check                    |

---

## Mapper

`ResidentMapper` is a MapStruct interface with Spring component model.

| Method                            | Description                                                                |
| --------------------------------- | -------------------------------------------------------------------------- |
| `toDTO(Resident resident)`        | Maps entity → DTO; computes `hasPortalAccount = (userId != null)`          |
| `toEntity(CreateResidentRequest)` | Maps create request → entity; ignores `id`, `userId`, `status`, timestamps |

> **Note:** `UpdateResidentRequest` is not mapped via MapStruct — field assignment is done manually in `ResidentService.update` to support null-means-no-change semantics.

---

## Integration with Auth Module

The residents module is coupled to the identity module in two places:

1. **Portal registration** — `ResidentService.createFromRegistration(RegisterRequest, User)` is called by `AuthService` to create the resident profile as part of the same transaction when a resident self-registers.

2. **Account activation/rejection** — `ResidentService.activateUser` and `rejectUser` both write to `UserRepository` (from the identity module). This is the only cross-module write operation in this domain; it is justified because the activation workflow is a staff operation exposed through the resident management UI.

---

## Security

| Endpoint                                     | Minimum Role   |
| -------------------------------------------- | -------------- |
| `GET /api/v1/residents`                      | CLERK or ADMIN |
| `POST /api/v1/residents`                     | CLERK or ADMIN |
| `GET /api/v1/residents/{id}`                 | CLERK or ADMIN |
| `PUT /api/v1/residents/{id}`                 | CLERK or ADMIN |
| `GET /api/v1/residents/pending-users`        | CLERK or ADMIN |
| `POST /api/v1/residents/users/{id}/activate` | CLERK or ADMIN |
| `POST /api/v1/residents/users/{id}/reject`   | CLERK or ADMIN |

The class-level `@PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")` applies to all endpoints. Residents do not have direct access to any endpoint in this module.

---

## Error Codes

| HTTP Status | Scenario                                       |
| ----------- | ---------------------------------------------- |
| `400`       | Bean validation failure on request body        |
| `400`       | `activateUser`/`rejectUser` — user not pending |
| `401`       | Missing or invalid JWT                         |
| `403`       | Authenticated user lacks CLERK or ADMIN role   |
| `404`       | Resident not found by ID                       |
| `404`       | User not found during activation/rejection     |

All errors follow the standard `ErrorResponse` envelope:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Resident not found",
  "timestamp": "2025-02-25T10:00:00Z",
  "path": "/api/v1/residents/3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```
