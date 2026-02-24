# Barangay Clearance System — Implementation Plan

**Version:** 1.0.0
**Date:** 2026-02-24
**Status:** Ready for Development
**Audience:** Solo Developer

---

## Table of Contents

1. [Recommended Folder Structure](#1-recommended-folder-structure)
2. [Key Dependencies](#2-key-dependencies)
3. [Environment Variable Template](#3-environment-variable-template)
4. [Phase 0 — Scaffolding & Infrastructure (Week 1)](#phase-0--project-scaffolding--infrastructure-week-1)
5. [Phase 1 — Identity Module: Auth & JWT (Week 2)](#phase-1--identity-module--auth--jwt-week-2)
6. [Phase 2 — Residents Module (Week 2–3)](#phase-2--residents-module-week-23)
7. [Phase 3 — Clearance Module (Week 3–4)](#phase-3--clearance-module-week-34)
8. [Phase 4 — Payments Module (Week 4)](#phase-4--payments-module-week-4)
9. [Phase 5 — PDF Generation (Week 5)](#phase-5--pdf-generation-week-5)
10. [Phase 6 — Settings Module (Week 5)](#phase-6--settings-module-week-5)
11. [Phase 7 — Reports Module (Week 6)](#phase-7--reports-module-week-6)
12. [Phase 8 — Frontend Polish & Role-Based Navigation (Week 6)](#phase-8--frontend-polish--role-based-navigation-week-6)
13. [Phase 9 — Testing & QA (Week 7)](#phase-9--testing--qa-week-7)
14. [Phase 10 — Deployment (Week 7–8)](#phase-10--deployment-week-78)
15. [Cross-Phase Reference: API Endpoint Inventory](#15-cross-phase-reference-api-endpoint-inventory)

---

## 1. Recommended Folder Structure

### 1.1 Repository Root

```
barangay-clearance/
├── backend/                    # Spring Boot 3.3.x Maven project
├── frontend/                   # Next.js 14 project
├── nginx/
│   └── nginx.conf              # Nginx reverse proxy config
├── docker-compose.yml          # Production full-stack compose
├── docker-compose.dev.yml      # Dev: PostgreSQL only
├── .env.example                # Environment variable template
└── docs/
    ├── PRD_Barangay_Clearance_System.md
    └── IMPLEMENTATION_PLAN.md  (this file)
```

### 1.2 Backend Folder Structure

```
backend/
├── pom.xml
├── mvnw / mvnw.cmd
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/com/barangay/clearance/
│   │   │   ├── BaragayClearanceApplication.java
│   │   │   │
│   │   │   ├── identity/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   └── UserController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   ├── TokenResponse.java
│   │   │   │   │   ├── RefreshRequest.java
│   │   │   │   │   └── UserDTO.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── User.java
│   │   │   │   │   └── RefreshToken.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   └── RefreshTokenRepository.java
│   │   │   │   └── service/
│   │   │   │       ├── AuthService.java
│   │   │   │       ├── UserService.java
│   │   │   │       └── JwtService.java
│   │   │   │
│   │   │   ├── residents/
│   │   │   │   ├── controller/
│   │   │   │   │   └── ResidentController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ResidentDTO.java
│   │   │   │   │   └── ResidentSearchRequest.java
│   │   │   │   ├── entity/
│   │   │   │   │   └── Resident.java
│   │   │   │   ├── repository/
│   │   │   │   │   └── ResidentRepository.java
│   │   │   │   └── service/
│   │   │   │       └── ResidentService.java
│   │   │   │
│   │   │   ├── clearance/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── ClearanceController.java          # backoffice
│   │   │   │   │   └── PortalClearanceController.java    # /me/clearances
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ClearanceRequestDTO.java
│   │   │   │   │   ├── CreateClearanceRequest.java
│   │   │   │   │   ├── ApproveRequest.java               # (empty body)
│   │   │   │   │   └── RejectRequest.java                # { reason }
│   │   │   │   ├── entity/
│   │   │   │   │   ├── ClearanceRequest.java
│   │   │   │   │   └── ClearanceNumberSequence.java
│   │   │   │   ├── enums/
│   │   │   │   │   ├── ClearanceStatus.java
│   │   │   │   │   ├── ClearancePaymentStatus.java
│   │   │   │   │   ├── Purpose.java
│   │   │   │   │   └── Urgency.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── ClearanceRequestRepository.java
│   │   │   │   │   └── ClearanceNumberSequenceRepository.java
│   │   │   │   └── service/
│   │   │   │       ├── ClearanceService.java
│   │   │   │       └── ClearanceNumberService.java
│   │   │   │
│   │   │   ├── payments/
│   │   │   │   ├── controller/
│   │   │   │   │   └── PaymentController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── PaymentDTO.java
│   │   │   │   │   └── InitiatePaymentRequest.java       # empty body; key is in header
│   │   │   │   ├── entity/
│   │   │   │   │   └── Payment.java
│   │   │   │   ├── enums/
│   │   │   │   │   ├── PaymentMethod.java
│   │   │   │   │   ├── PaymentStatus.java
│   │   │   │   │   └── PaymentProvider.java
│   │   │   │   ├── gateway/
│   │   │   │   │   ├── PaymentGateway.java               # interface
│   │   │   │   │   ├── PaymentRequest.java               # record
│   │   │   │   │   ├── PaymentResult.java                # record
│   │   │   │   │   └── StubPaymentGateway.java
│   │   │   │   ├── repository/
│   │   │   │   │   └── PaymentRepository.java
│   │   │   │   └── service/
│   │   │   │       └── PaymentService.java
│   │   │   │
│   │   │   ├── audit/
│   │   │   │   ├── entity/
│   │   │   │   │   └── AuditLog.java
│   │   │   │   ├── enums/
│   │   │   │   │   └── AuditEventType.java
│   │   │   │   ├── repository/
│   │   │   │   │   └── AuditLogRepository.java
│   │   │   │   └── service/
│   │   │   │       └── AuditService.java
│   │   │   │
│   │   │   ├── settings/
│   │   │   │   ├── controller/
│   │   │   │   │   └── SettingsController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── BarangaySettingsDTO.java
│   │   │   │   │   └── FeeConfigDTO.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── BarangaySettings.java
│   │   │   │   │   └── FeeConfig.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── BarangaySettingsRepository.java
│   │   │   │   │   └── FeeConfigRepository.java
│   │   │   │   └── service/
│   │   │   │       └── SettingsService.java
│   │   │   │
│   │   │   ├── reports/
│   │   │   │   ├── controller/
│   │   │   │   │   └── ReportsController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ReportFilterRequest.java
│   │   │   │   │   └── ReportRowDTO.java
│   │   │   │   └── service/
│   │   │   │       └── ReportsService.java
│   │   │   │
│   │   │   ├── pdf/
│   │   │   │   └── service/
│   │   │   │       ├── ClearancePdfService.java          # interface
│   │   │   │       └── ClearancePdfServiceImpl.java      # PDFBox implementation
│   │   │   │
│   │   │   └── shared/
│   │   │       ├── exception/
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── AppException.java
│   │   │       │   ├── NotFoundException.java
│   │   │       │   ├── BadRequestException.java
│   │   │       │   ├── ConflictException.java
│   │   │       │   ├── ForbiddenException.java
│   │   │       │   └── ErrorResponse.java
│   │   │       ├── security/
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── JwtAuthFilter.java
│   │   │       │   └── UserPrincipal.java
│   │   │       └── util/
│   │   │           ├── PageResponse.java
│   │   │           └── Constants.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       ├── application-test.yml
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__initial_schema.sql
│   │               ├── V2__seed_settings.sql
│   │               └── V3__seed_admin.sql
│   │
│   └── test/
│       └── java/com/barangay/clearance/
│           ├── BaseIntegrationTest.java
│           ├── identity/
│           │   ├── AuthServiceTest.java
│           │   └── AuthControllerIT.java
│           ├── clearance/
│           │   ├── ClearanceServiceTest.java
│           │   ├── ClearanceNumberServiceTest.java
│           │   └── ClearanceControllerIT.java
│           ├── payments/
│           │   └── PaymentServiceTest.java
│           ├── residents/
│           │   └── ResidentControllerIT.java
│           └── pdf/
│               └── ClearancePdfServiceTest.java
```

### 1.3 Frontend Folder Structure

```
frontend/
├── package.json
├── next.config.ts
├── tailwind.config.ts
├── tsconfig.json
├── Dockerfile
├── public/
│   └── favicon.ico
└── src/
    ├── app/
    │   ├── layout.tsx                              # Root layout
    │   ├── page.tsx                                # Root redirect (/ → role home)
    │   ├── login/
    │   │   └── page.tsx
    │   ├── register/
    │   │   └── page.tsx
    │   ├── portal/
    │   │   ├── layout.tsx                          # Portal layout (RESIDENT guard)
    │   │   ├── dashboard/
    │   │   │   └── page.tsx                        # My Requests list
    │   │   └── requests/
    │   │       ├── new/
    │   │       │   └── page.tsx
    │   │       └── [id]/
    │   │           └── page.tsx
    │   └── backoffice/
    │       ├── layout.tsx                          # Backoffice layout (CLERK|APPROVER|ADMIN guard)
    │       ├── dashboard/
    │       │   └── page.tsx
    │       ├── residents/
    │       │   ├── page.tsx
    │       │   ├── new/
    │       │   │   └── page.tsx
    │       │   └── [id]/
    │       │       └── page.tsx
    │       ├── clearances/
    │       │   ├── page.tsx
    │       │   ├── new/
    │       │   │   └── page.tsx
    │       │   └── [id]/
    │       │       └── page.tsx
    │       ├── reports/
    │       │   └── page.tsx
    │       └── admin/
    │           ├── users/
    │           │   ├── page.tsx
    │           │   └── new/
    │           │       └── page.tsx
    │           └── settings/
    │               ├── page.tsx
    │               └── fees/
    │                   └── page.tsx
    │
    ├── components/
    │   ├── ui/                                     # shadcn/ui components (added via CLI)
    │   ├── portal/
    │   │   ├── RequestCard.tsx
    │   │   └── StatusTimeline.tsx
    │   ├── backoffice/
    │   │   ├── ClearanceTable.tsx
    │   │   ├── ResidentTable.tsx
    │   │   ├── DashboardSummaryCards.tsx
    │   │   └── ActionButtons.tsx
    │   ├── shared/
    │   │   ├── StatusBadge.tsx
    │   │   ├── PaymentBadge.tsx
    │   │   ├── LoadingSkeleton.tsx
    │   │   ├── ErrorToast.tsx
    │   │   └── PageHeader.tsx
    │   └── forms/
    │       ├── LoginForm.tsx
    │       ├── RegisterForm.tsx
    │       ├── ClearanceRequestForm.tsx
    │       └── ResidentForm.tsx
    │
    ├── lib/
    │   ├── api.ts                                  # Axios instance + interceptors
    │   ├── auth.ts                                 # Token helpers, role checks
    │   └── utils.ts                                # cn(), formatDate(), etc.
    │
    ├── hooks/
    │   ├── useAuth.ts
    │   ├── useClearances.ts
    │   └── useResidents.ts
    │
    ├── types/
    │   ├── auth.ts
    │   ├── clearance.ts
    │   ├── resident.ts
    │   ├── payment.ts
    │   └── settings.ts
    │
    ├── context/
    │   └── AuthContext.tsx
    │
    └── middleware.ts                               # Route guard: role-based redirect
```

---

## 2. Key Dependencies

### 2.1 Backend — `pom.xml` Key Dependencies

```xml
<!-- Spring Boot Parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
</parent>

<!-- Core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- JWT — JJWT 0.12.x -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- PDF Generation -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- Mapping -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>

<!-- Boilerplate -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- API Documentation -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

**Annotation processor order in `maven-compiler-plugin`:**
```xml
<annotationProcessorPaths>
    <path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></path>
    <path><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId><version>1.5.5.Final</version></path>
</annotationProcessorPaths>
```

### 2.2 Frontend — `package.json` Key Dependencies

```json
{
  "dependencies": {
    "next": "14.2.x",
    "react": "^18.3.x",
    "react-dom": "^18.3.x",
    "axios": "^1.7.x",
    "@tanstack/react-query": "^5.x",
    "react-hook-form": "^7.x",
    "zod": "^3.x",
    "@hookform/resolvers": "^3.x",
    "lucide-react": "^0.x",
    "date-fns": "^3.x",
    "clsx": "^2.x",
    "tailwind-merge": "^2.x",
    "class-variance-authority": "^0.7.x",
    "@radix-ui/react-dialog": "^1.x",
    "@radix-ui/react-select": "^2.x",
    "@radix-ui/react-label": "^2.x",
    "@radix-ui/react-toast": "^1.x",
    "@radix-ui/react-dropdown-menu": "^2.x"
  },
  "devDependencies": {
    "typescript": "^5.x",
    "@types/react": "^18.x",
    "@types/node": "^20.x",
    "tailwindcss": "^3.4.x",
    "postcss": "^8.x",
    "autoprefixer": "^10.x",
    "eslint": "^8.x",
    "eslint-config-next": "14.x"
  }
}
```

**shadcn/ui components** are added on-demand via `npx shadcn-ui@latest add <component>`, not installed as a package. Expected components: `button`, `input`, `select`, `card`, `badge`, `dialog`, `table`, `form`, `toast`, `skeleton`, `separator`, `textarea`, `label`.

---

## 3. Environment Variable Template

### `.env.example`

```bash
# =========================================
# Barangay Clearance System — Environment Variables
# Copy to .env and fill in values before running docker compose
# NEVER commit .env to version control
# =========================================

# --- PostgreSQL ---
DB_USER=barangay_user
DB_PASSWORD=change_me_strong_password_here
DB_NAME=barangay_clearance

# --- JWT ---
# Generate with: openssl rand -hex 32
JWT_SECRET=replace_with_256_bit_hex_secret

# --- Payment ---
PAYMENT_PROVIDER=stub
PAYMENT_STUB_ALWAYS_SUCCESS=true

# --- Spring Profile ---
SPRING_PROFILES_ACTIVE=prod

# --- Frontend ---
# URL seen by the browser (external)
NEXT_PUBLIC_API_URL=https://your-domain.com/api/v1
```

### `application.yml` (base config)

```yaml
spring:
  application:
    name: barangay-clearance-api
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/barangay_clearance}
    username: ${DB_USER:barangay_user}
    password: ${DB_PASSWORD:devpassword}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway owns DDL; Hibernate only validates
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false

jwt:
  secret: ${JWT_SECRET:dev-secret-change-in-production-minimum-256-bits}
  access-token-expiration-ms: 900000      # 15 minutes
  refresh-token-expiration-ms: 604800000  # 7 days

payment:
  provider: ${PAYMENT_PROVIDER:stub}
  stub:
    always-success: ${PAYMENT_STUB_ALWAYS_SUCCESS:true}

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

---

## Phase 0 — Project Scaffolding & Infrastructure (Week 1)

### Goal

Establish the skeleton that all subsequent phases build upon: an empty but runnable Spring Boot backend, an empty but runnable Next.js frontend, Docker Compose for all environments, the complete database schema via Flyway, and the shared error-handling infrastructure.

By the end of this phase, `docker compose -f docker-compose.dev.yml up -d && ./mvnw spring-boot:run` runs cleanly and Flyway migrations apply all 9 tables successfully.

---

### Deliverables

**Backend:**
- `backend/pom.xml` — all dependencies declared (see Section 2.1)
- `backend/mvnw` + `backend/.mvn/wrapper/maven-wrapper.properties` — Maven wrapper
- `backend/src/main/java/com/barangay/clearance/BarangayClearanceApplication.java`
- `backend/src/main/resources/application.yml` — base config
- `backend/src/main/resources/application-local.yml` — local dev overrides
- `backend/src/main/resources/application-prod.yml` — prod overrides (no secrets)
- `backend/src/main/resources/application-test.yml` — test overrides
- `backend/src/main/resources/db/migration/V1__initial_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_settings.sql`
- `backend/src/main/resources/db/migration/V3__seed_admin.sql`
- `backend/src/main/java/com/barangay/clearance/shared/exception/ErrorResponse.java`
- `backend/src/main/java/com/barangay/clearance/shared/exception/AppException.java`
- `backend/src/main/java/com/barangay/clearance/shared/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/barangay/clearance/shared/util/PageResponse.java`

**Frontend:**
- `frontend/` — initialized via `npx create-next-app@14 frontend --typescript --tailwind --app`
- `frontend/src/app/layout.tsx` — root layout
- `frontend/src/app/page.tsx` — root redirect placeholder
- `frontend/src/lib/api.ts` — Axios instance skeleton (no interceptors yet)
- `frontend/src/types/` — empty TypeScript type files for each domain

**Infrastructure:**
- `docker-compose.dev.yml` — PostgreSQL only (port 5432 exposed to host)
- `docker-compose.yml` — production: postgres + backend + frontend + nginx
- `nginx/nginx.conf` — HTTP→HTTPS redirect, `/api/` proxy to backend, `/` proxy to frontend
- `.env.example` — as defined in Section 3

---

### Key Implementation Notes

**Flyway V1 — `V1__initial_schema.sql`**

Create all 9 tables with their exact column definitions and indexes as specified in the PRD Section 7. Critical points:

- All PKs are `UUID DEFAULT gen_random_uuid()` — this is PostgreSQL-specific; Hibernate `ddl-auto: validate` will confirm the schema matches entity annotations.
- `clearance_number_sequence.year_month` is `CHAR(7)` (e.g., `'2025-02'`), not a date type.
- `payments` has a **composite unique index** on `(idempotency_key, initiated_by_user_id)` — not a single-column unique constraint. This enforces the per-user idempotency scope defined in ADR-011.
- `barangay_settings.id` has `CHECK (id = 1)` to enforce the single-row invariant at the database level.
- `fee_config.id` has the same single-row `CHECK (id = 1)` constraint.
- `audit_logs` has no update/delete permissions in the application — enforce this via `@PrePersist` only (no `@PreUpdate`).
- Create all indexes documented in Section 7: `idx_users_email`, `idx_users_role_status`, `idx_residents_name` (on `lower(last_name), lower(first_name)` — functional index), `idx_cr_status`, `idx_cr_issued_at`, `idx_payments_idempotency`.

**Flyway V2 — `V2__seed_settings.sql`**

```sql
INSERT INTO barangay_settings (id, barangay_name, municipality, province, captain_name, captain_position)
VALUES (1, 'Barangay Name', 'Municipality', 'Province', 'Barangay Captain', 'Punong Barangay')
ON CONFLICT (id) DO NOTHING;

INSERT INTO fee_config (id, regular_fee, express_fee)
VALUES (1, 50.00, 100.00)
ON CONFLICT (id) DO NOTHING;
```

**Flyway V3 — `V3__seed_admin.sql`**

The initial ADMIN password must be BCrypt-hashed at strength 12 before embedding in SQL. Generate it once before writing the migration:

```java
// Run once in a scratch main method or use Spring Security CLI:
// spring security encode --password adminPass@123 --algorithm bcrypt --strength 12
// Then paste the hash into the SQL:
```

```sql
INSERT INTO users (id, email, password_hash, first_name, last_name, role, status, must_change_password)
VALUES (
    gen_random_uuid(),
    'admin@barangay.gov.ph',
    '$2a$12$<generated_hash_here>',
    'System',
    'Admin',
    'ADMIN',
    'ACTIVE',
    true
) ON CONFLICT (email) DO NOTHING;
```

Document the temporary password in `docs/ADMIN_SETUP.md` (not in `.env`) and destroy after first login.

**`ErrorResponse.java`**

```java
@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private Instant timestamp;
    private String path;
    private Map<String, String> details;  // field-level validation errors
}
```

**`PageResponse<T>`**

A generic wrapper matching the JSON shape `{ content, totalElements, totalPages, page, size }`. Used by all paginated endpoints.

```java
@Data
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }
}
```

**`GlobalExceptionHandler`**

Handle at minimum:
- `MethodArgumentNotValidException` → 400, populate `details` map with field errors
- `ConstraintViolationException` → 400
- `NotFoundException extends AppException` → 404
- `BadRequestException extends AppException` → 400
- `ConflictException extends AppException` → 409
- `ForbiddenException extends AppException` → 403
- `AccessDeniedException` (Spring Security) → 403
- `AuthenticationException` (Spring Security) → 401
- `RuntimeException` (catch-all) → 500

**`application-local.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/barangay_clearance
    username: barangay_user
    password: devpassword
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    com.barangay: DEBUG
    org.hibernate.SQL: DEBUG
```

**`application-test.yml`**

```yaml
spring:
  jpa:
    show-sql: false
payment:
  stub:
    always-success: true
```

Testcontainers overrides the datasource URL via `@ServiceConnection`; no datasource URL needed in this file.

---

### Definition of Done

- `docker compose -f docker-compose.dev.yml up -d` starts PostgreSQL on port 5432 with no errors.
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` starts the backend cleanly. Flyway log shows 3 migrations applied (V1, V2, V3).
- `./mvnw test` runs with 0 test files yet — build succeeds.
- `http://localhost:8080/swagger-ui.html` opens a (nearly empty) Swagger UI.
- `npm run dev` inside `frontend/` starts Next.js on port 3000 with no errors.
- Posting to `http://localhost:8080/api/v1/nonexistent` returns the `ErrorResponse` JSON envelope with `status: 404`.

---

## Phase 1 — Identity Module: Auth & JWT (Week 2)

### Goal

Complete user authentication: self-registration for residents, login, JWT access + refresh token issuance, silent token refresh, logout, and admin-managed staff account operations. After this phase, every subsequent module has working auth to build against.

---

### Deliverables

**Entities:**
- `User.java` — JPA entity mapped to `users` table
- `RefreshToken.java` — JPA entity mapped to `refresh_tokens` table

**Repositories:**
- `UserRepository.java` — `findByEmail`, `findByRole`, `findByStatus`
- `RefreshTokenRepository.java` — `findByTokenHash`, `deleteByUserId`, `deleteByExpiresAtBefore`

**Services:**
- `JwtService.java` — generate/validate access token; hash/compare refresh token
- `AuthService.java` — register, login, refresh, logout business logic
- `UserService.java` — admin user management (list staff, create staff, deactivate)

**Security infrastructure:**
- `UserPrincipal.java` — implements `UserDetails`; holds userId and role
- `JwtAuthFilter.java` — extends `OncePerRequestFilter`
- `SecurityConfig.java` — `SecurityFilterChain` bean

**Controllers:**
- `AuthController.java` — `/api/v1/auth/**` (all public)
- `UserController.java` — `/api/v1/admin/users/**` (ADMIN only)

**DTOs:**
- `LoginRequest.java`, `RegisterRequest.java`, `TokenResponse.java`, `RefreshRequest.java`, `UserDTO.java`, `CreateStaffRequest.java`

**Frontend:**
- `frontend/src/app/login/page.tsx` — login form with React Hook Form + Zod validation
- `frontend/src/app/register/page.tsx` — registration form
- `frontend/src/lib/api.ts` — Axios instance with request interceptor (attach Bearer) and response interceptor (401 → refresh → retry)
- `frontend/src/context/AuthContext.tsx` — `AuthProvider`, `useAuth()` hook
- `frontend/src/middleware.ts` — skeleton (permit `/login`, `/register`; redirect others to login)
- `frontend/src/types/auth.ts`

---

### Key Implementation Notes

**`JwtService`**

Use JJWT 0.12.x API. The 0.12.x API differs significantly from older 0.11.x:
```java
// Key generation (HMAC-SHA256)
SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

// Build access token — embed userId and role as custom claims
String token = Jwts.builder()
    .subject(userId.toString())
    .claim("role", role.name())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
    .signWith(key)
    .compact();

// Parse — use .verifyWith(key).build().parseSignedClaims(token)
Claims claims = Jwts.parser().verifyWith(key).build()
    .parseSignedClaims(token).getPayload();
```

Refresh tokens are opaque UUIDs stored as SHA-256 hashes in the database:
```java
String rawToken = UUID.randomUUID().toString();
String hash = DigestUtils.sha256Hex(rawToken);  // spring-core utility
```

Return the raw token to the client; store only the hash. On verification, hash the incoming token and compare.

**`JwtAuthFilter`**

1. Extract `Authorization: Bearer <token>` header.
2. If absent, continue filter chain (public endpoints are handled by `SecurityConfig`).
3. Parse and validate the JWT.
4. Load `UserPrincipal` from claims (no database hit on every request — use data embedded in the JWT).
5. Set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.
6. On any `JwtException`, clear the context and let the 401 handler respond.

**`SecurityConfig`**

```java
.csrf(AbstractHttpConfigurer::disable)
.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
.exceptionHandling(e -> e
    .authenticationEntryPoint((req, res, ex) -> /* write ErrorResponse 401 */)
    .accessDeniedHandler((req, res, ex) -> /* write ErrorResponse 403 */)
)
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
    .requestMatchers("/api/v1/me/**").hasRole("RESIDENT")
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.POST, "/api/v1/clearances/*/approve").hasAnyRole("APPROVER","ADMIN")
    .requestMatchers(HttpMethod.POST, "/api/v1/clearances/*/reject").hasAnyRole("APPROVER","ADMIN")
    .anyRequest().authenticated()
)
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
```

Note: Spring Security's `hasRole("RESIDENT")` expects the authority to be `ROLE_RESIDENT`. When building `UserPrincipal`, set authority as `new SimpleGrantedAuthority("ROLE_" + user.getRole().name())`.

**`POST /auth/register` business logic:**

1. Validate email uniqueness — throw `ConflictException` if already taken.
2. Validate password: minimum 8 characters (Jakarta Validation on DTO).
3. Hash password: `passwordEncoder.encode(raw)` — BCrypt strength 12 via `@Bean BCryptPasswordEncoder(12)`.
4. Create `User` with role `RESIDENT`, status `PENDING_VERIFICATION`, `mustChangePassword = false`.
5. Create `Resident` profile from the registration fields — link `resident.userId` to the new user.
6. Audit log: `USER_REGISTERED`.
7. Return 201 with message — do NOT return a token; resident must wait for clerk activation.

**`POST /auth/login` business logic:**

1. Look up user by email.
2. Verify `passwordEncoder.matches(raw, hash)`.
3. Check status: if `PENDING_VERIFICATION` → 403 with message. If `REJECTED` or `DEACTIVATED` → 403.
4. If `mustChangePassword` → return token with extra claim `mustChangePassword: true` (frontend intercepts and forces change-password screen).
5. Generate access token (15 min) and refresh token (7 days).
6. Save `RefreshToken` entity (hashed) in database.
7. Audit log: `USER_LOGIN`.
8. Return `TokenResponse`.

**`POST /auth/refresh`:**

1. Receive `{ refreshToken }` from client.
2. Hash the incoming token; find `RefreshToken` by hash.
3. Validate: not revoked, not expired.
4. Issue new access token (do NOT rotate refresh token in MVP — rotation adds complexity).
5. Return new `accessToken` only.

**`POST /auth/logout`:**

1. Receive `{ refreshToken }` from client.
2. Hash and find; set `revoked = true`.
3. Return 200 (no body).

**Frontend token storage strategy:**

Store `accessToken` in React state (memory) via `AuthContext`. Store `refreshToken` in an `httpOnly` cookie set via the backend `Set-Cookie` header, OR in `localStorage` as a fallback (simpler for MVP). If using `localStorage`, document the XSS risk. The recommended approach for MVP is `localStorage` for both tokens given the internal-use nature of the application, with a note to migrate to `httpOnly` cookies in Phase 2.

**Axios interceptors (`api.ts`):**

```typescript
// Request: attach access token
api.interceptors.request.use(config => {
  const token = getAccessToken(); // from AuthContext or localStorage
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response: on 401, attempt refresh
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        error.config.headers.Authorization = `Bearer ${newToken}`;
        return api(error.config);
      }
      // Refresh failed → logout
      clearTokens();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

**`must_change_password` flow:**

On first login, if `mustChangePassword: true` is in the JWT claim:
- Frontend redirects to `/change-password` page.
- User sets a new password.
- Backend endpoint `PUT /api/v1/auth/change-password` validates old password (or temporary password), hashes new, sets `mustChangePassword = false`.
- New tokens issued.

---

### Definition of Done

- `POST /api/v1/auth/register` with valid body → 201; duplicate email → 409.
- `POST /api/v1/auth/login` → 200 with tokens; wrong credentials → 401; `PENDING_VERIFICATION` → 403.
- `POST /api/v1/auth/refresh` → 200 with new access token; expired refresh → 401.
- `POST /api/v1/auth/logout` → 200; subsequent refresh with same token → 401.
- `GET /api/v1/admin/users` with RESIDENT token → 403. With ADMIN token → 200.
- Frontend: logging in redirects RESIDENT to `/portal/dashboard` and ADMIN/CLERK/APPROVER to `/backoffice/dashboard`. Logging out clears tokens. Silent refresh works on token expiry.

---

## Phase 2 — Residents Module (Week 2–3)

### Goal

Enable clerk-managed resident registry with search, plus the pending-registration review workflow that unblocks residents from submitting requests.

---

### Deliverables

**Entity:**
- `Resident.java` — UUID PK, optional FK `user_id` → `users(id)`

**Repository:**
- `ResidentRepository.java` — custom JPQL search method:
  ```java
  @Query("SELECT r FROM Resident r WHERE " +
         "(:q IS NULL OR LOWER(CONCAT(r.firstName, ' ', r.lastName)) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
         "(:purok IS NULL OR LOWER(r.purokZone) LIKE LOWER(CONCAT('%', :purok, '%')))")
  Page<Resident> search(@Param("q") String q, @Param("purok") String purok, Pageable pageable);
  ```

**Service:**
- `ResidentService.java` — create, update, search, `findPendingUsers()`, `activateUser()`, `rejectUser()`

**Controller:**
- `ResidentController.java` — all `/api/v1/residents/**` and `/api/v1/residents/users/**` endpoints
  - `GET /residents?q=&purok=&page=&size=` — paginated search (CLERK, ADMIN)
  - `POST /residents` — create (CLERK, ADMIN)
  - `GET /residents/{id}` — detail (CLERK, ADMIN)
  - `PUT /residents/{id}` — update (CLERK, ADMIN)
  - `GET /residents/pending-users` — list `PENDING_VERIFICATION` users (CLERK, ADMIN)
  - `POST /residents/users/{userId}/activate` — set user status to `ACTIVE` (CLERK, ADMIN)
  - `POST /residents/users/{userId}/reject` — set user status to `REJECTED` (CLERK, ADMIN)

**DTOs:**
- `ResidentDTO.java` — includes computed `hasPortalAccount` field
- `ResidentSearchRequest.java`

**MapStruct Mapper:**
- `ResidentMapper.java` — `Resident ↔ ResidentDTO`

**Frontend:**
- `frontend/src/app/backoffice/residents/page.tsx` — list with debounced search (300ms `setTimeout`)
- `frontend/src/app/backoffice/residents/new/page.tsx` — create form
- `frontend/src/app/backoffice/residents/[id]/page.tsx` — detail + edit + pending user actions
- `frontend/src/components/backoffice/ResidentTable.tsx`
- `frontend/src/types/resident.ts`

---

### Key Implementation Notes

**Resident creation from registration flow:**

When a resident registers via `POST /auth/register`, `AuthService` calls `ResidentService.createFromRegistration(RegisterRequest, User)` to atomically create both the `User` and `Resident` in the same transaction. The `Resident` gets `userId` linked immediately. This means the clerk's pending-users list shows the registration details from the resident profile.

**Activating a resident:**

`activateUser(userId)` sets `user.status = ACTIVE`. The resident can now log in and submit requests. The `@PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")` annotation enforces access at the method level.

**Walk-in residents:**

Clerks can create a `Resident` profile with no `user_id`. This profile can later be linked to a portal account via `PUT /residents/{id}` by setting `userId`, or a walk-in clearance request can be created directly against the resident profile without any portal account.

**Search performance:**

The `idx_residents_name` index in V1 is a functional index on `lower(last_name), lower(first_name)`. The JPQL query uses `LOWER()` to match. For `purok_zone` partial match, no special index is needed at MVP scale. Add a GIN index in Phase 2 if needed.

**`hasPortalAccount` in DTO:**

This is not a database column. Compute it in the mapper:
```java
// In ResidentMapper.java
@Mapping(target = "hasPortalAccount", expression = "java(resident.getUserId() != null)")
ResidentDTO toDto(Resident resident);
```

**Audit logging:**

Audit `RESIDENT_CREATED`, `RESIDENT_UPDATED`, `USER_ACCOUNT_ACTIVATED`, `USER_ACCOUNT_REJECTED` events via `AuditService`.

---

### Definition of Done

- `POST /api/v1/residents` creates a resident; duplicate email on linked account returns 409.
- `GET /api/v1/residents?q=dela+Cruz` returns paginated results matching the name.
- `GET /api/v1/residents/pending-users` returns residents with `status = PENDING_VERIFICATION`.
- `POST /api/v1/residents/users/{userId}/activate` changes user status; user can now log in.
- Frontend: resident search filters in real time (debounced 300ms). Pending-users list shows Activate/Reject buttons. Activating a user shows success toast.

---

## Phase 3 — Clearance Module (Week 3–4)

### Goal

Implement the core state machine: submit, approve, reject, and release clearance requests, plus the atomic clearance number sequence and audit trail for all transitions.

---

### Deliverables

**Entities:**
- `ClearanceRequest.java` — all fields from PRD Section 7.4; enums for `status`, `paymentStatus`, `purpose`, `urgency`
- `ClearanceNumberSequence.java` — `year_month` (PK), `lastSeq`

**Repositories:**
- `ClearanceRequestRepository.java`
  - `findByResidentUserId(UUID userId, Pageable page)` — portal scoping
  - `findAllWithFilters(status, paymentStatus, from, to, Pageable)` — backoffice list
  - `countByStatus(ClearanceStatus status)` — dashboard cards
- `ClearanceNumberSequenceRepository.java` — used only by `ClearanceNumberService`

**Services:**
- `ClearanceNumberService.java` — atomic sequence increment
- `ClearanceService.java` — state machine, all transition methods

**Controllers:**
- `ClearanceController.java` — backoffice `/api/v1/clearances/**`
- `PortalClearanceController.java` — resident portal `/api/v1/me/clearances/**`

**DTOs:**
- `ClearanceRequestDTO.java` — full DTO (includes resident name denormalized)
- `CreateClearanceRequest.java` — `{ purpose, purposeOther, urgency, copies, notes }`
- `RejectRequest.java` — `{ reason }`

**MapStruct Mapper:**
- `ClearanceMapper.java` — `ClearanceRequest ↔ ClearanceRequestDTO`

**Frontend:**
- `frontend/src/app/portal/dashboard/page.tsx` — My Requests list (request cards)
- `frontend/src/app/portal/requests/new/page.tsx` — submit form
- `frontend/src/app/portal/requests/[id]/page.tsx` — detail + status timeline
- `frontend/src/app/backoffice/clearances/page.tsx` — list with filters
- `frontend/src/app/backoffice/clearances/new/page.tsx` — walk-in request form
- `frontend/src/app/backoffice/clearances/[id]/page.tsx` — detail + action buttons
- `frontend/src/components/portal/StatusTimeline.tsx`
- `frontend/src/components/portal/RequestCard.tsx`
- `frontend/src/components/backoffice/ClearanceTable.tsx`
- `frontend/src/components/backoffice/ActionButtons.tsx`
- `frontend/src/types/clearance.ts`

---

### Key Implementation Notes

**State machine in `ClearanceService`:**

Enforce guards via explicit checks — do not use a library. Each transition method validates the current status before mutating:

```java
public ClearanceRequestDTO approve(UUID id, UUID actorUserId) {
    ClearanceRequest cr = findOrThrow(id);
    if (cr.getStatus() != ClearanceStatus.FOR_APPROVAL) {
        throw new BadRequestException("Only FOR_APPROVAL requests can be approved.");
    }
    cr.setStatus(ClearanceStatus.APPROVED);
    cr.setApprovedAt(Instant.now());
    clearanceRepository.save(cr);
    auditService.log(AuditEventType.CLEARANCE_APPROVED, actorUserId, cr.getId(),
                     ClearanceStatus.FOR_APPROVAL, ClearanceStatus.APPROVED);
    return clearanceMapper.toDto(cr);
}

public ClearanceRequestDTO reject(UUID id, String reason, UUID actorUserId) {
    if (reason == null || reason.isBlank()) {
        throw new BadRequestException("Rejection reason is required.");
    }
    ClearanceRequest cr = findOrThrow(id);
    if (cr.getStatus() != ClearanceStatus.FOR_APPROVAL) {
        throw new BadRequestException("Only FOR_APPROVAL requests can be rejected.");
    }
    cr.setStatus(ClearanceStatus.REJECTED);
    cr.setRejectionReason(reason);
    // Note: setting to DRAFT happens when resident opens the request in the portal
    // or immediately on rejection — choose immediate for simplicity:
    cr.setStatus(ClearanceStatus.DRAFT);  // goes directly to DRAFT after rejection
    clearanceRepository.save(cr);
    auditService.log(...);
    return clearanceMapper.toDto(cr);
}
```

Wait — the PRD state diagram shows `REJECTED` as an intermediate state and then `DRAFT` separately. Implement it as two distinct statuses: rejection sets `REJECTED`, and a separate "acknowledge rejection" action by the resident (implicit when they click "Edit & Resubmit") transitions to `DRAFT`. However, for simplicity in MVP, set status to `REJECTED` and allow editing when status is `REJECTED`. The `resubmit()` method sets it back to `FOR_APPROVAL`.

**`ClearanceNumberService` — atomic sequence:**

Use a native query to avoid race conditions:
```java
@Modifying
@Query(value = """
    INSERT INTO clearance_number_sequence (year_month, last_seq)
    VALUES (:yearMonth, 1)
    ON CONFLICT (year_month)
    DO UPDATE SET last_seq = clearance_number_sequence.last_seq + 1
    RETURNING last_seq
    """, nativeQuery = true)
int incrementAndGet(@Param("yearMonth") String yearMonth);
```

This is a single atomic PostgreSQL operation. No application-level locking needed. Format the result:
```java
String clearanceNumber = String.format("%s-%04d", yearMonth, seq); // e.g., "2025-02-0001"
```

Call this only inside `release()`, wrapped in `@Transactional`. The clearance number is assigned only at the moment of release — never earlier (ADR-008).

**`release()` method guard:**

```java
public ClearanceRequestDTO release(UUID id, UUID actorUserId) {
    ClearanceRequest cr = findOrThrow(id);
    if (cr.getStatus() != ClearanceStatus.APPROVED) {
        throw new BadRequestException("Only APPROVED requests can be released.");
    }
    if (cr.getPaymentStatus() != ClearancePaymentStatus.PAID) {
        throw new BadRequestException("Payment must be confirmed before release.");
    }
    String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    int seq = clearanceNumberService.incrementAndGet(yearMonth);
    String number = String.format("%s-%04d", yearMonth, seq);
    cr.setClearanceNumber(number);
    cr.setStatus(ClearanceStatus.RELEASED);
    cr.setReleasedAt(Instant.now());
    cr.setIssuedAt(Instant.now());  // issuedAt = releasedAt in MVP
    clearanceRepository.save(cr);
    auditService.log(AuditEventType.CLEARANCE_RELEASED, actorUserId, cr.getId(), ...);
    return clearanceMapper.toDto(cr);
}
```

**Portal scoping:**

`PortalClearanceController` always resolves the `residentId` from the JWT principal — never from a request parameter. This prevents horizontal privilege escalation (resident A accessing resident B's requests):

```java
@GetMapping
public PageResponse<ClearanceRequestDTO> myRequests(
        @AuthenticationPrincipal UserPrincipal principal,
        Pageable pageable) {
    return clearanceService.findByUserId(principal.getUserId(), pageable);
}
```

**Walk-in request (backoffice):**

`POST /api/v1/clearances` creates a clearance on behalf of any resident (clerk selects from resident registry). The clerk's `userId` is stored in `created_by_user_id`. The request starts at `FOR_APPROVAL` immediately (no DRAFT step for walk-ins).

**`resubmit()` endpoint:**

`PUT /api/v1/me/clearances/{id}` — resident edits a `REJECTED` or `DRAFT` request. The method validates that the current principal owns the request (check `clearance.resident.userId == principal.userId`), then updates the editable fields and sets status to `FOR_APPROVAL`.

**Spring application events (Phase 2 prep):**

In every status transition, publish a Spring event:
```java
applicationEventPublisher.publishEvent(
    new ClearanceStatusChangedEvent(cr.getId(), previousStatus, newStatus, cr.getResident().getUserId())
);
```

No listener needed in MVP — this is a zero-cost Phase 2 preparation.

**Dashboard summary counts:**

`GET /api/v1/backoffice/dashboard/summary` (add a `DashboardController` or include in `ClearanceController`):
```java
{
    "pendingApproval": count,
    "approvedAwaitingPayment": count,
    "releasedToday": count
}
```

Use `@Query` with `COUNT(*)` grouped by status and date.

---

### Definition of Done

- `POST /api/v1/me/clearances` with ACTIVE resident → 201 with `FOR_APPROVAL`.
- `POST /api/v1/clearances/{id}/approve` with APPROVER token → 200 with `APPROVED`. With CLERK token → 403.
- `POST /api/v1/clearances/{id}/reject` without reason → 400.
- `POST /api/v1/clearances/{id}/release` when `paymentStatus = UNPAID` → 400.
- `POST /api/v1/clearances/{id}/release` when `APPROVED + PAID` → 200; `clearanceNumber` is `YYYY-MM-NNNN`; sequential for the month.
- Concurrent release of 10 requests for the same month assigns 10 unique sequential numbers (test with concurrent threads in `ClearanceNumberServiceTest`).
- Portal: resident only sees their own requests. Unauthorized access to another resident's request → 403.

---

## Phase 4 — Payments Module (Week 4)

### Goal

Implement stub payment processing with full idempotency logic (client-supplied UUID key, per-user scope, 24h TTL, PENDING→409, SUCCESS/FAILED→cached replay) and cash mark-as-paid for clerks.

---

### Deliverables

**Entity:**
- `Payment.java` — all fields from PRD Section 7.6, including `responseBody` as `@Type(JsonType.class)` JSONB or `String` (serialize/deserialize manually for simplicity)

**Repository:**
- `PaymentRepository.java`
  - `findByIdempotencyKeyAndInitiatedByUserIdAndIdempotencyExpiresAtAfter(String key, UUID userId, Instant now)`
  - `findByClearanceRequestId(UUID clearanceRequestId)`

**Gateway abstraction:**
- `PaymentGateway.java` — interface with `initiate(PaymentRequest)` and `getProviderCode()`
- `PaymentRequest.java` — Java `record`
- `PaymentResult.java` — Java `record`
- `StubPaymentGateway.java` — `@ConditionalOnProperty(name = "payment.provider", havingValue = "stub", matchIfMissing = true)`

**Service:**
- `PaymentService.java` — full idempotency logic as specified in PRD Section 11.3

**Controller:**
- `PaymentController.java`
  - `POST /api/v1/clearances/{id}/payments` — online payment (RESIDENT) or manual trigger
  - `POST /api/v1/clearances/{id}/mark-paid` — cash payment (CLERK, ADMIN)
  - `GET /api/v1/clearances/{id}/payments` — get payment status (CLERK, ADMIN)

**DTOs:**
- `PaymentDTO.java` — includes `idempotent: boolean` flag

**MapStruct Mapper:**
- `PaymentMapper.java`

**Frontend:**
- "Pay Now" button on `/portal/requests/[id]` (calls `POST /me/clearances/{id}/pay`)
- Payment result feedback (success toast or error message)
- Use `crypto.randomUUID()` to generate `Idempotency-Key` before each payment attempt
- "Mark as Paid" button on `/backoffice/clearances/[id]` for CLERK role

---

### Key Implementation Notes

**Idempotency key validation (controller layer):**

```java
@PostMapping("/{id}/payments")
public ResponseEntity<PaymentDTO> initiatePayment(
        @PathVariable UUID id,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @AuthenticationPrincipal UserPrincipal principal) {

    // Validate UUID v4 format
    if (!isValidUuidV4(idempotencyKey)) {
        throw new BadRequestException("Idempotency-Key must be a valid UUID v4");
    }

    PaymentDTO result = paymentService.initiatePayment(id, idempotencyKey, principal.getUserId());
    HttpStatus status = result.isIdempotent() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status).body(result);
}
```

If the `Idempotency-Key` header is missing entirely, Spring will throw `MissingRequestHeaderException` → caught by `GlobalExceptionHandler` → 400 with message.

**PENDING conflict response:**

```java
if (existing.getStatus() == PaymentStatus.PENDING) {
    throw new ConflictException("Payment with this Idempotency-Key is already in progress. Retry after a moment.");
}
```

**`responseBody` JSONB column:**

For simplicity in MVP, serialize the `PaymentDTO` to a JSON string and store it as `TEXT` or `JSONB`:
```java
// In PaymentService, after update:
String body = objectMapper.writeValueAsString(paymentMapper.toDto(payment));
payment.setResponseBody(body);
```

On cache replay, deserialize:
```java
PaymentDTO cached = objectMapper.readValue(existing.getResponseBody(), PaymentDTO.class);
cached.setIdempotent(true);  // flag the response as a replay
return cached;
```

**Transaction boundary for `initiatePayment`:**

The method is `@Transactional`. The PENDING anchor insert (`paymentRepository.save(payment)`) followed by a flush (call `paymentRepository.saveAndFlush(payment)`) ensures the unique constraint fires immediately. If a concurrent request with the same key arrives between steps 2 and 5, the constraint will catch it and throw a `DataIntegrityViolationException` — handle this as a 409 Conflict.

**`StubPaymentGateway`:**

```java
@Value("${payment.stub.always-success:true}")
private boolean alwaysSuccess;

@Override
public PaymentResult initiate(PaymentRequest request) {
    boolean success = alwaysSuccess || ThreadLocalRandom.current().nextBoolean();
    return new PaymentResult(
        "STUB-" + UUID.randomUUID(),
        success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
        success ? "Stub payment succeeded" : "Stub payment failed (random)"
    );
}
```

Use `ThreadLocalRandom` (not `Random`) for thread safety.

**Fee resolution at payment time (ADR-010):**

```java
FeeConfig fees = feeConfigRepository.findById(1)
    .orElseThrow(() -> new IllegalStateException("fee_config not seeded"));
BigDecimal amount = clearance.getUrgency() == Urgency.EXPRESS
    ? fees.getExpressFee()
    : fees.getRegularFee();
```

The fee is read from `fee_config` at the moment the `POST /payments` request is processed — not at submission time.

**Waived fee edge case:**

If `regularFee = 0.00` or `expressFee = 0.00`, the `PaymentService` should detect amount == 0 and skip the gateway call, creating a `SUCCESS` payment immediately.

**Frontend — generating idempotency key:**

```typescript
const handlePayNow = async () => {
  const idempotencyKey = crypto.randomUUID(); // browser native, no library needed
  try {
    await api.post(`/clearances/${id}/payments`, {}, {
      headers: { 'Idempotency-Key': idempotencyKey }
    });
    toast.success('Payment successful!');
    refetch(); // re-fetch clearance to update payment status
  } catch (err) {
    toast.error('Payment failed. Please try again.');
  }
};
```

Do not persist the idempotency key between page loads in MVP — each "Pay Now" click generates a fresh key. The frontend relies on the 409 response if a click fires twice in quick succession.

---

### Definition of Done

- `POST /clearances/{id}/payments` with valid idempotency key and `APPROVED` clearance → 201 with `{ status: "SUCCESS" }` (stub always-success mode).
- Same request with same key (within 24h) → 200 with `{ idempotent: true }`.
- Same request with PENDING existing record → 409.
- Missing `Idempotency-Key` header → 400.
- `POST /clearances/{id}/mark-paid` as CLERK on `APPROVED + UNPAID` clearance → 200; clearance `paymentStatus` = `PAID`.
- Duplicate `mark-paid` on already-PAID clearance → 200 with existing payment record (no error).
- Payment creates audit log entry `PAYMENT_SUCCESS` or `PAYMENT_CASH_RECORDED`.

---

## Phase 5 — PDF Generation (Week 5)

### Goal

Generate barangay clearance certificates on demand using Apache PDFBox 3.x. The PDF must be production-quality: A4 layout, correct fonts, logo embedding, and all required fields from the clearance request and resident profile.

---

### Deliverables

**Service:**
- `ClearancePdfService.java` — interface: `byte[] generate(ClearanceRequest, Resident, BarangaySettings)`
- `ClearancePdfServiceImpl.java` — PDFBox implementation

**Wire into controllers:**
- `GET /api/v1/clearances/{id}/pdf` (CLERK, ADMIN) — in `ClearanceController`
- `GET /api/v1/me/clearances/{id}/pdf` (RESIDENT, RELEASED only) — in `PortalClearanceController`

**Frontend:**
- "Download PDF" button on `/portal/requests/[id]` — visible only when `status = RELEASED`
- "Print / Download PDF" button on `/backoffice/clearances/[id]` — visible when `status = RELEASED`
- Handle binary response from `api.get(url, { responseType: 'blob' })` and trigger browser download

---

### Key Implementation Notes

**PDFBox 3.x layout implementation:**

PDFBox uses a coordinate system where (0,0) is the bottom-left corner of the page. Keep track of the current `y` position and decrement it as you draw content downward.

```java
@Service
public class ClearancePdfServiceImpl implements ClearancePdfService {

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();   // 595.28 pt
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight(); // 841.89 pt
    private static final float MARGIN = 50f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    @Override
    public byte[] generate(ClearanceRequest request, Resident resident, BarangaySettings settings) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // 1. Header: logo (if present) + barangay info
                y = drawHeader(doc, cs, settings, y);

                // 2. Horizontal rule
                drawHorizontalLine(cs, y -= 10);
                y -= 15;

                // 3. Title: "BARANGAY CLEARANCE"
                y = drawCenteredText(cs, "BARANGAY CLEARANCE", PDType1Font.HELVETICA_BOLD, 16, y);
                y -= 20;

                // 4. Clearance number + date + validity
                y = drawText(cs, "Clearance No.: " + request.getClearanceNumber(), 12, y);
                y = drawText(cs, "Date: " + formatDate(request.getIssuedAt()), 12, y);
                y = drawText(cs, "Validity: 6 months from date of issue", 10, y);
                y -= 20;

                // 5. Body paragraph
                y = drawBodyParagraph(cs, request, resident, y);
                y -= 30;

                // 6. Signature block
                drawSignatureBlock(cs, settings, y);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}
```

**Logo embedding:**

```java
private float drawHeader(PDDocument doc, PDPageContentStream cs,
                         BarangaySettings settings, float y) throws IOException {
    float textX = MARGIN;
    if (settings.getLogoImage() != null) {
        try (InputStream logoStream = new ByteArrayInputStream(settings.getLogoImage())) {
            PDImageXObject logo = PDImageXObject.createFromByteArray(doc,
                settings.getLogoImage(), "logo");
            float logoHeight = Math.min(80f, logo.getHeight());
            float logoWidth = logo.getWidth() * (logoHeight / logo.getHeight()); // proportional scale
            cs.drawImage(logo, MARGIN, y - logoHeight, logoWidth, logoHeight);
            textX = MARGIN + logoWidth + 10;
        }
    }
    // Draw barangay info text to the right of the logo
    drawTextAt(cs, "REPUBLIKA NG PILIPINAS", textX, y - 12, 10);
    drawTextAt(cs, "LUNGSOD/BAYAN NG " + settings.getMunicipality().toUpperCase(), textX, y - 24, 10);
    drawTextAt(cs, "LALAWIGAN NG " + settings.getProvince().toUpperCase(), textX, y - 36, 10);
    drawTextAt(cs, "BARANGAY " + settings.getBarangayName().toUpperCase(), textX, y - 48,
               PDType1Font.HELVETICA_BOLD, 11);
    return y - 90; // advance past header
}
```

**Body paragraph with text wrapping:**

PDFBox does not have automatic text wrapping. Implement a simple word-wrap helper:

```java
private void drawWrappedText(PDPageContentStream cs, String text, PDFont font,
                              float fontSize, float x, float y, float maxWidth)
        throws IOException {
    List<String> lines = wrapText(text, font, fontSize, maxWidth);
    float lineHeight = fontSize * 1.4f;
    for (String line : lines) {
        drawTextAt(cs, line, x, y, font, fontSize);
        y -= lineHeight;
    }
}

private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth)
        throws IOException {
    List<String> lines = new ArrayList<>();
    String[] words = text.split(" ");
    StringBuilder line = new StringBuilder();
    for (String word : words) {
        String test = line.isEmpty() ? word : line + " " + word;
        float width = font.getStringWidth(test) / 1000 * fontSize;
        if (width > maxWidth && !line.isEmpty()) {
            lines.add(line.toString());
            line = new StringBuilder(word);
        } else {
            line = new StringBuilder(test);
        }
    }
    if (!line.isEmpty()) lines.add(line.toString());
    return lines;
}
```

**Body paragraph text:**

```java
String age = Period.between(resident.getBirthdate(), LocalDate.now()).getYears() + " years old";
String body = String.format(
    "This is to certify that %s %s, %s, born on %s, residing at %s, Purok/Zone %s, " +
    "is a bona fide resident of this barangay and has no derogatory record on file in this office. " +
    "This clearance is issued upon the request of the above-named person for the purpose of: %s.",
    resident.getFirstName(), resident.getLastName(),
    age,
    resident.getBirthdate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
    resident.getAddress(),
    resident.getPurokZone() != null ? resident.getPurokZone() : "N/A",
    request.getPurpose() == Purpose.OTHER ? request.getPurposeOther() : request.getPurpose().getDisplayName()
);
```

**Controller response:**

```java
@GetMapping("/{id}/pdf")
public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
    byte[] pdf = clearanceService.generatePdf(id, principal.getUserId(), principal.getRole());
    String filename = "clearance-" + /* clearanceNumber */ + ".pdf";
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(pdf);
}
```

For the resident portal endpoint, validate status == RELEASED before calling `generatePdf`.

**Frontend download trigger:**

```typescript
const handleDownloadPdf = async () => {
  const response = await api.get(`/me/clearances/${id}/pdf`, { responseType: 'blob' });
  const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `clearance-${clearanceNumber}.pdf`;
  link.click();
  URL.revokeObjectURL(url);
};
```

---

### Definition of Done

- `GET /api/v1/clearances/{id}/pdf` for a RELEASED clearance → 200 with `Content-Type: application/pdf`.
- PDF contains: clearance number, issuance date, resident full name, age, birthdate, address, purpose.
- PDF contains barangay name, municipality, province, captain name.
- If logo is uploaded in settings, logo appears in the PDF header.
- If no logo, header shows text only (no exception thrown).
- `GET /api/v1/me/clearances/{id}/pdf` for a non-RELEASED clearance → 403 (resident cannot download unreleased PDF).
- `ClearancePdfServiceTest` generates a non-null PDF byte array; opening it in a PDF viewer shows correct content.

---

## Phase 6 — Settings Module (Week 5)

### Goal

Enable the admin to configure barangay profile (name, logo, municipality, province, captain name) and clearance fees, which flow into both the PDF and the payment amount calculation.

---

### Deliverables

**Entities:**
- `BarangaySettings.java` — `@Entity`, `id = 1` singleton row
- `FeeConfig.java` — `@Entity`, `id = 1` singleton row

**Repositories:**
- `BarangaySettingsRepository.java` — `JpaRepository<BarangaySettings, Integer>`
- `FeeConfigRepository.java` — `JpaRepository<FeeConfig, Integer>`

**Service:**
- `SettingsService.java`
  - `getSettings()` — always returns row with id=1
  - `updateSettings(BarangaySettingsDTO)` — patch fields (do not overwrite logo if not provided)
  - `uploadLogo(MultipartFile)` — validate type + size; store as `byte[]`
  - `getFees()` — return row with id=1
  - `updateFees(FeeConfigDTO)`

**Controller:**
- `SettingsController.java` — all endpoints under `/api/v1/settings/**` (ADMIN only)

**DTOs:**
- `BarangaySettingsDTO.java` — excludes `logoImage` binary field; includes `hasLogo: boolean`
- `FeeConfigDTO.java` — `{ regularFee, expressFee }`

**Frontend:**
- `frontend/src/app/backoffice/admin/settings/page.tsx` — form + logo upload with preview
- `frontend/src/app/backoffice/admin/settings/fees/page.tsx` — fee form
- `frontend/src/types/settings.ts`

---

### Key Implementation Notes

**Logo upload validation:**

```java
@PostMapping("/logo")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> uploadLogo(@RequestParam("file") MultipartFile file) {
    // Validate content type
    String contentType = file.getContentType();
    if (!List.of("image/png", "image/jpeg", "image/gif").contains(contentType)) {
        throw new BadRequestException("Only PNG, JPG, or GIF files are accepted.");
    }
    // Validate size: 2 MB = 2 * 1024 * 1024 bytes
    if (file.getSize() > 2 * 1024 * 1024) {
        throw new BadRequestException("Logo must be less than 2 MB.");
    }
    settingsService.uploadLogo(file.getBytes(), contentType);
    return ResponseEntity.noContent().build();
}
```

Configure Spring's multipart size limit in `application.yml`:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 3MB
```

**Logo retrieval for settings UI:**

The `BarangaySettingsDTO` should include a `logoUrl` field pointing to a separate endpoint for fetching the logo binary, or include the logo as a Base64 data URL. For MVP simplicity, provide a separate `GET /settings/logo` endpoint that returns the raw bytes with the stored `Content-Type`, and use it in the frontend `<img>` tag.

**Single-row pattern:**

```java
public BarangaySettings getSettings() {
    return settingsRepository.findById(1)
        .orElseThrow(() -> new IllegalStateException("barangay_settings not seeded by Flyway V2"));
}
```

Never allow `POST /settings` — only `PUT`. The row always exists (seeded in V2).

**Fee update:**

```java
public FeeConfig updateFees(FeeConfigDTO dto) {
    FeeConfig config = feeConfigRepository.findById(1).orElseThrow();
    config.setRegularFee(dto.getRegularFee());
    config.setExpressFee(dto.getExpressFee());
    config.setUpdatedAt(Instant.now());
    return feeConfigRepository.save(config);
}
```

No audit log for fee changes in MVP, but add one for Phase 2 compliance.

**Logo preview in frontend:**

```typescript
// Fetch logo as blob and create an object URL
const { data: logoBlob } = useQuery({
  queryKey: ['settings-logo'],
  queryFn: () => api.get('/settings/logo', { responseType: 'blob' }).then(r => r.data),
  enabled: hasLogo,
});
const logoUrl = logoBlob ? URL.createObjectURL(logoBlob) : null;
```

Clean up object URLs with `useEffect` cleanup to prevent memory leaks.

---

### Definition of Done

- `GET /api/v1/settings` as ADMIN → 200 with current settings.
- `PUT /api/v1/settings` updates barangay name; next PDF generation reflects the new name.
- `POST /api/v1/settings/logo` with valid PNG ≤ 2MB → 204. With oversized file → 400.
- `POST /api/v1/settings/logo` with non-image file → 400.
- `PUT /api/v1/settings/fees` updates fees; next payment initiation uses the new fee amount.
- Frontend: logo upload shows a preview after selection. Save shows success toast.

---

## Phase 7 — Reports Module (Week 6)

### Goal

Provide a filterable, paginated report of clearance issuances accessible to clerks and admins, meeting the log-book replacement requirement.

---

### Deliverables

**Service:**
- `ReportsService.java` — dynamic JPA query with optional filters

**Controller:**
- `ReportsController.java` — `GET /api/v1/reports/clearances`

**DTOs:**
- `ReportFilterRequest.java` — query params: `from`, `to`, `status`, `purok`, `purpose`, `paymentStatus`, `page`, `size`
- `ReportRowDTO.java` — `{ clearanceNumber, residentFullName, purpose, urgency, status, paymentStatus, issuedAt }`

**Frontend:**
- `frontend/src/app/backoffice/reports/page.tsx` — filter form + paginated table

---

### Key Implementation Notes

**Dynamic query using JPA Criteria API or JPQL:**

Use a custom repository implementation with `@Query` that builds the where clause from nullable parameters:

```java
@Query("""
    SELECT cr FROM ClearanceRequest cr
    JOIN cr.resident r
    WHERE (:status IS NULL OR cr.status = :status)
      AND (:paymentStatus IS NULL OR cr.paymentStatus = :paymentStatus)
      AND (:purpose IS NULL OR cr.purpose = :purpose)
      AND (:purok IS NULL OR LOWER(r.purokZone) LIKE LOWER(CONCAT('%', :purok, '%')))
      AND (:from IS NULL OR cr.issuedAt >= :from)
      AND (:to IS NULL OR cr.issuedAt <= :to)
    ORDER BY cr.issuedAt DESC
    """)
Page<ClearanceRequest> findByFilters(
    @Param("status") ClearanceStatus status,
    @Param("paymentStatus") ClearancePaymentStatus paymentStatus,
    @Param("purpose") Purpose purpose,
    @Param("purok") String purok,
    @Param("from") Instant from,
    @Param("to") Instant to,
    Pageable pageable);
```

Note: `NULL` comparison in JPQL using `:param IS NULL` works in Hibernate but use `@Nullable` parameters carefully. Test this with Testcontainers to ensure PostgreSQL compatibility.

**Controller:**

```java
@GetMapping("/clearances")
@PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")
public PageResponse<ReportRowDTO> getReport(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String paymentStatus,
        @RequestParam(required = false) String purpose,
        @RequestParam(required = false) String purok,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to,
        @PageableDefault(size = 20, sort = "issuedAt", direction = DESC) Pageable pageable) {

    ClearanceStatus statusEnum = status != null ? ClearanceStatus.valueOf(status) : null;
    // ... parse other enums
    Instant fromInstant = from != null ? from.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
    Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

    return PageResponse.of(reportsService.getReport(...).map(reportMapper::toRowDto));
}
```

**`ReportRowDTO` — resident name denormalization:**

Since `ReportRowDTO` needs `residentFullName` but `ClearanceRequest` only holds `resident_id`, either: (a) join in the JPQL query and project, or (b) map in the service layer after fetching. Option (b) is simpler with MapStruct:

```java
@Mapping(target = "residentFullName",
         expression = "java(cr.getResident().getFirstName() + ' ' + cr.getResident().getLastName())")
ReportRowDTO toRowDto(ClearanceRequest cr);
```

---

### Definition of Done

- `GET /api/v1/reports/clearances` without filters → 200, paginated list of all clearances.
- With `status=RELEASED&from=2025-02-01&to=2025-02-28` → only RELEASED clearances in that date range.
- With `purok=3` → only residents with purok containing "3".
- With RESIDENT token → 403.
- Frontend: filter form updates the table on submission. Empty state: "No records found for the selected filters."

---

## Phase 8 — Frontend Polish & Role-Based Navigation (Week 6)

### Goal

Wire up all frontend pages with real API integration, complete the `middleware.ts` route guard, implement the backoffice dashboard, and add polish: loading skeletons, error toasts, status timeline, and the "must change password" flow.

---

### Deliverables

**Route guard (`middleware.ts` — complete):**

```typescript
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { jwtDecode } from 'jwt-decode'; // add 'jwt-decode' package

export function middleware(req: NextRequest) {
  const token = req.cookies.get('accessToken')?.value
              ?? req.headers.get('Authorization')?.replace('Bearer ', '');

  const { pathname } = req.nextUrl;

  // Public routes
  if (pathname.startsWith('/login') || pathname.startsWith('/register')) {
    if (token) {
      const { role } = jwtDecode<{ role: string }>(token);
      return NextResponse.redirect(new URL(
        role === 'RESIDENT' ? '/portal/dashboard' : '/backoffice/dashboard', req.url));
    }
    return NextResponse.next();
  }

  if (!token) {
    return NextResponse.redirect(new URL('/login', req.url));
  }

  const { role } = jwtDecode<{ role: string }>(token);

  if (pathname.startsWith('/portal') && role !== 'RESIDENT') {
    return NextResponse.redirect(new URL('/backoffice/dashboard', req.url));
  }
  if (pathname.startsWith('/backoffice') && role === 'RESIDENT') {
    return NextResponse.redirect(new URL('/portal/dashboard', req.url));
  }
  if (pathname.startsWith('/backoffice/admin') && !['ADMIN'].includes(role)) {
    return NextResponse.redirect(new URL('/backoffice/dashboard', req.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/portal/:path*', '/backoffice/:path*', '/login', '/register'],
};
```

Note: `jwtDecode` is a client-side decode (no signature verification). Verification happens on the backend. This is acceptable for routing purposes. Add `jwt-decode` to `package.json`.

**`AuthContext.tsx` — complete:**

```typescript
interface AuthContextValue {
  user: { userId: string; role: UserRole; email: string } | null;
  accessToken: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<string | null>;
}
```

Persist `accessToken` to `localStorage` on login. On app initialization, read from `localStorage` and re-hydrate context. On 401 in Axios interceptor, call `refreshToken()` from context.

**Backoffice dashboard (`/backoffice/dashboard`):**

```typescript
// Fetch summary counts
const { data: summary } = useQuery({
  queryKey: ['dashboard-summary'],
  queryFn: () => api.get('/clearances/summary').then(r => r.data),
  refetchInterval: 30000, // auto-refresh every 30 seconds
});
```

Display three `<Card>` components: Pending Review, Approved Awaiting Payment, Released Today. Use `<Skeleton>` from shadcn/ui while loading.

**`StatusTimeline` component:**

```typescript
const steps = [
  { label: 'Submitted', status: 'FOR_APPROVAL', date: request.submittedAt },
  { label: 'Under Review / Approved', status: 'APPROVED', date: request.approvedAt },
  { label: 'Payment', status: 'PAID', paymentStatus: request.paymentStatus },
  { label: 'Released', status: 'RELEASED', date: request.releasedAt },
];
```

Mark each step as completed (green check), current (yellow spinner), or pending (grey circle). If status is `REJECTED`, show a red step with the rejection reason.

**Error toast system:**

Install and configure Radix UI `Toast` (via shadcn/ui `toast` component). Create a `useToast` hook. All API error responses caught in Axios interceptor or `try/catch` blocks trigger `toast.error(errorMessage)`. All success actions trigger `toast.success(...)`.

**Loading skeletons:**

Use shadcn/ui `Skeleton` component for all list pages and detail pages while TanStack Query is in `isLoading` state.

**"Must change password" flow:**

1. After login, if `mustChangePassword` is in the JWT, redirect to `/change-password`.
2. `PUT /api/v1/auth/change-password` endpoint: validate current password, set new password, clear `mustChangePassword`, return new tokens.
3. Frontend: simple form with current password + new password + confirm fields.

**Mobile-first Tailwind responsive breakpoints:**

- Default (mobile): single column stack layout
- `md:` (768px+): sidebar + main content for backoffice
- `lg:` (1024px+): wider sidebar, more table columns

---

### Definition of Done

- Navigating to `/portal/dashboard` without a token → redirects to `/login`.
- CLERK token trying to access `/portal/dashboard` → redirected to `/backoffice/dashboard`.
- RESIDENT token trying to access `/backoffice/clearances` → redirected to `/portal/dashboard`.
- CLERK/APPROVER token trying to access `/backoffice/admin/users` → redirected to `/backoffice/dashboard`.
- Dashboard stat cards load and show correct counts.
- Status timeline shows correct step for each clearance status.
- Loading skeletons appear before data loads; no layout shift on load.
- Toast notifications appear on success and error actions.
- First-login admin (mustChangePassword=true) is prompted to change password before accessing any other page.

---

## Phase 9 — Testing & QA (Week 7)

### Goal

Verify correctness of critical business logic with automated unit and integration tests, and complete a manual end-to-end QA pass of the full clearance workflow.

---

### Deliverables

**Unit Tests (JUnit 5 + Mockito):**

| Test Class | Key Scenarios |
|---|---|
| `JwtServiceTest` | Token generation, validation, expiry, invalid signature → exception |
| `AuthServiceTest` | Register success, duplicate email → exception; login OK, wrong password, PENDING status; refresh, logout |
| `ClearanceServiceTest` | All state transitions with correct guards; illegal transitions throw `BadRequestException`; reject without reason throws; release with UNPAID throws |
| `ClearanceNumberServiceTest` | Sequential number assignment for one month; monthly reset (different year_month → resets to 0001); concurrent calls return unique sequential numbers |
| `PaymentServiceTest` | New payment with success stub → SUCCESS; idempotent replay (SUCCESS found) → cached response; PENDING found → `ConflictException`; already PAID clearance → `ConflictException`; cash mark-as-paid; duplicate mark-as-paid → existing record |
| `ClearancePdfServiceTest` | Generate returns non-null byte array; byte array starts with `%PDF` magic bytes; no exception when logo is null |

**Integration Tests (Testcontainers PostgreSQL):**

All integration tests extend `BaseIntegrationTest`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");
}
```

| Test Class | Coverage |
|---|---|
| `AuthControllerIT` | Register → 201; login → 200 with tokens; refresh → 200; logout → 200; second use of same refresh → 401 |
| `ResidentControllerIT` | CRUD with CLERK token; search pagination; pending-users list; activate/reject |
| `ClearanceWorkflowIT` | Full workflow: register + activate resident → submit clearance → approve → mark paid → release → download PDF |
| `ClearanceWorkflowIT` | Rejection path: reject with reason → resident edits and resubmits → FOR_APPROVAL |
| `PaymentControllerIT` | Idempotency: same key → 200 + idempotent:true; PENDING → 409; missing header → 400 |
| `SettingsControllerIT` | Update settings → 200; upload valid logo → 204; upload oversized → 400; upload non-image → 400 |
| `SecurityGuardIT` | Verify RESIDENT cannot hit `/clearances/{id}/approve`; CLERK cannot hit `/admin/users`; unauthenticated → 401 |

**Manual QA Checklist:**

Complete these flows end-to-end in the browser with the full Docker Compose stack running:

1. **Full clearance workflow:**
   - Register as resident → confirmation message
   - Log in as CLERK → activate the resident
   - Log in as resident → submit new clearance request (Employment, Regular, 1 copy)
   - Log in as APPROVER → approve the request
   - Log in as resident → click "Pay Now" → payment success toast
   - Log in as CLERK → click "Release" → clearance number assigned (2026-02-0001)
   - Log in as resident → click "Download PDF" → PDF downloads and opens correctly

2. **Rejection + resubmit flow:**
   - Submit request as resident
   - Log in as APPROVER → reject with reason "Incomplete documents"
   - Log in as resident → see rejection reason → click "Edit & Resubmit" → update notes → resubmit
   - Verify status returns to FOR_APPROVAL

3. **Duplicate payment idempotency:**
   - Approve a clearance
   - Log in as resident → click "Pay Now" twice rapidly
   - Verify only one payment record created

4. **Admin settings:**
   - Log in as ADMIN → upload logo → update captain name
   - Release a new clearance → verify logo and captain name appear in the downloaded PDF

5. **Role guard verification:**
   - With resident JWT, navigate directly to `/backoffice/clearances` → redirected
   - With resident JWT, call `POST /api/v1/clearances/{id}/approve` directly → 403

---

### Definition of Done

- `./mvnw test` passes with 0 failures.
- Code coverage (via JaCoCo, optional) on service classes ≥ 70%.
- All 5 manual QA checklists pass end-to-end.
- No `500 Internal Server Error` responses in any tested scenario.

---

## Phase 10 — Deployment (Week 7–8)

### Goal

Containerize both services and provide a production-ready Docker Compose configuration with Nginx TLS termination, health checks, and a daily PostgreSQL backup script.

---

### Deliverables

**Backend Dockerfile:**

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Frontend Dockerfile:**

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Runtime (Next.js standalone output)
FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/public ./public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
EXPOSE 3000
CMD ["node", "server.js"]
```

Configure Next.js standalone output in `next.config.ts`:
```typescript
const nextConfig = {
  output: 'standalone',
};
```

**`docker-compose.yml` — Production:**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-barangay_clearance}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME:-barangay_clearance}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  backend:
    image: barangay-clearance-api:latest
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-barangay_clearance}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      PAYMENT_PROVIDER: ${PAYMENT_PROVIDER:-stub}
      PAYMENT_STUB_ALWAYS_SUCCESS: ${PAYMENT_STUB_ALWAYS_SUCCESS:-true}
    expose:
      - "8080"

  frontend:
    image: barangay-clearance-web:latest
    restart: unless-stopped
    environment:
      NEXT_PUBLIC_API_URL: ${NEXT_PUBLIC_API_URL:-/api/v1}
    expose:
      - "3000"

  nginx:
    image: nginx:1.27-alpine
    restart: unless-stopped
    depends_on:
      - backend
      - frontend
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./nginx/certs:/etc/nginx/certs:ro
    ports:
      - "80:80"
      - "443:443"

volumes:
  postgres_data:
```

**`nginx/nginx.conf` — Production:**

```nginx
server {
    listen 80;
    server_name _;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Increase buffer for PDF responses
    proxy_buffer_size 128k;
    proxy_buffers 4 256k;
    proxy_busy_buffers_size 256k;

    # Backend API
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
    }

    # Next.js frontend
    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_http_version 1.1;
    }
}
```

**PostgreSQL backup script (`scripts/backup-db.sh`):**

```bash
#!/bin/bash
# Run daily via cron: 0 2 * * * /path/to/backup-db.sh
set -euo pipefail

BACKUP_DIR="/opt/barangay-backups"
DATE=$(date +%Y%m%d_%H%M%S)
FILENAME="barangay_clearance_${DATE}.sql.gz"
RETENTION_DAYS=30

mkdir -p "$BACKUP_DIR"

docker exec barangay-clearance-postgres-1 \
  pg_dump -U "$DB_USER" "$DB_NAME" \
  | gzip > "${BACKUP_DIR}/${FILENAME}"

echo "[$(date)] Backup created: ${FILENAME}"

# Purge backups older than RETENTION_DAYS days
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete
echo "[$(date)] Old backups purged."
```

Make executable: `chmod +x scripts/backup-db.sh`. Add to host cron: `0 2 * * * /opt/barangay-clearance/scripts/backup-db.sh >> /var/log/barangay-backup.log 2>&1`.

**Production deployment checklist:**

```
Environment Variable Checklist (required for production):
[ ] DB_USER=barangay_user                          # non-default username
[ ] DB_PASSWORD=<strong_random_password>           # minimum 20 chars
[ ] DB_NAME=barangay_clearance
[ ] JWT_SECRET=<openssl rand -hex 32>              # 256-bit random secret, never reuse
[ ] NEXT_PUBLIC_API_URL=https://your-domain.com/api/v1
[ ] SPRING_PROFILES_ACTIVE=prod
[ ] PAYMENT_PROVIDER=stub                          # change to paymongo/maya in Phase 2
[ ] PAYMENT_STUB_ALWAYS_SUCCESS=true

Pre-launch Steps:
[ ] TLS certificate (Let's Encrypt certbot or self-signed for intranet):
    certbot certonly --standalone -d your-domain.com
    cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/certs/
    cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/certs/
[ ] Build images: docker build -t barangay-clearance-api:latest backend/
[ ] Build images: docker build -t barangay-clearance-web:latest frontend/
[ ] Deploy: docker compose --env-file .env up -d
[ ] Verify Flyway migrations ran: docker logs barangay-clearance-backend-1 | grep "Flyway"
[ ] Log in as admin@barangay.gov.ph, change password
[ ] Configure barangay settings (name, logo, captain name)
[ ] Set production fees
[ ] Configure cron for daily database backup
```

---

### Definition of Done

- `docker compose --env-file .env up -d` starts all 4 services with no errors.
- `docker compose ps` shows all services as healthy.
- `https://your-domain.com` loads the login page.
- `https://your-domain.com/api/v1/auth/login` responds (HTTP 200 for valid credentials).
- `http://your-domain.com` redirects to HTTPS (301).
- `./scripts/backup-db.sh` creates a `.sql.gz` file in the backup directory.
- All Phase 9 QA flows pass on the Docker Compose stack.

---

## 15. Cross-Phase Reference: API Endpoint Inventory

| Phase | Method | Path | Role | Controller |
|---|---|---|---|---|
| 1 | POST | `/api/v1/auth/register` | Public | AuthController |
| 1 | POST | `/api/v1/auth/login` | Public | AuthController |
| 1 | POST | `/api/v1/auth/refresh` | Public | AuthController |
| 1 | POST | `/api/v1/auth/logout` | Any | AuthController |
| 1 | PUT | `/api/v1/auth/change-password` | Any | AuthController |
| 1 | GET | `/api/v1/admin/users` | ADMIN | UserController |
| 1 | POST | `/api/v1/admin/users` | ADMIN | UserController |
| 1 | PUT | `/api/v1/admin/users/{id}/deactivate` | ADMIN | UserController |
| 2 | GET | `/api/v1/residents` | CLERK, ADMIN | ResidentController |
| 2 | POST | `/api/v1/residents` | CLERK, ADMIN | ResidentController |
| 2 | GET | `/api/v1/residents/{id}` | CLERK, ADMIN | ResidentController |
| 2 | PUT | `/api/v1/residents/{id}` | CLERK, ADMIN | ResidentController |
| 2 | GET | `/api/v1/residents/pending-users` | CLERK, ADMIN | ResidentController |
| 2 | POST | `/api/v1/residents/users/{userId}/activate` | CLERK, ADMIN | ResidentController |
| 2 | POST | `/api/v1/residents/users/{userId}/reject` | CLERK, ADMIN | ResidentController |
| 3 | GET | `/api/v1/me/clearances` | RESIDENT | PortalClearanceController |
| 3 | POST | `/api/v1/me/clearances` | RESIDENT | PortalClearanceController |
| 3 | GET | `/api/v1/me/clearances/{id}` | RESIDENT | PortalClearanceController |
| 3 | PUT | `/api/v1/me/clearances/{id}` | RESIDENT | PortalClearanceController |
| 3 | GET | `/api/v1/clearances` | CLERK, APPROVER, ADMIN | ClearanceController |
| 3 | POST | `/api/v1/clearances` | CLERK, ADMIN | ClearanceController |
| 3 | GET | `/api/v1/clearances/{id}` | CLERK, APPROVER, ADMIN | ClearanceController |
| 3 | POST | `/api/v1/clearances/{id}/approve` | APPROVER, ADMIN | ClearanceController |
| 3 | POST | `/api/v1/clearances/{id}/reject` | APPROVER, ADMIN | ClearanceController |
| 3 | POST | `/api/v1/clearances/{id}/release` | CLERK, ADMIN | ClearanceController |
| 3 | GET | `/api/v1/clearances/summary` | CLERK, APPROVER, ADMIN | ClearanceController |
| 4 | POST | `/api/v1/me/clearances/{id}/pay` | RESIDENT | PortalClearanceController |
| 4 | POST | `/api/v1/clearances/{id}/payments` | RESIDENT, CLERK, ADMIN | PaymentController |
| 4 | POST | `/api/v1/clearances/{id}/mark-paid` | CLERK, ADMIN | PaymentController |
| 4 | GET | `/api/v1/clearances/{id}/payments` | CLERK, ADMIN | PaymentController |
| 5 | GET | `/api/v1/me/clearances/{id}/pdf` | RESIDENT | PortalClearanceController |
| 5 | GET | `/api/v1/clearances/{id}/pdf` | CLERK, ADMIN | ClearanceController |
| 6 | GET | `/api/v1/settings` | ADMIN | SettingsController |
| 6 | PUT | `/api/v1/settings` | ADMIN | SettingsController |
| 6 | POST | `/api/v1/settings/logo` | ADMIN | SettingsController |
| 6 | GET | `/api/v1/settings/logo` | ADMIN, CLERK | SettingsController |
| 6 | GET | `/api/v1/settings/fees` | ADMIN | SettingsController |
| 6 | PUT | `/api/v1/settings/fees` | ADMIN | SettingsController |
| 7 | GET | `/api/v1/reports/clearances` | CLERK, ADMIN | ReportsController |

---

*End of Implementation Plan — Version 1.0.0*