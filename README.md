# Barangay Clearance System

A modular monolith web application for digitizing barangay (Filipino local government unit) clearance issuance. Residents submit requests via a self-service portal; barangay staff manage, approve, and release clearances via a back-office dashboard; the system generates signed PDF clearances.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Backend](#backend)
- [Frontend](#frontend)
- [Roles & Access Control](#roles--access-control)
- [Clearance Workflow](#clearance-workflow)
- [API Documentation](#api-documentation)
- [Database](#database)
- [Development Notes](#development-notes)
- [Project Status](#project-status)

---

## Overview

| Actor    | Interface   | Capabilities                                                       |
| -------- | ----------- | ------------------------------------------------------------------ |
| Resident | Portal      | Register, submit clearance requests, track status, download PDF    |
| Clerk    | Back-office | Process incoming requests, record payments                         |
| Approver | Back-office | Approve or reject clearance requests                               |
| Admin    | Back-office | Manage staff accounts, barangay settings, fee config, view reports |

---

## Tech Stack

### Backend

| Technology        | Version | Purpose                              |
| ----------------- | ------- | ------------------------------------ |
| Java              | 21      | Runtime                              |
| Spring Boot       | 3.3.4   | Application framework                |
| PostgreSQL        | 16      | Primary database                     |
| Flyway            | —       | Database migrations                  |
| JPA / Hibernate   | —       | ORM (`ddl-auto: validate`)           |
| JJWT              | 0.12.6  | JWT access & refresh tokens          |
| PDFBox            | 3.0.3   | Server-side clearance PDF generation |
| MapStruct         | 1.5.5   | DTO ↔ entity mapping                 |
| Lombok            | 1.18.34 | Boilerplate reduction                |
| SpringDoc OpenAPI | 2.6.0   | Swagger UI                           |

### Frontend

| Technology            | Version | Purpose                        |
| --------------------- | ------- | ------------------------------ |
| Next.js               | 14      | React framework (App Router)   |
| TypeScript            | 5       | Type safety                    |
| Tailwind CSS          | 3.4     | Utility-first styling          |
| shadcn/ui             | —       | Component library              |
| TanStack React Query  | 5       | Server-state management        |
| React Hook Form + Zod | 7 / 4   | Form management and validation |
| Axios                 | 1       | HTTP client with interceptors  |

### Infrastructure

- **Docker Compose** — local dev (PostgreSQL only) and full production stack
- **Nginx** — reverse proxy in production

---

## Project Structure

```
barangay-clearance/
├── backend/                        # Spring Boot application
│   ├── pom.xml
│   └── src/main/java/com/barangay/clearance/
│       ├── identity/               # Auth, JWT, users, refresh tokens
│       ├── residents/              # Resident registry CRUD + portal activation
│       ├── clearance/              # State machine, number sequence, requests
│       ├── payments/               # PaymentGateway interface + StubPaymentGateway
│       ├── pdf/                    # PDFBox clearance PDF generation
│       ├── settings/               # Barangay settings + fee config (singleton tables)
│       ├── reports/                # Filtered clearance reports
│       └── shared/
│           ├── exception/          # AppException, GlobalExceptionHandler
│           ├── security/           # JwtAuthFilter, SecurityConfig, UserPrincipal
│           └── util/               # PageResponse<T>, SpecificationBuilder<T>
├── frontend/                       # Next.js application
│   └── src/
│       ├── app/
│       │   ├── login/ register/    # Public auth pages
│       │   ├── portal/             # Resident dashboard & requests
│       │   └── backoffice/         # Staff clearances, residents, reports, admin
│       ├── components/             # Shared UI components
│       ├── context/AuthContext.tsx # Auth state + useAuth() hook
│       ├── hooks/                  # TanStack React Query hooks
│       ├── lib/api.ts              # Axios instance (auto-attach token, 401→refresh)
│       ├── types/                  # TypeScript domain types
│       └── middleware.ts           # Route guards (role-based access)
├── docs/                           # Architecture, PRD, phase plans
├── nginx/                          # Nginx reverse proxy config
├── docker-compose.dev.yml          # Local dev: PostgreSQL only
└── docker-compose.yml              # Full production stack
```

---

## Prerequisites

- **Java 21+**
- **Node.js 20+** and **npm**
- **Docker & Docker Compose**
- **Maven** (or use the included `./mvnw` wrapper)

---

## Getting Started

### 1. Start the database

```bash
docker compose -f docker-compose.dev.yml up -d
```

This starts PostgreSQL 16 on port **5433** with:

- Database: `barangay_clearance`
- User: `barangay`
- Password: `barangay_dev`

### 2. Start the backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway runs automatically on startup, applying all migrations.

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

### Running URLs

| Service      | URL                                   |
| ------------ | ------------------------------------- |
| Frontend     | http://localhost:3000                 |
| Backend API  | http://localhost:8080                 |
| Swagger UI   | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |

---

## Backend

### Available Profiles

| Profile | Description                                                   |
| ------- | ------------------------------------------------------------- |
| `local` | Local dev — connects to Docker PostgreSQL on port 5433        |
| `test`  | In-memory H2 for integration tests                            |
| `prod`  | Production — requires env vars for DB credentials, JWT secret |

### Common Commands

```bash
# Run all tests
cd backend && ./mvnw test

# Run a single test class
cd backend && ./mvnw test -Dtest=ClearanceServiceTest

# Build without running tests
cd backend && ./mvnw clean install -DskipTests

# Regenerate MapStruct mappers + JPA metamodel (required after editing entities or mappers)
cd backend && ./mvnw clean compile
```

### Module Conventions

- Each module is self-contained: `controller/`, `service/`, `service/mapper/`, `repository/`, `entity/`, `dto/`
- No cross-module JPA relationships — cross-module data is accessed via service calls only
- All business errors use `AppException(HttpStatus, message)` — never throw raw exceptions from controllers
- Paginated endpoints always return `PageResponse.of(page)`, never Spring's `Page<T>` directly
- Write operations use `@Transactional` on service implementations

### Security

- **Stateless JWT** — no server-side session; `userId` and `role` embedded in token claims
- Access tokens: short-lived; refresh tokens: opaque UUIDs, only SHA-256 hash stored in DB
- `JwtAuthFilter` processes every request — no DB hit per request
- Public endpoints: `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`

---

## Frontend

### Common Commands

```bash
cd frontend

npm run dev      # Start dev server (http://localhost:3000)
npm run build    # Production build
npm run start    # Serve production build
npm run lint     # Run ESLint
```

### Key Patterns

- **Auth state** — `useAuth()` from `context/AuthContext.tsx`; tokens stored in `localStorage`; JWT decoded client-side via `jwt-decode`
- **API calls** — always use the `lib/api.ts` Axios instance; it auto-attaches the Bearer token and handles `401 → refresh → retry` with request queuing
- **Route guards** — `middleware.ts` enforces role-based access; portal routes require `RESIDENT`, backoffice routes require staff roles
- **Data fetching** — TanStack React Query hooks live in `src/hooks/`
- **Forms** — React Hook Form + Zod; the Zod schema is the single source of validation truth

---

## Roles & Access Control

| Role       | Access                                                |
| ---------- | ----------------------------------------------------- |
| `ADMIN`    | Full back-office access, user management, settings    |
| `APPROVER` | Approve/reject clearance requests                     |
| `CLERK`    | Process requests, record payments, release clearances |
| `RESIDENT` | Resident portal only — submit and track own requests  |

The default admin credentials are documented in [docs/ADMIN_SETUP.md](docs/ADMIN_SETUP.md). The admin must change the temporary password on first login.

---

## Clearance Workflow

```
DRAFT ──► FOR_APPROVAL ──► APPROVED ──► [PAID] ──► RELEASED
                       └──► REJECTED ──► DRAFT (resident resubmits)
```

- Payment status is tracked separately: `UNPAID → PAID → WAIVED`
- **Release requires** both `APPROVED` status and `PAID` (or `WAIVED`) payment status
- Invalid state transitions are rejected with HTTP 400
- Clearance numbers follow the format `YYYY-MMNNNN` (e.g., `2026-030001`), generated atomically via PostgreSQL to prevent duplicates under concurrent load

---

## API Documentation

Interactive Swagger UI is available at **http://localhost:8080/swagger-ui.html** when the backend is running.

A quick reference is available at [backend/docs/API_QUICK_REFERENCE.md](backend/docs/API_QUICK_REFERENCE.md).

All protected endpoints require an `Authorization: Bearer <access_token>` header.

---

## Database

Migrations are managed by Flyway and live in `backend/src/main/resources/db/migration/`:

| Migration                | Description                                                   |
| ------------------------ | ------------------------------------------------------------- |
| `V1__initial_schema.sql` | All 9 tables with UUID PKs, indexes                           |
| `V2__seed_settings.sql`  | Singleton rows for `barangay_settings` and `fee_config`       |
| `V3__seed_admin.sql`     | Initial admin user (BCrypt hash, `must_change_password=true`) |
| `V4__...`                | Expanded user status enum                                     |

Hibernate is set to `ddl-auto: validate` — it validates the schema but never creates or alters tables. All schema changes must go through a Flyway migration file.

---

## Development Notes

- **Lombok must process before MapStruct.** This is enforced via `lombok-mapstruct-binding` in `pom.xml`. After any entity or mapper change, run `./mvnw clean compile`.
- **Singleton tables** (`barangay_settings`, `fee_config`) enforce `CHECK (id = 1)`. Always use `ON CONFLICT (id) DO UPDATE` when writing; always fetch by `id = 1`.
- **Clearance number generation** uses an atomic `ON CONFLICT DO UPDATE RETURNING` on `clearance_number_sequence` — never use `@GeneratedValue` or application-side counters for this.

---

## Project Status

See [docs/plans/barangay-clearance/project_status.md](docs/plans/barangay-clearance/project_status.md) for the full phase-by-phase progress tracker.

**Current phase:** Phase 9 — Testing & QA (10/13 phases complete)

| Phase | Name                         | Status         |
| ----- | ---------------------------- | -------------- |
| 0     | Scaffolding & Infrastructure | ✅ Complete    |
| 1     | Identity Module: Auth & JWT  | ✅ Complete    |
| 2     | Residents Module             | ✅ Complete    |
| 3     | Clearance Module             | ✅ Complete    |
| 4     | Payments Module              | ✅ Complete    |
| 5     | PDF Generation               | ✅ Complete    |
| 6     | Settings Module              | ✅ Complete    |
| 7     | Reports Module               | ✅ Complete    |
| 8     | Frontend Polish & Navigation | ✅ Complete    |
| 11    | User Management              | ✅ Complete    |
| 9     | Testing & QA                 | 🔴 Not Started |
| 12    | Audit Logging                | 🔴 Not Started |
| 10    | Deployment                   | 🔴 Not Started |
