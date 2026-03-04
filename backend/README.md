# Barangay Clearance System — Backend

Spring Boot REST API for the Barangay Clearance System. Handles authentication, resident management, clearance request workflow, payments, PDF generation, and reporting.

## Tech Stack

| Technology                  | Version                  | Purpose                   |
| --------------------------- | ------------------------ | ------------------------- |
| Java                        | 21                       | Runtime                   |
| Spring Boot                 | 3.3.4                    | Application framework     |
| PostgreSQL                  | 16                       | Primary database          |
| Flyway                      | (managed by Spring Boot) | Database migrations       |
| Spring Data JPA / Hibernate | (managed by Spring Boot) | ORM                       |
| Spring Security             | (managed by Spring Boot) | Auth & authorization      |
| JJWT                        | 0.12.6                   | JWT issuance & validation |
| Apache PDFBox               | 3.0.3                    | PDF generation            |
| MapStruct                   | 1.5.5.Final              | Bean mapping              |
| Lombok                      | 1.18.34                  | Boilerplate reduction     |
| SpringDoc OpenAPI           | 2.6.0                    | API docs / Swagger UI     |
| Testcontainers              | 1.20.1                   | Integration testing       |

---

## Prerequisites

- **Java 21** — `java -version`
- **Maven** — provided via `./mvnw` wrapper (no separate install needed)
- **Docker & Docker Compose** — required to run PostgreSQL locally

---

## Getting Started

### 1. Start the database

```bash
docker compose -f ../docker-compose.dev.yml up -d
```

This starts a PostgreSQL 16 container on **port 5433** with:

- Database: `barangay_clearance`
- Username: `barangay`
- Password: `barangay_dev`

### 2. Run the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The API starts on **http://localhost:8080**.

Flyway runs automatically on startup and applies all pending migrations under `src/main/resources/db/migration/`.

### 3. Explore the API

Open Swagger UI: **http://localhost:8080/swagger-ui.html**

OpenAPI JSON spec: **http://localhost:8080/v3/api-docs**

---

## Available Profiles

| Profile   | Description                                           | Config file               |
| --------- | ----------------------------------------------------- | ------------------------- |
| `local`   | Local development with verbose SQL logging            | `application-local.yml`   |
| `no-auth` | Disables JWT — all requests permitted without a token | `application-no-auth.yml` |
| `prod`    | Production — requires environment variables           | `application-prod.yml`    |
| `test`    | Integration tests with Testcontainers                 | `application-test.yml`    |

### `no-auth` profile

Activating the `no-auth` profile swaps in `LocalSecurityConfig` (all requests permitted, CSRF disabled, stateless) and disables `JwtAuthFilter` entirely. This is useful when developing or testing the frontend without needing to obtain or refresh tokens.

It also enables `DEBUG`-level logging for `com.barangay.clearance` and Spring Security.

```bash
# No auth, local DB
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,no-auth

# No auth only (uses default application.yml datasource settings)
./mvnw spring-boot:run -Dspring-boot.run.profiles=no-auth
```

> **Warning:** Never run `no-auth` in production — it bypasses all authentication and authorization.

To run with a specific profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=<profile>
```

---

## Common Commands

```bash
# ────────── Building ──────────────
# Build the JAR (skip tests)
./mvnw clean install -DskipTests

# Build the JAR with tests
./mvnw clean package

# Regenerate MapStruct mappers and JPA metamodel (required after entity/mapper changes)
./mvnw clean compile

# ────────── Unit Tests ──────────────
# Run all tests (unit + integration)
./mvnw test

# Run unit tests only (excluding integration tests)
./mvnw test -Dtest=JwtServiceTest,AuthServiceTest,ClearanceServiceTest,ClearanceNumberServiceTest,PaymentServiceTest,ClearancePdfServiceTest

# Run a single unit test class
./mvnw test -Dtest=ClearanceServiceTest

# Run a specific unit test method
./mvnw test -Dtest=ClearanceServiceTest#approve_fromForApproval_setsApprovedStatus

# Run tests with verbose output
./mvnw test -X

# Run tests and generate code coverage report
./mvnw test jacoco:report
# Coverage report: target/site/jacoco/index.html

# ────────── Integration Tests (IT) ──────────────
# Run integration tests only
./mvnw test -Dtest='*IT'

# Run a single integration test class
./mvnw test -Dtest=AuthControllerIT

# Run specific integration test method
./mvnw test -Dtest=ClearanceWorkflowIT#happyPath_registerActivateSubmitApprovePayRelease

