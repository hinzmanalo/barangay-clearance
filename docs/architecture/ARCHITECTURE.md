# Barangay Clearance System — Architecture & System Design

> **Audience:** System architects and backend/frontend developers
> **Stack:** Spring Boot 3.3.4 · Java 21 · PostgreSQL 16 · Next.js 14
> **Pattern:** Modular Monolith with clean module boundaries

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Tech Stack](#2-tech-stack)
3. [Design Considerations](#3-design-considerations)
4. [System Architecture](#4-system-architecture)
5. [Module Structure](#5-module-structure)
6. [Database Design & Entity Relationships](#6-database-design--entity-relationships)
7. [Security Architecture](#7-security-architecture)
8. [Clearance State Machine](#8-clearance-state-machine)
9. [Payment Subsystem](#9-payment-subsystem)
10. [PDF Generation](#10-pdf-generation)
11. [Configuration & Profiles](#11-configuration--profiles)
12. [Infrastructure & Deployment](#12-infrastructure--deployment)
13. [Cross-Cutting Concerns](#13-cross-cutting-concerns)
14. [Concurrency](#14-concurrency)
15. [Transaction Management](#15-transaction-management)
16. [Performance Considerations](#16-performance-considerations)

---

## 1. System Overview

The **Barangay Clearance System** is a web application that digitizes the issuance of barangay clearances — official documents issued by Filipino local government units (barangays) that certify a resident's good standing in the community.

### Actors

| Actor        | Description                                                                                  |
| ------------ | -------------------------------------------------------------------------------------------- |
| **Resident** | Applies for a clearance via the resident portal (online or walk-in)                          |
| **Clerk**    | Barangay staff who creates walk-in requests, collects cash payments, and releases clearances |
| **Approver** | Barangay officer who reviews and approves or rejects clearance requests                      |
| **Admin**    | System administrator who manages staff accounts, barangay settings, and fee configuration    |

### Core Workflows

1. **Online Application** — Resident registers → staff activates account → resident submits clearance request → approver reviews → resident pays → clerk releases → resident downloads PDF
2. **Walk-in Application** — Clerk creates request on behalf of resident → approver reviews → clerk collects cash → clerk releases → clearance PDF generated
3. **Administration** — Admin manages barangay profile (name, captain, logo), fee structure, and staff accounts

---

## 2. Tech Stack

### Backend

| Component   | Technology                  | Version       | Rationale                                                       |
| ----------- | --------------------------- | ------------- | --------------------------------------------------------------- |
| Language    | Java                        | 21 (LTS)      | Virtual threads ready; long-term support                        |
| Framework   | Spring Boot                 | 3.3.4         | Convention over configuration; mature ecosystem                 |
| Security    | Spring Security + JJWT      | 6.x + 0.12.6  | Stateless JWT; no session backend needed                        |
| ORM         | Spring Data JPA / Hibernate | 6.x           | Reduces boilerplate; dynamic query via Specifications           |
| Database    | PostgreSQL                  | 16            | ACID transactions; `gen_random_uuid()`; `ON CONFLICT DO UPDATE` |
| Migrations  | Flyway                      | 10.x          | Version-controlled schema; audit trail of changes               |
| PDF         | Apache PDFBox               | 3.0.3         | Pure Java; no native dependencies; A4/custom layout             |
| Mapping     | MapStruct                   | 1.5.5         | Compile-time safe entity↔DTO mapping; zero reflection overhead  |
| Boilerplate | Lombok                      | 1.18.34       | Getters/setters/builders at compile time                        |
| API Docs    | SpringDoc OpenAPI           | 2.6.0         | Auto-generates Swagger UI from annotations                      |
| Build       | Maven                       | 3.x (wrapper) | Standard Java build; reproducible builds                        |

### Frontend

| Component     | Technology            | Version | Rationale                                       |
| ------------- | --------------------- | ------- | ----------------------------------------------- |
| Framework     | Next.js               | 14      | App Router; SSR/SSG; React 18                   |
| Language      | TypeScript            | 5.x     | Type safety across API boundaries               |
| Styling       | Tailwind CSS          | 3.x     | Utility-first; no runtime style computation     |
| UI Components | shadcn/ui             | latest  | Accessible; composable; Radix UI primitives     |
| Forms         | React Hook Form + Zod | latest  | Validation schema shared with types             |
| Data Fetching | TanStack React Query  | v5      | Caching; background refresh; optimistic updates |
| HTTP Client   | Axios                 | 1.x     | Interceptors for 401→refresh→retry flow         |

### Infrastructure

| Component     | Technology     | Notes                                      |
| ------------- | -------------- | ------------------------------------------ |
| Container     | Docker Compose | Local dev + production                     |
| Reverse Proxy | Nginx          | Routes `/api` → Spring Boot, `/` → Next.js |
| Database      | PostgreSQL 16  | Managed via Docker volume in dev           |

---

## 3. Design Considerations

### 3.1 Modular Monolith over Microservices

**Decision:** Single deployable artifact with strict module boundaries.

**Rationale:**

- The system is **small domain** — 6 bounded contexts, all tightly coupled around a single PostgreSQL database
- Microservices would introduce distributed transaction complexity with no meaningful scalability benefit at barangay scale (dozens to hundreds of concurrent users)
- Module boundaries are enforced **by convention** (no cross-module JPA relationships), making a future split to microservices feasible without major refactoring

### 3.2 Stateless JWT Authentication

**Decision:** Short-lived access tokens (15 min) + long-lived opaque refresh tokens (7 days) stored as SHA-256 hashes in the database.

**Rationale:**

- No session state means horizontal scaling without sticky sessions or shared session stores
- Refresh token rotation is not implemented (token is NOT rotated on refresh) — a deliberate simplicity trade-off for an MVP. See [ADR-003](./ADR.md#adr-003-refresh-token-non-rotation)
- SHA-256 hashing of refresh tokens means a database breach does not expose valid tokens

### 3.3 Role-Based Access Control at Method Level

**Decision:** `@PreAuthorize("hasRole(...)")` on controller methods, not URL patterns.

**Rationale:**

- Method-level security is more maintainable — the security rule lives next to the business logic
- Avoids brittle URL-pattern matching in `SecurityConfig` that breaks on route changes
- Roles (`ADMIN`, `CLERK`, `APPROVER`, `RESIDENT`) map directly to barangay organizational roles

### 3.4 Ownership Validation at Service Layer

**Decision:** For portal (resident) endpoints, the authenticated resident's identity is always resolved from the JWT, never from request parameters.

**Rationale:**

- Prevents horizontal privilege escalation (resident A accessing resident B's clearances by changing an ID in the URL)
- The service layer validates ownership before returning any data

### 3.5 Payment Gateway Abstraction

**Decision:** `PaymentGateway` is a Java interface with `StubPaymentGateway` as the initial implementation.

**Rationale:**

- Barangay systems in the Philippines often start with cash-only operations
- The stub simulates a successful online payment gateway, allowing the full workflow to be tested without real payment integration
- Future plug-in of PayMongo or Maya requires only a new `PaymentGateway` implementation — no service layer changes

### 3.6 Idempotent Payments

**Decision:** Composite unique index on `(idempotency_key, initiated_by_user_id)` with 24-hour TTL enforced in application logic.

**Rationale:**

- Prevents duplicate charges on network retries or browser refresh
- DB constraint as final guard even if application-level check races

### 3.7 Atomic Clearance Number Generation

**Decision:** PostgreSQL `ON CONFLICT DO UPDATE RETURNING` on `clearance_number_sequence` table.

**Rationale:**

- Prevents duplicate clearance numbers under concurrent requests without application-level locking
- `REQUIRES_NEW` transaction propagation ensures the sequence commit is independent of the parent transaction, preventing rollback of the sequence increment on outer transaction failure

### 3.8 Singleton Tables for Settings

**Decision:** `barangay_settings` and `fee_config` tables use `CHECK (id = 1)` to enforce a single row at the database level.

**Rationale:**

- Simpler than a key-value store for a fixed, small configuration set
- The constraint is enforced at the DB level, not just the application level
- `PUT` semantics (upsert) rather than `POST` — no resource creation, just update

---

## 4. System Architecture

### 4.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser / Client                        │
│                     Next.js 14 (Port 3000)                      │
│          Resident Portal  │  Back-office Dashboard              │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS
                           ▼
              ┌────────────────────────┐
              │      Nginx Reverse     │
              │         Proxy          │
              │  /api → :8080          │
              │  /    → :3000          │
              └────────────┬───────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│               Spring Boot 3.3.4 (Port 8080)                     │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ identity │  │residents │  │clearance │  │  payments     │  │
│  │  module  │  │  module  │  │  module  │  │   module      │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────┘  │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │   pdf    │  │settings  │  │ reports  │  │    shared     │  │
│  │  module  │  │  module  │  │  module  │  │ (security,    │  │
│  └──────────┘  └──────────┘  └──────────┘  │  exception,  │  │
│                                             │  util)       │  │
│                                             └───────────────┘  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ JDBC (HikariCP)
                           ▼
              ┌────────────────────────┐
              │    PostgreSQL 16        │
              │  (Flyway migrations)    │
              └────────────────────────┘
```

### 4.2 Request Lifecycle

```
Client Request
    │
    ▼
JwtAuthFilter (shared/security)
    │  Extract Bearer token from Authorization header
    │  Validate signature + expiry (no DB hit)
    │  Populate SecurityContext with UserPrincipal
    │
    ▼
Controller (@RestController)
    │  Input validation (@Valid, @NotBlank, etc.)
    │  @PreAuthorize role check
    │  Delegates to Service
    │
    ▼
Service (@Service, @Transactional)
    │  Business logic
    │  Ownership validation (portal routes)
    │  State machine enforcement
    │  Calls Repository
    │
    ▼
Repository (Spring Data JPA)
    │  JpaRepository + JpaSpecificationExecutor
    │  Dynamic queries via SpecificationBuilder
    │
    ▼
PostgreSQL 16
    │
    ▼
MapStruct Mapper
    │  Entity → DTO (never expose entity directly)
    │
    ▼
ResponseEntity<DTO>
    │
    ▼
GlobalExceptionHandler (@RestControllerAdvice)
    (intercepts exceptions → standardized ErrorResponse)
```

### 4.3 Layer Responsibilities

| Layer          | Responsibility                                      | Annotations                                  |
| -------------- | --------------------------------------------------- | -------------------------------------------- |
| **Controller** | HTTP routing, input validation, authorization gate  | `@RestController`, `@PreAuthorize`, `@Valid` |
| **Service**    | Business logic, state machine, transaction boundary | `@Service`, `@Transactional`                 |
| **Repository** | Data access, dynamic queries                        | `@Repository`, extends `JpaRepository`       |
| **Entity**     | Database schema mapping, constraints                | `@Entity`, `@Table`, `@Column`               |
| **DTO**        | Data contract between layers, never exposes entity  | `@Data`, `@Builder` (Lombok)                 |
| **Mapper**     | Compile-time entity↔DTO conversion                  | `@Mapper` (MapStruct)                        |

---

## 5. Module Structure

### 5.1 Package Layout

```
com.barangay.clearance/
├── BarangayClearanceApplication.java
│
├── identity/                          # Bounded Context: Authentication & Users
│   ├── controller/
│   │   ├── AuthController.java        # /api/v1/auth/**
│   │   └── UserController.java        # /api/v1/users/**
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── TokenResponse.java
│   │   ├── RefreshRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── CreateStaffRequest.java
│   │   └── UserDTO.java
│   ├── entity/
│   │   ├── User.java                  # Role, UserStatus enums
│   │   └── RefreshToken.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── RefreshTokenRepository.java
│   └── service/
│       ├── AuthService.java
│       └── JwtService.java
│
├── residents/                         # Bounded Context: Resident Registry
│   ├── controller/
│   │   └── ResidentController.java    # /api/v1/residents/**
│   ├── dto/
│   │   ├── ResidentDTO.java
│   │   ├── CreateResidentRequest.java
│   │   ├── UpdateResidentRequest.java
│   │   └── ResidentSearchRequest.java
│   ├── entity/
│   │   └── Resident.java              # Gender, ResidentStatus enums
│   ├── repository/
│   │   └── ResidentRepository.java
│   └── service/
│       ├── ResidentService.java
│       └── mapper/
│           └── ResidentMapper.java
│
├── clearance/                         # Bounded Context: Clearance Workflow
│   ├── controller/
│   │   ├── ClearanceController.java        # /api/v1/clearances/** (backoffice)
│   │   └── PortalClearanceController.java  # /api/v1/me/clearances/** (portal)
│   ├── dto/
│   │   ├── ClearanceRequestDTO.java
│   │   ├── CreateClearanceRequest.java
│   │   ├── ClearanceSummaryDTO.java
│   │   └── RejectRequest.java
│   ├── entity/
│   │   ├── ClearanceRequest.java      # Purpose, Urgency, Status, PaymentStatus enums
│   │   └── ClearanceNumberSequence.java
│   ├── repository/
│   │   ├── ClearanceRequestRepository.java
│   │   └── ClearanceNumberSequenceRepository.java
│   └── service/
│       ├── ClearanceService.java
│       ├── ClearanceNumberService.java
│       ├── ClearanceStatusChangedEvent.java  # Spring ApplicationEvent for status transitions
│       ├── ClearanceAuditListener.java       # @EventListener — translates events → audit entries
│       └── mapper/
│           └── ClearanceMapper.java
│
├── payments/                          # Bounded Context: Payment Processing
│   ├── controller/
│   │   └── PaymentController.java
│   ├── dto/
│   │   └── PaymentDTO.java
│   ├── entity/
│   │   └── Payment.java               # PaymentStatus, PaymentMethod enums
│   ├── gateway/
│   │   ├── PaymentGateway.java        # Strategy interface
│   │   ├── PaymentRequest.java
│   │   ├── PaymentResult.java
│   │   └── StubPaymentGateway.java    # Always-success stub
│   ├── repository/
│   │   └── PaymentRepository.java
│   └── service/
│       ├── PaymentService.java
│       └── mapper/
│           └── PaymentMapper.java
│
├── pdf/                               # Bounded Context: Document Generation
│   └── service/
│       ├── ClearancePdfService.java
│       └── ClearancePdfServiceImpl.java
│
├── settings/                          # Bounded Context: Configuration
│   ├── controller/
│   │   └── SettingsController.java    # /api/v1/settings/**
│   ├── dto/
│   │   ├── BarangaySettingsDTO.java
│   │   └── FeeConfigDTO.java
│   ├── entity/
│   │   ├── BarangaySettings.java      # Singleton (id=1)
│   │   └── FeeConfig.java            # Singleton (id=1)
│   ├── repository/
│   │   ├── BarangaySettingsRepository.java
│   │   └── FeeConfigRepository.java
│   └── service/
│       └── SettingsService.java
│
├── reports/                           # Bounded Context: Reporting
│   ├── controller/
│   │   └── ReportsController.java     # /api/v1/reports/**
│   ├── dto/
│   │   ├── ReportRowDTO.java
│   │   └── ReportRowProjection.java
│   ├── repository/
│   │   └── ReportRepository.java
│   └── service/
│       └── ReportsService.java
│
└── shared/                            # Cross-Cutting Concerns
    ├── audit/
    │   ├── AuditAction.java           # String constants for every auditable event
    │   ├── AuditAsyncConfig.java      # Dedicated audit-pool thread pool + TaskDecorator
    │   ├── AuditLog.java              # Immutable @Entity mapped to audit_logs
    │   ├── AuditLogRepository.java    # JpaRepository + JpaSpecificationExecutor
    │   └── AuditService.java          # @Async + REQUIRES_NEW write gateway
    ├── exception/
    │   ├── AppException.java
    │   ├── ErrorResponse.java
    │   └── GlobalExceptionHandler.java
    ├── security/
    │   ├── SecurityConfig.java        # @Profile("!no-auth") — JWT enabled
    │   ├── LocalSecurityConfig.java   # @Profile("no-auth") — permits all
    │   ├── JwtAuthFilter.java
    │   └── UserPrincipal.java
    └── util/
        ├── PageResponse.java
        └── SpecificationBuilder.java
```

### 5.2 Module Dependency Rules

```
identity ──────────────────────────────────────────────────────┐
residents ──────────────────────────────────────────────────── ▼
clearance ──────────────────────────────────────────────── shared
payments ───────────────────────────────────────────────────── ▲
pdf ────────────────────────────────────────────────────────── │
settings ───────────────────────────────────────────────────── │
reports ────────────────────────────────────────────────────── │
```

**Rules enforced by convention:**

- No module imports JPA entities from another module
- Cross-module data is passed as primitive types or DTOs (never entity references)
- `clearance` module references `resident` data by UUID only; name is enriched post-fetch (denormalization)
- `pdf` module receives all data it needs as parameters — no repository access

---

## 6. Database Design & Entity Relationships

### 6.1 Entity Relationship Diagram

```
┌──────────────────────┐         ┌─────────────────────────┐
│        users         │         │     barangay_settings   │
├──────────────────────┤         ├─────────────────────────┤
│ PK  id (UUID)        │         │ PK  id = 1 (CHECK)      │
│     email (UNIQUE)   │         │     barangay_name        │
│     password_hash    │         │     municipality         │
│     first_name       │         │     province             │
│     last_name        │         │     captain_name         │
│     role             │         │     logo (BYTEA)         │
│     status           │         │     logo_mime_type       │
│     must_change_pwd  │         │     updated_at           │
│     created_at       │         └─────────────────────────┘
│     updated_at       │
└──────┬───────────────┘         ┌─────────────────────────┐
       │                         │       fee_config        │
       │ 1                       ├─────────────────────────┤
       │                         │ PK  id = 1 (CHECK)      │
       ├──────────────────────┐  │     standard_fee        │
       │                      │  │     rush_fee            │
       ▼ 0..N                 │  │     updated_at          │
┌──────────────────────┐      │  └─────────────────────────┘
│    refresh_tokens    │      │
├──────────────────────┤      │  ┌─────────────────────────┐
│ PK  id (UUID)        │      │  │ clearance_number_seq    │
│ FK  user_id          │      │  ├─────────────────────────┤
│     token_hash       │      │  │ PK  year_month (VARCHAR)│
│     expires_at       │      │  │     last_seq (INTEGER)  │
│     revoked          │      │  └─────────────────────────┘
│     created_at       │      │
└──────────────────────┘      │
                              │
       ┌───────────────────── │ ────────────────────────────┐
       │                      │                             │
       ▼ 0..1 (user_id)       │                             │
┌──────────────────────┐      │                             │
│      residents       │      │                             │
├──────────────────────┤      │                             │
│ PK  id (UUID)        │      │                             │
│ FK  user_id (NULL ok)│      │                             │
│     first_name       │      │                             │
│     middle_name      │      │                             │
│     last_name        │      │                             │
│     birth_date       │      │                             │
│     gender           │      │                             │
│     address          │      │                             │
│     contact_number   │      │                             │
│     email            │      │                             │
│     status           │      │                             │
│     created_at       │      │
│     updated_at       │      │
└──────┬───────────────┘      │
       │                      │
       │ 1                    │
       │                      │
       ▼ 0..N                 │
┌──────────────────────────────────────────────────────────┐
│                    clearance_requests                    │
├──────────────────────────────────────────────────────────┤
│ PK  id (UUID)                                            │
│     clearance_number (VARCHAR(12), nullable)             │
│ FK  resident_id → residents.id                           │
│ FK  requested_by → users.id                             │
│     purpose (VARCHAR(50))                                │
│     purpose_other (VARCHAR(255))                         │
│     urgency (STANDARD | RUSH)                            │
│     fee_amount (NUMERIC)                                 │
│     copies (INTEGER)                                     │
│     status (DRAFT|FOR_APPROVAL|APPROVED|REJECTED|RELEASED│
│     payment_status (UNPAID | PAID | WAIVED)              │
│     notes                                                │
│ FK  reviewed_by → users.id (nullable)                   │
│     reviewed_at                                          │
│     issued_at                                            │
│     created_at                                           │
│     updated_at                                           │
└──────┬───────────────────────────────────────────────────┘
       │
       │ 1
       │
       ▼ 0..N
┌──────────────────────────────────────────────────────────┐
│                        payments                          │
├──────────────────────────────────────────────────────────┤
│ PK  id (UUID)                                            │
│ FK  clearance_request_id → clearance_requests.id        │
│     amount (NUMERIC)                                     │
│     idempotency_key (VARCHAR(64))                        │
│ FK  initiated_by_user_id → users.id                     │
│     payment_method (STUB | CASH)                         │
│     provider (VARCHAR(50))                               │
│     status (PENDING | SUCCESS | FAILED)                  │
│     response_body (TEXT — cached JSON)                   │
│     idempotency_expires_at                               │
│     created_at                                           │
│     updated_at                                           │
│                                                          │
│ UNIQUE INDEX: (idempotency_key, initiated_by_user_id)    │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│                       audit_logs                         │
├──────────────────────────────────────────────────────────┤
│ PK  id (UUID)                                            │
│ FK  user_id → users.id (ON DELETE SET NULL)             │
│     action (VARCHAR(100))                                │
│     entity_type (VARCHAR(50))                            │
│     entity_id (UUID)                                     │
│     details (TEXT)                                       │
│     ip_address (VARCHAR(45))                             │
│     created_at                                           │
└──────────────────────────────────────────────────────────┘
```

### 6.2 Table Catalog

| Table                       | Rows        | PK Type    | Notes                           |
| --------------------------- | ----------- | ---------- | ------------------------------- |
| `users`                     | Many        | UUID       | `gen_random_uuid()` default     |
| `refresh_tokens`            | Many        | UUID       | `token_hash` is SHA-256 hex     |
| `residents`                 | Many        | UUID       | `user_id` nullable (walk-ins)   |
| `barangay_settings`         | **1**       | INTEGER    | `CHECK (id = 1)` singleton      |
| `fee_config`                | **1**       | INTEGER    | `CHECK (id = 1)` singleton      |
| `clearance_number_sequence` | 1 per month | VARCHAR(7) | `year_month` = "YYYY-MM"        |
| `clearance_requests`        | Many        | UUID       | Core workflow table             |
| `payments`                  | Many        | UUID       | Composite unique on idempotency |
| `audit_logs`                | Many        | UUID       | Append-only; never updated      |

### 6.3 Key Indexes

| Index                      | Table                | Columns                                 | Type                |
| -------------------------- | -------------------- | --------------------------------------- | ------------------- |
| `idx_users_email`          | `users`              | `email`                                 | B-tree              |
| `idx_users_role_status`    | `users`              | `role, status`                          | B-tree (composite)  |
| `idx_residents_name`       | `residents`          | `lower(last_name), lower(first_name)`   | B-tree (functional) |
| `idx_cr_status`            | `clearance_requests` | `status`                                | B-tree              |
| `idx_cr_issued_at`         | `clearance_requests` | `issued_at`                             | B-tree              |
| `idx_payments_idempotency` | `payments`           | `idempotency_key, initiated_by_user_id` | **UNIQUE** B-tree   |

### 6.4 Flyway Migration History

| Version | File                                 | Changes                                                                       |
| ------- | ------------------------------------ | ----------------------------------------------------------------------------- |
| V1      | `V1__initial_schema.sql`             | All 9 tables, PKs, FKs, basic indexes                                         |
| V2      | `V2__seed_settings.sql`              | Insert singleton rows for settings + fee config                               |
| V3      | `V3__seed_admin.sql`                 | Insert initial admin user (`must_change_password=true`)                       |
| V4      | `V4__expand_user_status.sql`         | Add `PENDING_VERIFICATION`, `REJECTED`, `DEACTIVATED` to `users.status` CHECK |
| V5      | `V5__fix_admin_password.sql`         | Admin password correction                                                     |
| V6      | `V6__clearance_extra_columns.sql`    | Add `purpose_other`, `copies` columns to `clearance_requests`                 |
| V7      | `V7__fix_year_month_column_type.sql` | ALTER `year_month` to VARCHAR(7)                                              |
| V8      | `V8__payments_add_columns.sql`       | Add `idempotency_expires_at`, `payment_method` to `payments`                  |

---

## 7. Security Architecture

### 7.1 Authentication Flow

```
┌─────────┐      POST /api/v1/auth/login          ┌─────────────┐
│ Client  │ ─────────────────────────────────────► │  Auth       │
│         │    { email, password }                 │  Controller │
│         │                                        └──────┬──────┘
│         │                                               │ validate credentials
│         │                                               ▼
│         │                                        ┌─────────────┐
│         │                                        │  Auth       │
│         │                                        │  Service    │
│         │                                        └──────┬──────┘
│         │                                               │ BCrypt verify
│         │                                               │ check user.status
│         │                                               │
│         │                                               ▼
│         │                                        ┌─────────────┐
│         │                                        │  JWT        │
│         │                                        │  Service    │
│         │                                        └──────┬──────┘
│         │                                               │ generate access token
│         │                                               │ (15 min, HMAC-SHA256)
│         │                                               │ generate refresh token
│         │                                               │ (UUID v4, 7 days)
│         │                                               │ SHA-256 hash → DB
│         │                                               │
│         │   { accessToken, refreshToken, expiresIn }    │
│         │ ◄─────────────────────────────────────────── ┘
└─────────┘
```

### 7.2 Request Authorization Flow

```
Client Request (Authorization: Bearer <accessToken>)
    │
    ▼
┌──────────────────────────────────────────────────┐
│                  JwtAuthFilter                   │
│                                                  │
│  1. Extract token from Authorization header      │
│  2. Validate HMAC-SHA256 signature               │
│  3. Check token expiry                           │
│  4. Extract userId, email, role claims           │
│  5. Build UserPrincipal (UserDetails)            │
│  6. Set SecurityContext                          │
│                                                  │
│  *** NO DATABASE HIT ***                         │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼ (if valid)
┌──────────────────────────────────────────────────┐
│              Spring Security Filter               │
│       @PreAuthorize("hasRole('CLERK')")          │
│       Method Security (AOP-based)                │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
              Controller → Service
```

### 7.3 Token Specifications

| Token         | Type           | Algorithm   | Expiry     | Storage                      |
| ------------- | -------------- | ----------- | ---------- | ---------------------------- |
| Access Token  | JWT            | HMAC-SHA256 | 15 minutes | Client memory / localStorage |
| Refresh Token | Opaque UUID v4 | —           | 7 days     | DB (SHA-256 hash only)       |

**JWT Claims:**

```json
{
  "sub": "uuid-of-user",
  "email": "user@example.com",
  "role": "RESIDENT",
  "mustChangePassword": false,
  "iat": 1704067200,
  "exp": 1704068100
}
```

### 7.4 Role Permission Matrix

| Endpoint Category          | ADMIN | APPROVER | CLERK | RESIDENT |
| -------------------------- | ----- | -------- | ----- | -------- |
| Create staff account       | ✓     | —        | —     | —        |
| Manage barangay settings   | ✓     | —        | —     | —        |
| View all clearances        | ✓     | ✓        | ✓     | —        |
| Create walk-in clearance   | ✓     | —        | ✓     | —        |
| Approve / Reject clearance | ✓     | ✓        | —     | —        |
| Release clearance          | ✓     | —        | ✓     | —        |
| Record cash payment        | ✓     | —        | ✓     | —        |
| Download any PDF           | ✓     | —        | ✓     | —        |
| Manage residents           | ✓     | —        | ✓     | —        |
| Activate pending accounts  | ✓     | —        | ✓     | —        |
| View dashboard summary     | ✓     | ✓        | ✓     | —        |
| Submit own clearance       | —     | —        | —     | ✓        |
| View own clearances        | —     | —        | —     | ✓        |
| Download own PDF           | —     | —        | —     | ✓        |
| Pay own clearance          | —     | —        | —     | ✓        |

### 7.5 CORS Configuration

| Setting           | Value                         |
| ----------------- | ----------------------------- |
| Allowed Origins   | `http://localhost:3000` (dev) |
| Allowed Methods   | GET, POST, PUT, PATCH, DELETE |
| Allow Credentials | true                          |
| CSRF              | Disabled (stateless JWT)      |

---

## 8. Clearance State Machine

### 8.1 State Diagram

```
                    ┌─────────────────────────────┐
                    │         DRAFT               │
                    │  (walk-in initial state OR  │
                    │   re-submitted by resident) │
                    └──────────────┬──────────────┘
                                   │ submit / create
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │        FOR_APPROVAL          │
                    │   (awaiting staff review)    │
                    └──────────┬──────┬───────────┘
                               │      │
                    approve    │      │ reject
                               │      │
              ┌────────────────┘      └─────────────────┐
              │                                         │
              ▼                                         ▼
┌─────────────────────────┐              ┌─────────────────────────┐
│        APPROVED          │              │        REJECTED          │
│   payment_status=UNPAID  │              │  (notes contain reason)  │
└──────────┬──────────────┘              └──────────┬──────────────┘
           │                                         │
     pay   │ (payment_status → PAID)                 │ resubmit
           │                                         │
           ▼                                         ▼
┌─────────────────────────┐              ┌─────────────────────────┐
│        APPROVED          │              │         DRAFT            │
│   payment_status=PAID    │              │  (resident edits and    │
└──────────┬──────────────┘              │   resubmits)            │
           │                             └─────────────────────────┘
    release│ (assign clearance_number)
           │
           ▼
┌─────────────────────────┐
│        RELEASED          │
│  (clearance_number set)  │
│  (issued_at set)         │
│  (PDF downloadable)      │
└─────────────────────────┘
```

### 8.2 Transition Guards

| Transition                  | Guard Conditions                                             |
| --------------------------- | ------------------------------------------------------------ |
| → FOR_APPROVAL              | Resident has ACTIVE status                                   |
| FOR_APPROVAL → APPROVED     | Caller has APPROVER or ADMIN role                            |
| FOR_APPROVAL → REJECTED     | Caller has APPROVER or ADMIN role; rejection reason required |
| REJECTED → DRAFT (resubmit) | Caller is the original requester (ownership validated)       |
| APPROVED → RELEASED         | Caller has CLERK or ADMIN role; `payment_status = PAID`      |
| Any state → RELEASED        | Blocked if `payment_status = UNPAID`                         |

### 8.3 Clearance Number Format

```
Format:  YYYY-MM-NNNN
Example: 2025-02-0001

where:
  YYYY = Year (4 digits)
  MM   = Month (2 digits, zero-padded)
  NNNN = Sequential counter per month (4 digits, zero-padded, resets each month)
```

**Generation algorithm:**

```sql
INSERT INTO clearance_number_sequence (year_month, last_seq)
VALUES ('2025-02', 1)
ON CONFLICT (year_month)
DO UPDATE SET last_seq = clearance_number_sequence.last_seq + 1
RETURNING last_seq;
```

The returned sequence is formatted as `String.format("%s-%04d", yearMonth, seq)`.

---

## 9. Payment Subsystem

### 9.1 Payment Gateway Pattern

```
PaymentService
    │
    │ uses
    ▼
PaymentGateway (interface)
    │
    ├── StubPaymentGateway  (active: payment.provider=stub)
    │       Always returns SUCCESS — simulates real gateway
    │
    └── [Future: PayMongoGateway, MayaGateway]
            Plug-in by implementing PaymentGateway interface
            No service layer changes required
```

### 9.2 Idempotency Protocol

```
Client sends: POST /api/v1/clearances/{id}/payments
Headers: Idempotency-Key: <UUID v4>

PaymentService checks:
    ┌─────────────────────────────────────────────────────┐
    │         Existing record for (key, userId)?          │
    └──────────────────────────────────────────────────── ┘
                              │
                    ┌─────────┴──────────┐
                    │                    │
                    ▼ YES                ▼ NO
    ┌───────────────────────────┐  ┌─────────────────────────────┐
    │ Check status + expiry     │  │ Check clearance status      │
    └─────────────┬─────────────┘  │ (must be APPROVED)          │
                  │                └──────────────┬──────────────┘
          ┌───────┼──────────────┐                │
          │       │              │                │ Insert PENDING record
     PENDING SCES/FAIL      EXPIRED          (may hit UNIQUE constraint)
          │       │              │                │
    409   │  return cached  create new       Call gateway
   Conflict│  response       payment              │
          │   (idempotent:   record               │
          │    true)                    ┌─────────┴──────────┐
          │                            │                    │
          │                          SUCCESS             FAILED
          │                            │                    │
          │                   update status          update status
          │                   mark clearance          (FAILED)
          │                   PAID (markPaid)
          │
          └─────────────────────────────────────────────────┘
```

### 9.3 Cash Payment Flow

```
POST /api/v1/clearances/{id}/mark-paid  (CLERK/ADMIN only)
    │
    ▼
PaymentService.markPaid(clearanceId, staffUserId)
    │
    ├── If clearance already PAID: return existing payment (idempotent)
    │
    ├── Validate clearance status = APPROVED
    │
    ├── Create Payment record:
    │     payment_method = CASH
    │     provider = CASH
    │     status = SUCCESS
    │
    └── Mark clearance payment_status = PAID
```

---

## 10. PDF Generation

### 10.1 PDF Document Layout

```
┌──────────────────────────────────────────────────────────┐
│  [LOGO]   BARANGAY <NAME>                                │
│           Municipality of <MUN>, <PROVINCE>              │
├──────────────────────────────────────────────────────────┤
│                 BARANGAY CLEARANCE                        │
├──────────────────────────────────────────────────────────┤
│  Clearance No.:  2025-02-0001                            │
│  Date Issued:    February 26, 2025                       │
│  Valid Until:    February 26, 2026                       │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  This is to certify that:                               │
│                                                          │
│  Name:    JUAN DELA CRUZ                                 │
│  Address: 123 Main Street, Barangay Example              │
│  Purpose: Employment                                     │
│                                                          │
│  ...has been a resident of good standing...              │
│                                                          │
├──────────────────────────────────────────────────────────┤
│                    [Signature Block]                      │
│                    HON. MARIA SANTOS                     │
│                    Barangay Captain                      │
└──────────────────────────────────────────────────────────┘
```

### 10.2 Generation Pipeline

```
ClearanceController.downloadPdf(id)
    │
    ▼
ClearanceService.getReleasedEntity(id)     ← must be RELEASED
    │
    ▼
ClearanceService.getResidentForClearance(residentId)
    │
    ▼
SettingsService.getSettings()              ← barangay name, captain, logo
    │
    ▼
ClearancePdfServiceImpl.generate(clearance, resident, settings)
    │  PDFBox: create document → add page → draw elements
    ▼
byte[] pdfBytes
    │
    ▼
ResponseEntity<byte[]>
  Content-Type: application/pdf
  Content-Disposition: attachment; filename="clearance-2025-02-0001.pdf"
```

---

## 11. Configuration & Profiles

### 11.1 Profile Summary

| Profile   | Database              | Auth         | Use Case                              |
| --------- | --------------------- | ------------ | ------------------------------------- |
| `local`   | PostgreSQL :5433      | JWT enabled  | Default local development             |
| `no-auth` | (combined with local) | **Disabled** | Frontend dev without token management |
| `test`    | H2 / test PostgreSQL  | JWT enabled  | Integration tests                     |
| `prod`    | PostgreSQL (env vars) | JWT enabled  | Production deployment                 |

### 11.2 Key Configuration Properties

```yaml
# JWT
app.jwt.secret: <256-bit minimum HMAC secret>
app.jwt.access-token-expiry-ms: 900000 # 15 minutes
app.jwt.refresh-token-expiry-ms: 604800000 # 7 days

# Payment
payment.provider: stub # stub | paymongo | maya
payment.stub.always-success: true

# Flyway
spring.flyway.enabled: true
spring.flyway.locations: classpath:db/migration

# JPA
spring.jpa.hibernate.ddl-auto: validate # Flyway owns schema
```

### 11.3 Environment Variables (Production)

| Variable                | Description                                      |
| ----------------------- | ------------------------------------------------ |
| `DB_URL`                | `jdbc:postgresql://host:5432/barangay_clearance` |
| `DB_USERNAME`           | PostgreSQL username                              |
| `DB_PASSWORD`           | PostgreSQL password                              |
| `JWT_SECRET`            | HMAC-SHA256 secret (min 256 bits)                |
| `JWT_ACCESS_EXPIRY_MS`  | Access token expiry in milliseconds              |
| `JWT_REFRESH_EXPIRY_MS` | Refresh token expiry in milliseconds             |

---

## 12. Infrastructure & Deployment

### 12.1 Local Development Setup

```
docker-compose.dev.yml
├── postgres:16              port 5433 → 5432
│     volume: postgres_data
│
└── [optional] nginx         port 80

Backend: ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
Frontend: cd frontend && npm run dev
```

### 12.2 Production Topology

```
Internet
    │
    ▼
Nginx (port 80/443)
    │
    ├── /api/* → Spring Boot :8080
    │               │
    │               └── PostgreSQL :5432
    │
    └── /* → Next.js :3000
```

### 12.3 Build & Deployment

```bash
# Build JAR
cd backend && ./mvnw clean package -DskipTests

# Run with production profile
java -jar target/barangay-clearance-*.jar \
  --spring.profiles.active=prod \
  --DB_URL=... --JWT_SECRET=...

# Build frontend
cd frontend && npm run build && npm start
```

---

## 13. Cross-Cutting Concerns

### 13.1 Error Handling

All exceptions are normalized by `GlobalExceptionHandler` into a standard `ErrorResponse`:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Clearance request not found",
  "timestamp": "2025-02-26T10:30:00Z",
  "path": "/api/v1/clearances/non-existent-id",
  "details": []
}
```

**Handled exceptions:**

| Exception                         | HTTP Status                      |
| --------------------------------- | -------------------------------- |
| `AppException`                    | Configured (400/401/403/404/409) |
| `MethodArgumentNotValidException` | 400 (with field-level details)   |
| `ConstraintViolationException`    | 400                              |
| `AccessDeniedException`           | 403                              |
| `AuthenticationException`         | 401                              |
| Unhandled `RuntimeException`      | 500                              |

### 13.2 Pagination

All list endpoints return paginated responses via `PageResponse<T>`:

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

Query parameters: `?page=0&size=20&sort=createdAt,desc`

### 13.3 Dynamic Query Building

`SpecificationBuilder<T>` wraps Spring Data JPA `Specification` for composable, null-safe filters:

```java
Specification<ClearanceRequest> spec = SpecificationBuilder.<ClearanceRequest>builder()
    .equals("status", status)
    .equals("paymentStatus", paymentStatus)
    .greaterThanOrEqual("createdAt", from)
    .lessThanOrEqual("createdAt", to)
    .build();
```

### 13.4 Annotation Processor Order

**Critical:** Lombok must run before MapStruct. Enforced via `lombok-mapstruct-binding` in `pom.xml` annotationProcessorPaths.

```
Compilation order:
1. Lombok  → generates getters/setters/builders
2. MapStruct → uses Lombok-generated methods to build mappers
3. JPA ModelGen → generates metamodel classes (_ClearanceRequest_, etc.)
```

After any entity or mapper change:

```bash
./mvnw clean compile
```

### 13.5 Audit Trail

Audit logging is fully implemented across all modules via the `shared/audit/` package.

#### Architecture

`audit_logs` is append-only. Every state-changing operation is recorded with:

- `user_id` — actor UUID (`null` for unauthenticated events such as failed logins)
- `action` — string constant from `AuditAction` (e.g., `CLEARANCE_APPROVED`)
- `entity_type` — affected entity class name (max 50 chars)
- `entity_id` — affected entity's primary key
- `details` — human-readable or JSON description of what changed
- `ip_address` — real client IP (resolved via `X-Forwarded-For` → `remoteAddr`)
- `created_at` — database-set timestamp, immutable

#### `AuditService` Design

```java
@Async("auditTaskExecutor")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void log(UUID userId, String action, String entityType, UUID entityId, String details)
```

Two key design decisions:

1. **`@Async` on a dedicated thread pool** (`audit-pool-*`, 2 core / 5 max / 500 queue) — audit writes are dispatched off the request thread so they add zero latency to user-facing responses.
2. **`REQUIRES_NEW` propagation** — the audit write commits in its own transaction, independent of the caller. If the caller's transaction rolls back (e.g. a downstream constraint violation), the audit record is still persisted.

Audit failure is silently swallowed: the `catch` block logs the error at `ERROR` level but never re-throws. A missing audit entry is always preferable to a failed user request.

#### IP Address Resolution

`AuditService` reads the client IP from `RequestContextHolder` — no need to pass `HttpServletRequest` into every service method. The `AuditAsyncConfig` task decorator copies the request context to the async thread so the IP is available even after the request thread has moved on.

```
Request thread  ──► AuditService.log() dispatched to audit-pool
                         └── RequestContext propagated via TaskDecorator
                              └── X-Forwarded-For / remoteAddr resolved
```

#### Clearance State Machine — Event-Driven Auditing

Clearance status transitions use a Spring `ApplicationEvent` pattern instead of direct `AuditService` calls. `ClearanceService` publishes a `ClearanceStatusChangedEvent`; `ClearanceAuditListener` translates it to an audit entry. This keeps audit concerns out of the state machine logic.

```
ClearanceService.approve() ──► publishEvent(ClearanceStatusChangedEvent)
                                    └── ClearanceAuditListener.onStatusChanged()
                                             └── AuditService.log(actorId, "CLEARANCE_APPROVED", ...)
```

#### Audit Action Catalog

All action constants live in `AuditAction` (a non-instantiable constants class, not an enum, to keep the set open for extension):

| Category       | Action Constants                                                                                                                             |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| **Auth**       | `USER_REGISTERED`, `USER_LOGIN`, `USER_LOGIN_FAILED`, `USER_LOGOUT`, `USER_TOKEN_REFRESHED`, `USER_PASSWORD_CHANGED`                         |
| **Staff**      | `STAFF_CREATED`, `STAFF_ACTIVATED`, `STAFF_DEACTIVATED`, `STAFF_ROLE_CHANGED`, `STAFF_PASSWORD_RESET`                                        |
| **Residents**  | `RESIDENT_CREATED`, `RESIDENT_UPDATED`, `RESIDENT_ACTIVATED`                                                                                 |
| **Clearances** | `CLEARANCE_SUBMITTED`, `CLEARANCE_RESUBMITTED`, `CLEARANCE_APPROVED`, `CLEARANCE_REJECTED`, `CLEARANCE_RELEASED`, `CLEARANCE_PDF_DOWNLOADED` |
| **Payments**   | `PAYMENT_INITIATED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `PAYMENT_CASH_RECORDED`                                                            |
| **Settings**   | `SETTINGS_UPDATED`, `SETTINGS_LOGO_UPLOADED`, `FEES_UPDATED`                                                                                 |

#### Call Sites by Module

| Module    | File                                               | Actions logged                                                                     |
| --------- | -------------------------------------------------- | ---------------------------------------------------------------------------------- |
| identity  | `AuthService`                                      | Auth events (login, logout, register, token refresh, password change)              |
| identity  | `UserService`                                      | Staff lifecycle events (create, activate, deactivate, role change, password reset) |
| residents | `ResidentService`                                  | Resident create, update, portal activation                                         |
| clearance | `ClearanceAuditListener`                           | All clearance state transitions (via event)                                        |
| clearance | `ClearanceController`, `PortalClearanceController` | PDF downloads (direct call — not a state transition)                               |
| payments  | `PaymentService`                                   | Payment initiated, success, failed, cash recorded                                  |
| settings  | `SettingsService`                                  | Settings update, logo upload, fee update                                           |

---

## 14. Concurrency

### 14.1 Concurrency Risks and Mitigations

The system has three areas where concurrent requests can produce incorrect results without explicit protection.

#### 14.1.1 Clearance Number Duplication

**Risk:** Two requests calling `ClearanceNumberService.next()` simultaneously could read the same `last_seq` value and generate identical clearance numbers.

**Mitigation:** PostgreSQL `ON CONFLICT DO UPDATE RETURNING` executes atomically at the database level. The `INSERT … ON CONFLICT` statement is a single atomic read-modify-write — no application-level lock is needed.

```sql
-- This entire statement is atomic in PostgreSQL
INSERT INTO clearance_number_sequence (year_month, last_seq)
VALUES ('2025-02', 1)
ON CONFLICT (year_month)
DO UPDATE SET last_seq = clearance_number_sequence.last_seq + 1
RETURNING last_seq;
```

**Transaction propagation:** `ClearanceNumberService.next()` runs in `REQUIRES_NEW`. Even if the outer transaction (e.g., the release operation) rolls back, the sequence row is already committed — preventing the same number from being reissued.

---

#### 14.1.2 Duplicate Payments (Race Condition)

**Risk:** A resident double-taps the "Pay" button, or a network retry fires a second payment request before the first response arrives. Both requests reach the server before either has committed a `payments` row.

**Mitigation — two layers:**

1. **Application-layer check** — `PaymentService` checks for an existing record with the same `(idempotency_key, initiated_by_user_id)` before inserting. The first request to pass this check creates the PENDING row.
2. **Database unique constraint** — `UNIQUE INDEX (idempotency_key, initiated_by_user_id)` catches any race between the application check and the INSERT. The losing concurrent request receives a `DataIntegrityViolationException`, which `PaymentService` translates to a `409 Conflict`.

```
Request A ──────────────────────────────────────────► DB INSERT (wins) → PENDING
Request B ──────────────► (check passes) ──► DB INSERT (loses: constraint violation) → 409
```

---

#### 14.1.3 State Machine Transition Conflicts

**Risk:** Two clerks simultaneously attempt to release the same approved clearance, resulting in duplicate clearance numbers being assigned or the `released` transition executing twice.

**Mitigation:** Spring Data JPA with Hibernate's default optimistic locking via `@Version` is not currently applied to `ClearanceRequest`. Instead, the state transition guard in `ClearanceService` checks the current status before transitioning. Under concurrent load without explicit locking, a double-release is possible if both requests read `APPROVED` before either commits the `RELEASED` state.

**Current status:** Acceptable trade-off for barangay-scale concurrency (single-digit concurrent staff users). A future hardening option is:

```java
// Option A: Optimistic locking (add @Version field to ClearanceRequest)
@Version
private Long version;  // Hibernate throws OptimisticLockException on conflict

// Option B: Pessimistic locking (for the release operation only)
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<ClearanceRequest> findById(UUID id);
```

---

### 14.2 Thread Model

Spring Boot uses an embedded Tomcat with a default thread pool:

| Setting            | Default | Notes                              |
| ------------------ | ------- | ---------------------------------- |
| Max threads        | 200     | `server.tomcat.threads.max`        |
| Min spare threads  | 10      | `server.tomcat.threads.min-spare`  |
| Connection timeout | 60s     | `server.tomcat.connection-timeout` |

At barangay scale (expected < 50 concurrent users), the default pool is more than sufficient. HikariCP manages the PostgreSQL connection pool independently:

| Setting            | Default | Notes                                         |
| ------------------ | ------- | --------------------------------------------- |
| Max pool size      | 10      | `spring.datasource.hikari.maximum-pool-size`  |
| Min idle           | 10      | `spring.datasource.hikari.minimum-idle`       |
| Connection timeout | 30s     | `spring.datasource.hikari.connection-timeout` |

---

### 14.3 Idempotency as a Concurrency Control

The payment idempotency key doubles as a concurrency control mechanism. The 24-hour TTL window means:

- Within 24 hours: duplicate requests with the same key are deduplicated (returns cached result)
- After 24 hours: the key can be reused for a new payment attempt (e.g., if the clearance was not released and payment needs to be retried for a different clearance)

This design eliminates the need for application-level distributed locks for the payment flow.

---

## 15. Transaction Management

### 15.1 Transaction Boundaries

Transactions are scoped at the **service layer**. Controllers never open transactions directly. Repositories participate in existing transactions.

| Annotation                                   | Used For                                                                        |
| -------------------------------------------- | ------------------------------------------------------------------------------- |
| `@Transactional`                             | Write operations (insert, update, delete)                                       |
| `@Transactional(readOnly = true)`            | Read-only queries — enables Hibernate optimizations, prevents accidental writes |
| `@Transactional(propagation = REQUIRES_NEW)` | Clearance number generation — must commit independently                         |

---

### 15.2 Transaction Propagation Map

```
HTTP Request
│
└── ClearanceService.release(id, staffUserId)          @Transactional
    │   ← outer transaction opens here
    │
    ├── clearanceRequestRepository.findById(id)         participates in outer tx
    │   [validate state = APPROVED, paymentStatus = PAID]
    │
    ├── ClearanceNumberService.next()                   @Transactional(REQUIRES_NEW)
    │   │   ← SUSPENDS outer tx; opens new inner tx
    │   ├── INSERT … ON CONFLICT … RETURNING last_seq
    │   └── ← inner tx COMMITS here (sequence row is permanent)
    │   ← outer tx RESUMES
    │
    ├── clearanceRequest.setStatus(RELEASED)
    ├── clearanceRequest.setClearanceNumber(number)
    ├── clearanceRequest.setIssuedAt(now)
    └── clearanceRequestRepository.save(clearanceRequest)
        └── ← outer tx COMMITS here
```

**Why `REQUIRES_NEW` matters:** If the outer transaction rolls back after the sequence number is generated (e.g., a constraint violation when saving the clearance), the sequence number is still committed. This creates a gap in clearance numbering — an intentional trade-off to avoid the complexity of sequence rollback and retry logic. Gaps are documented, expected, and non-problematic for a government document system.

---

### 15.3 Transaction Isolation

The system uses PostgreSQL's default isolation level: **Read Committed**.

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
| --------------- | ---------- | ------------------- | ------------ |
| Read Committed  | Prevented  | Possible            | Possible     |

**Implications:**

- A service method reading a `ClearanceRequest` twice within the same transaction may see different values if another transaction commits between reads (non-repeatable read). This is not a concern in the current codebase because service methods typically read an entity once and operate on it.
- The `ON CONFLICT DO UPDATE` for clearance numbers uses PostgreSQL row-level locking internally, providing stronger guarantees than the session isolation level for that specific operation.

**Future consideration:** If the clearance state machine is hardened with optimistic locking (`@Version`), Hibernate's `OptimisticLockException` will surface as a `409 Conflict` to the client, prompting a retry.

---

### 15.4 Read-Only Transaction Optimizations

Methods annotated `@Transactional(readOnly = true)` benefit from:

- **Hibernate flush mode set to `NEVER`** — no dirty checking at flush time; slightly faster for large result sets
- **PostgreSQL routing** — if a read replica is configured, Spring can route read-only transactions there (not currently configured, but the annotation makes future adoption zero-cost)
- **Connection tagging** — HikariCP can mark read-only connections, enabling the database driver to optimize accordingly

All query-only service methods (list, get-by-id, summary counts, reports) should use `readOnly = true`.

---

### 15.5 Registration Atomicity

`AuthService.register()` creates both a `User` and a linked `Resident` in a single `@Transactional` method. If either write fails (e.g., the `residents` insert violates a constraint), the entire registration rolls back — no orphan `User` rows are left in the database.

```
AuthService.register()  @Transactional
├── userRepository.save(user)        ← write 1
└── residentService.createFromRegistration(request, user)
    └── residentRepository.save(resident)  ← write 2
    (if this throws, write 1 is also rolled back)
```

---

## 16. Performance Considerations

### 16.1 Query Performance

#### Index Coverage

All high-frequency query patterns are covered by indexes:

| Query Pattern               | Index Used                                   | Notes                     |
| --------------------------- | -------------------------------------------- | ------------------------- |
| Login by email              | `idx_users_email` (B-tree unique)            | Single lookup per login   |
| Resident search by name     | `idx_residents_name` (functional: `lower()`) | Case-insensitive search   |
| Filter clearances by status | `idx_cr_status` (B-tree)                     | Dashboard / list views    |
| Report filter by issued_at  | `idx_cr_issued_at` (B-tree)                  | Date range report queries |
| Payment idempotency check   | `idx_payments_idempotency` (unique B-tree)   | Composite: key + userId   |
| Refresh token lookup        | `token_hash` column (unique)                 | Per-refresh-request       |

#### Avoiding N+1 Queries

**Resident name enrichment** in clearance list responses is the primary N+1 risk. The `ClearanceService` enriches `ClearanceRequestDTO` with `residentName` by calling `ResidentService.getById(residentId)` per clearance record.

**Current behavior:** Acceptable at barangay scale where list results are typically paginated to 20 records. Each page incurs at most 20 additional resident lookups.

**Future optimization (if needed):**

```java
// Batch resident lookups in a single query:
List<UUID> residentIds = clearances.stream()
    .map(ClearanceRequest::getResidentId)
    .distinct()
    .toList();
Map<UUID, Resident> residents = residentRepository
    .findAllById(residentIds)
    .stream()
    .collect(toMap(Resident::getId, identity()));
```

---

### 16.2 Connection Pool Sizing

**Default HikariCP configuration** (10 connections) is appropriate for expected load. The formula for optimal pool size at low concurrency:

```
pool size = (core count × 2) + effective_spindle_count
```

For a single-core VM with SSD storage (spindle count ≈ 1):

```
pool size = (1 × 2) + 1 = 3  (minimum)
```

The default of 10 provides headroom for burst traffic. Avoid over-provisioning — PostgreSQL allocates ~5–10 MB of shared memory per connection.

**Recommended production settings:**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10 # tune based on actual load
      minimum-idle: 5
      idle-timeout: 600000 # 10 minutes
      connection-timeout: 30000 # 30 seconds (fail fast)
      max-lifetime: 1800000 # 30 minutes (recycle connections)
```

---

### 16.3 PDF Generation Performance

PDF generation via PDFBox is CPU-bound and synchronous. Key characteristics:

| Factor             | Detail                                                    |
| ------------------ | --------------------------------------------------------- |
| Execution model    | Synchronous on the request thread                         |
| Expected duration  | < 500ms for a single-page clearance                       |
| Memory allocation  | ~2–5 MB per PDF generation (PDFBox document object graph) |
| Concurrency impact | Blocks one Tomcat thread per PDF request                  |

**At barangay scale:** PDF downloads are infrequent (one per released clearance). No optimization needed for the MVP.

**If PDF load increases significantly:**

- Move PDF generation to a background job (store result in object storage or DB)
- Return a `202 Accepted` with a job ID; client polls for completion

---

### 16.4 Pagination Enforcement

All list endpoints are paginated via `Pageable`. There is no "get all" endpoint. This prevents accidental full-table scans on large datasets.

Default page size is 20; clients can request up to the framework's configured maximum (Spring Data JPA default: no hard cap — consider adding `@PageableDefault(max = 100)` on controllers to prevent excessively large pages).

---

### 16.5 JWT Validation Cost

Access token validation in `JwtAuthFilter` is:

- **CPU-only** — HMAC-SHA256 signature verification is fast (microseconds)
- **No I/O** — no database lookup per request
- **No caching needed** — tokens are short-lived (15 min); no revocation check on access tokens

This means authentication adds negligible latency to every request.

---

### 16.6 Flyway Migration Performance

Flyway runs migrations on **every application startup** before the application becomes ready. For the current 8-migration history this is imperceptible (< 100ms). As the migration count grows:

- Flyway skips already-applied migrations (checks `flyway_schema_history`)
- Only pending migrations are executed
- In production, baseline the schema at a specific version if startup time becomes a concern (`flyway.baseline-on-migrate`)

---

### 16.7 Performance Monitoring Hooks

Spring Boot Actuator with Micrometer is included in the dependency tree. Key metrics available at `/actuator/metrics`:

| Metric                | Key                           |
| --------------------- | ----------------------------- |
| HTTP request duration | `http.server.requests`        |
| HikariCP pool usage   | `hikaricp.connections.active` |
| JVM heap              | `jvm.memory.used`             |
| JVM GC pause          | `jvm.gc.pause`                |

For production observability, expose the Prometheus endpoint (`/actuator/prometheus`) and scrape with a Prometheus + Grafana stack. No code changes required — only configuration:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
  endpoint:
    prometheus:
      enabled: true
```
