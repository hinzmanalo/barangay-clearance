# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Barangay Clearance System** is a modular monolith web application for digitizing barangay (Filipino local government unit) clearance issuance. Residents submit requests via a resident portal; barangay staff manage and approve requests via a back-office dashboard; the system generates clearance PDFs.

**Tech Stack:**
- **Backend:** Spring Boot 3.3.4, Java 21, PostgreSQL, Flyway, JPA/Hibernate, JJWT 0.12.x, PDFBox 3.x, MapStruct, Lombok, SpringDoc OpenAPI
- **Frontend:** Next.js 14, TypeScript, Tailwind CSS, React Hook Form + Zod, Axios, TanStack React Query, shadcn/ui
- **Infrastructure:** Docker Compose, Nginx reverse proxy

**Status:** Planning phase. No code exists yet — begin with Phase 0. See [docs/plans/barangay-clearance/project_status.md](docs/plans/barangay-clearance/project_status.md) for current progress.

## Common Commands

### Backend (once scaffolded)
```bash
# Start PostgreSQL for local dev
docker compose -f docker-compose.dev.yml up -d

# Run backend locally
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Run all tests
cd backend && ./mvnw test

# Run a single test class
cd backend && ./mvnw test -Dtest=ClearanceServiceTest

# Build without tests
cd backend && ./mvnw clean install -DskipTests

# After modifying entities or mappers (regenerates MapStruct + JPA metamodel)
cd backend && ./mvnw clean compile
```

### Frontend (once scaffolded)
```bash
cd frontend && npm run dev     # Dev server on port 3000
cd frontend && npm run build   # Production build
cd frontend && npm run lint    # ESLint
```

### URLs (when running)
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Frontend: `http://localhost:3000`

## Architecture

### Module Structure

The backend follows a modular monolith pattern with clean boundaries. Each module owns its own controller, service, repository, entity, and DTO layers — no cross-module JPA relationships.

```
backend/src/main/java/com/barangay/clearance/
├── identity/          # Auth, JWT, user management
├── residents/         # Resident registry CRUD
├── clearance/         # Core workflow: requests, state machine, numbering
├── payments/          # Payment gateway abstraction (stub provider)
├── pdf/               # PDFBox clearance PDF generation
├── settings/          # Barangay settings + fee config (singleton tables)
├── reports/           # Filtered clearance reports
└── shared/            # Security config, exception handling, utilities
    ├── exception/     # ErrorResponse, AppException, GlobalExceptionHandler
    ├── security/      # JwtAuthFilter, SecurityConfig, UserPrincipal
    └── util/          # PageResponse<T>
```

### Clearance State Machine

The core business logic enforces a strict state machine — invalid transitions must be rejected:

```
DRAFT → FOR_APPROVAL → APPROVED → [payment] → RELEASED
                     ↘ REJECTED → DRAFT (resident resubmits)
```

Payment status is tracked separately: `UNPAID → PAID`. Release requires both `APPROVED` status and `PAID` payment status.

Clearance numbers follow the format `YYYY-MMNNNN` (e.g., `2025-020001`), generated via an atomic PostgreSQL `RETURNING` query on `clearance_number_sequence` to prevent duplicates under concurrent load.

### JWT / Security

- Stateless JWT; no session state
- Access tokens: short-lived, embed `userId` + `role` as claims
- Refresh tokens: opaque UUIDs stored as SHA-256 hashes in DB (raw token returned to client only)
- `JwtAuthFilter` extracts `Authorization: Bearer` header; no DB hit per request
- Roles: `ADMIN`, `CLERK`, `APPROVER`, `RESIDENT`
- Public endpoints: `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`

### Database

PostgreSQL with Flyway migrations in `backend/src/main/resources/db/migration/`:
- `V1__initial_schema.sql` — all 9 tables with UUID PKs (`DEFAULT gen_random_uuid()`)
- `V2__seed_settings.sql` — singleton rows for `barangay_settings` and `fee_config`
- `V3__seed_admin.sql` — initial admin user (BCrypt, `must_change_password=true`)

Key constraints: `barangay_settings` and `fee_config` use `CHECK (id = 1)` to enforce singleton rows. `audit_logs` is append-only. `payments` has a composite unique index on `(idempotency_key, initiated_by_user_id)`.

### Frontend Structure

```
frontend/src/
├── app/
│   ├── login/ register/      # Public auth pages
│   ├── portal/               # Resident: dashboard, my requests
│   └── backoffice/           # Staff: clearances, residents, admin, reports
├── components/               # Shared UI components (StatusBadge, PageHeader, etc.)
├── lib/api.ts                # Axios instance with 401→refresh→retry interceptor
├── context/AuthContext.tsx   # Auth state + useAuth() hook
├── hooks/                    # Custom React Query hooks
├── types/                    # TypeScript domain types
└── middleware.ts             # Route guards (jwt-decode, role-based redirects)
```

### Payments

`PaymentGateway` is a Java interface. `StubPaymentGateway` is the initial implementation (simulates success). Designed for future plug-in of PayMongo/Maya. Payment idempotency is enforced via a unique `idempotency_key` per request — callers must supply one.

### PDF Generation

Server-side via PDFBox 3.x. `ClearancePdfService` fetches barangay settings (name, captain, logo bytes) and resident/clearance data, then renders: header with logo, title, metadata block, body paragraph, and signature block. Endpoints stream the PDF as `application/pdf`.

### Annotation Processor Order

Lombok must run before MapStruct. Enforced via `lombok-mapstruct-binding` in `annotationProcessorPaths`. After any entity or mapper change, run `./mvnw clean compile`.

## Implementation Phases

See [docs/plans/barangay-clearance/](docs/plans/barangay-clearance/) for detailed per-phase specs.

**Critical path:** Phase 0 (Scaffolding) → Phase 1 (Auth) → Phase 2 (Residents) → Phase 3 (Clearance) → [Phases 4/5/6 parallel: Payments/PDF/Settings] → [Phases 7/8 parallel: Reports/Frontend Polish] → Phase 9 (Testing) → Phase 10 (Deployment)

Each phase file contains: goal, deliverables list, key implementation notes, and a definition-of-done checklist. Always consult the relevant phase file before implementing.