# Run all tests with full output
./mvnw test -X
```

**Integration Tests** use Testcontainers PostgreSQL (`postgres:16-alpine`) for real database validation. Tests are prefixed with `*IT.java` convention and automatically isolated via table truncation between test classes.

**Note:** Integration tests require Docker to be running. Container startup is shared across all IT tests for efficiency.

For detailed information on the unit and integration test architecture, see [docs/AUTOMATED_TEST_PLAN.md](docs/AUTOMATED_TEST_PLAN.md).

---

## Project Structure

```
src/main/java/com/barangay/clearance/
├── BarangayClearanceApplication.java   # Entry point
│
├── identity/                # Auth, JWT, user management
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── residents/               # Resident registry CRUD
├── clearance/               # Core workflow: state machine, request processing, numbering
├── payments/                # Payment gateway abstraction (StubPaymentGateway)
├── pdf/                     # PDFBox clearance PDF generation
├── settings/                # Barangay settings & fee config (singleton tables)
├── reports/                 # Filtered clearance reports
│
└── shared/                  # Cross-cutting concerns
    ├── exception/           # ErrorResponse, AppException, GlobalExceptionHandler
    ├── security/            # JwtAuthFilter, SecurityConfig, UserPrincipal
    └── util/                # PageResponse<T>, SpecificationBuilder<T>
```

---

## Database Migrations

Flyway migrations are in `src/main/resources/db/migration/`:

| File                                 | Description                                                                 |
| ------------------------------------ | --------------------------------------------------------------------------- |
| `V1__initial_schema.sql`             | All 9 tables with UUID PKs (`gen_random_uuid()`)                            |
| `V2__seed_settings.sql`              | Singleton rows for `barangay_settings` and `fee_config`                     |
| `V3__seed_admin.sql`                 | Initial admin user (BCrypt hash, `must_change_password=true`)               |
| `V4__expand_user_status.sql`         | Expanded user status values                                                 |
| `V5__fix_admin_password.sql`         | Corrects the BCrypt hash for the seeded admin user                          |
| `V6__clearance_extra_columns.sql`    | Adds `purpose_other` and `copies` columns to `clearance_requests`           |
| `V7__fix_year_month_column_type.sql` | Alters `year_month` from `CHAR(7)` to `VARCHAR(7)` for Hibernate validation |
| `V8__payments_add_columns.sql`       | Adds `idempotency_expires_at` and `payment_method` to `payments`            |

Migrations run automatically on application startup. Do **not** modify existing migration files — create a new versioned file instead.

---

## Authentication

> For a full technical deep-dive, see [docs/Security.md](docs/Security.md).

The API uses **stateless JWT authentication**.

| Token type    | Storage                              | Expiry (default) |
| ------------- | ------------------------------------ | ---------------- |
| Access token  | Client memory / Authorization header | 15 minutes       |
| Refresh token | HttpOnly cookie or client storage    | 7 days           |

Refresh tokens are stored as SHA-256 hashes in the database. The raw token is returned to the client only once at issuance.

### Roles

| Role       | Description                                 |
| ---------- | ------------------------------------------- |
| `ADMIN`    | Full system access                          |
| `APPROVER` | Can approve/reject clearance requests       |
| `CLERK`    | Can manage residents and process requests   |
| `RESIDENT` | Can submit and track own clearance requests |

### Public endpoints (no token required)

```
POST /api/v1/auth/login
POST /api/v1/auth/register
POST /api/v1/auth/refresh
GET  /swagger-ui/**
GET  /v3/api-docs/**
GET  /actuator/health
```

---

## Clearance State Machine

```
DRAFT → FOR_APPROVAL → APPROVED → [payment required] → RELEASED
                     ↘ REJECTED → DRAFT (resident resubmits)
```

Payment status is tracked separately: `UNPAID → PAID`. A clearance can only be released when its status is `APPROVED` **and** payment status is `PAID`.

Clearance numbers follow the format `YYYY-MMNNNN` (e.g., `2025-020001`), generated atomically via a `RETURNING` query on the `clearance_number_sequence` table.

---

## PDF Generation

`ClearancePdfService` produces server-side PDFs using PDFBox. Each generated PDF includes:

- Barangay header with optional logo
- Clearance metadata (number, date, purpose)
- Resident information block
- Body paragraph (certification text)
- Barangay captain signature block

PDFs are streamed directly as `application/pdf` responses.

---

## Annotation Processor Note

**Lombok must run before MapStruct.** This is enforced via `lombok-mapstruct-binding` in the `annotationProcessorPaths` section of `pom.xml`. After any change to entities or mapper interfaces, run:

```bash
./mvnw clean compile
```

---

## Health Check

```
GET /actuator/health
```

Returns application health status. Additional endpoints (`metrics`, `prometheus`) are available when authenticated.

---

## Environment Variables (Production)

For the `prod` profile, configure the following environment variables (see `application-prod.yml`):

| Variable                | Description                        |
| ----------------------- | ---------------------------------- |
| `DB_URL`                | JDBC URL for PostgreSQL            |
| `DB_USERNAME`           | Database username                  |
| `DB_PASSWORD`           | Database password                  |
| `JWT_SECRET`            | HS256 signing secret (min 256-bit) |
| `JWT_ACCESS_EXPIRY_MS`  | Access token TTL in milliseconds   |
| `JWT_REFRESH_EXPIRY_MS` | Refresh token TTL in milliseconds  |
