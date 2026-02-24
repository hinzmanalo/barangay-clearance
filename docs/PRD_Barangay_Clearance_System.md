# PRD — Barangay Clearance System (MVP)

**Version:** 1.0.0
**Date:** 2026-02-24
**Author:** Solo Developer
**Status:** Ready for Development

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Stakeholders & User Roles](#2-stakeholders--user-roles)
3. [User Stories & Acceptance Criteria](#3-user-stories--acceptance-criteria)
4. [Functional Requirements](#4-functional-requirements)
5. [Non-Functional Requirements](#5-non-functional-requirements)
6. [System Architecture Design](#6-system-architecture-design) — [Backend Tech Stack](#60-backend-tech-stack-summary)
7. [Database Schema Design](#7-database-schema-design)
8. [API Endpoint Specification](#8-api-endpoint-specification)
9. [Clearance Workflow State Machine](#9-clearance-workflow-state-machine)
10. [PDF Generation Specification](#10-pdf-generation-specification)
11. [Payment Module Design](#11-payment-module-design)
12. [Frontend UI/UX Specification](#12-frontend-uiux-specification)
13. [Security Design](#13-security-design)
14. [Testing Strategy](#14-testing-strategy)
15. [Deployment Architecture](#15-deployment-architecture)
16. [Phase 2 Roadmap](#16-phase-2-roadmap)
17. [Technical Decisions Log (ADRs)](#17-technical-decisions-log-adrs)

---

## 1. Executive Summary

### 1.1 Project Overview

The **Barangay Clearance System** digitizes the end-to-end barangay clearance issuance process. Residents can submit clearance requests online via a resident portal; barangay staff (clerks and approvers) manage, approve, and release clearances via a back-office dashboard. The system replaces paper-based logbooks and manual tracking.

### 1.2 MVP Goal

> Resident → Online Request → Clerk Review → Approver Decision → Payment → Clerk Release → PDF Download

### 1.3 Success Metrics

| Metric | Target |
|--------|--------|
| Clearance request submission via portal | 70% of all requests |
| Average processing time (submission to release) | < 2 business days |
| Zero lost or untracked clearance requests | 100% tracked digitally |
| Staff onboarding time | < 1 day |
| System uptime | 99% |

### 1.4 Scope (MVP)

**In scope:**
- Resident registration, login, request submission, status tracking, PDF download
- Clerk: resident management, request review, payment recording, print & release
- Approver: approve or reject requests
- Admin: user management (staff accounts), barangay settings, fee configuration
- JWT-based authentication with role-based access control
- Payment stub (simulates PayMongo/Maya for Phase 2)
- Clearance PDF generation (Apache PDFBox)
- Basic reports (on-screen, no export)
- Docker Compose deployment

**Out of scope (Phase 2):**
- Email/SMS notifications
- Real payment gateway integration
- QR code verification
- Resident deduplication
- Multi-tenant management
- CSV/Excel export
- Offline/PWA support
- DPA compliance module

---

## 2. Stakeholders & User Roles

### 2.1 RESIDENT

A registered community member who needs a barangay clearance for employment, travel, loans, business, or school purposes.

**Goals:**
- Submit a clearance request without going to the barangay hall
- Track request status in real time
- Download the clearance PDF when released

**Pain points (current):** Must visit barangay hall multiple times, no visibility on processing status, paper receipts get lost.

**Portal access:** `/portal/*` routes only.

---

### 2.2 CLERK

A barangay staff member who manages the day-to-day clearance workflow.

**Goals:**
- Review and validate incoming resident requests
- Record payments (cashier function)
- Print and release clearances
- Maintain the resident registry

**Back-office access:** `/backoffice/clerk/*` routes.

---

### 2.3 APPROVER

The Barangay Captain or designated secretary who approves or rejects clearance requests.

**Goals:**
- Review requests forwarded by clerks
- Approve or reject with reason
- Maintain audit trail of decisions

**Back-office access:** `/backoffice/approver/*` routes.

---

### 2.4 ADMIN

The system administrator (usually the barangay IT officer or a senior clerk).

**Goals:**
- Create and manage staff accounts (Clerk, Approver accounts)
- Configure barangay profile (name, logo, municipality, province, captain name)
- Set clearance fees (Regular and Express)
- View all system reports

**Back-office access:** All `/backoffice/*` routes plus `/backoffice/admin/*`.

---

## 3. User Stories & Acceptance Criteria

### 3.1 Authentication

---

**US-AUTH-01: Resident self-registration**

> As a resident, I want to register an account so that I can submit clearance requests online.

**Acceptance Criteria:**
- Given I navigate to `/portal/register`, When I fill in first name, last name, birthdate, address, purok/zone, contact number, email, and password and submit, Then an account is created with status `PENDING_VERIFICATION` and I see a confirmation message telling me to wait for clerk activation.
- Given I submit the registration form, When the email already exists in the system, Then I see an error "Email is already registered."
- Given I submit the registration form, When the password is less than 8 characters, Then I see a validation error before the form is submitted.

---

**US-AUTH-02: Clerk verifies resident account**

> As a clerk, I want to verify pending resident registrations so that only real barangay residents can submit requests.

**Acceptance Criteria:**
- Given I am logged in as CLERK, When I navigate to the Pending Residents list, Then I see all accounts with status `PENDING_VERIFICATION`.
- Given I click "Activate" on a pending resident, When I confirm, Then the resident's status changes to `ACTIVE` and they can now log in and submit requests.
- Given I click "Reject" on a pending resident, When I confirm, Then the resident's status changes to `REJECTED` and they cannot log in.

---

**US-AUTH-03: Login**

> As any user, I want to log in with my email and password so that I can access the system.

**Acceptance Criteria:**
- Given I navigate to `/login`, When I enter correct credentials, Then I receive a JWT access token and refresh token, and I am redirected to my role's home page.
- Given I enter incorrect credentials, Then I see "Invalid email or password."
- Given I am a resident with status `PENDING_VERIFICATION`, Then I see "Your account is pending verification by the barangay clerk."
- Given my access token expires after 15 minutes, When I make an API call with the expired token, Then the client silently refreshes using the refresh token and retries the request.

---

**US-AUTH-04: Logout**

> As any user, I want to log out so that my session is invalidated.

**Acceptance Criteria:**
- Given I click logout, When the request is processed, Then my refresh token is revoked in the database and I am redirected to `/login`.
- Given I log out, When I try to use the old refresh token, Then I receive a 401 Unauthorized response.

---

### 3.2 Resident Registry

---

**US-REG-01: Create resident profile**

> As a clerk, I want to create a resident profile so that I can link it to a portal account or create a walk-in request.

**Acceptance Criteria:**
- Given I am logged in as CLERK and navigate to `/backoffice/residents/new`, When I fill in the required fields (first name, last name, birthdate, address, purok/zone, contact number) and submit, Then the resident profile is saved and I am redirected to the resident detail page.
- Given I submit the form, When a required field is empty, Then I see a field-level validation error.

---

**US-REG-02: Search residents**

> As a clerk, I want to search the resident registry by name, purok, or address so that I can quickly find a resident.

**Acceptance Criteria:**
- Given I am on the Residents list page, When I type in the search box, Then results filter in real time (debounced, 300ms) showing matching residents.
- Given no residents match the search query, Then I see "No residents found."

---

**US-REG-03: Edit resident profile**

> As a clerk, I want to update a resident's information when it changes.

**Acceptance Criteria:**
- Given I navigate to a resident's detail page and click "Edit", When I update fields and save, Then the updated values are saved and the `updated_at` timestamp is refreshed.

---

### 3.3 Resident Portal — Clearance Requests

---

**US-CLR-01: Submit clearance request**

> As a resident, I want to submit a clearance request online so that I don't have to visit the barangay hall.

**Acceptance Criteria:**
- Given I am logged in as an ACTIVE resident, When I navigate to `/portal/requests/new` and fill in purpose, number of copies, urgency (Regular/Express), and optional notes, and submit, Then a clearance request is created with status `FOR_APPROVAL` and payment status `UNPAID`, and I see the new request in My Requests.
- Given purpose is set to "Other", Then a free-text "Specify purpose" field appears and is required.
- Given I am a resident with status `PENDING_VERIFICATION`, Then the "New Request" button is disabled with a tooltip "Your account must be verified by the clerk first."

---

**US-CLR-02: Track request status**

> As a resident, I want to see the current status of my clearance request so that I know what is happening.

**Acceptance Criteria:**
- Given I am on My Requests list, When I click a request, Then I see a status timeline showing each completed and current step (FOR_APPROVAL → APPROVED → PAID → RELEASED).
- Given my request status is `FOR_APPROVAL`, Then I see "Your request is being reviewed by barangay staff."
- Given my request status is `APPROVED`, Then I see a "Pay Now" button (stub payment).
- Given my request status is `REJECTED`, Then I see the rejection reason and an "Edit & Resubmit" button.
- Given my request status is `RELEASED`, Then I see a "Download PDF" button.

---

**US-CLR-03: Pay for clearance (resident)**

> As a resident, I want to pay for my clearance online so that processing can proceed.

**Acceptance Criteria:**
- Given my request status is `APPROVED` and payment status is `UNPAID`, When I click "Pay Now", Then a stub payment is initiated.
- Given the stub payment returns SUCCESS, Then payment status changes to `PAID` and I see a "Payment Successful" confirmation.
- Given the stub payment returns FAILED, Then I see "Payment failed. Please try again." and the payment status remains `UNPAID`.

---

**US-CLR-04: Download clearance PDF**

> As a resident, I want to download my clearance PDF when it is released.

**Acceptance Criteria:**
- Given my request status is `RELEASED`, When I click "Download PDF", Then a PDF file is downloaded to my device.
- Given my request status is NOT `RELEASED`, Then the "Download PDF" button is not visible.

---

### 3.4 Back-office — Clearance Management

---

**US-BO-01: Review incoming requests**

> As a clerk, I want to see all incoming clearance requests so that I can monitor and process them.

**Acceptance Criteria:**
- Given I am logged in as CLERK, When I navigate to `/backoffice/clearances`, Then I see a paginated list of all clearance requests with columns: request no. (or "Pending"), resident name, purpose, urgency, status, payment status, submitted date.
- Given I apply filters (status, payment status, date range), Then the list updates to show only matching records.

---

**US-BO-02: Approve or reject request (Approver)**

> As an approver, I want to approve or reject a clearance request so that the workflow can proceed.

**Acceptance Criteria:**
- Given I am logged in as APPROVER, When I open a request with status `FOR_APPROVAL` and click "Approve", Then the status changes to `APPROVED`.
- Given I click "Reject", Then a dialog appears asking for a rejection reason (required), and upon confirmation the status changes to `REJECTED`.
- Given the status changes to `REJECTED`, Then the request goes back to the resident's portal as editable (`DRAFT`).

---

**US-BO-03: Mark payment as paid (Clerk)**

> As a clerk, I want to record a cash payment from a walk-in resident so that the clearance can proceed to release.

**Acceptance Criteria:**
- Given I open a request with status `APPROVED` and payment status `UNPAID`, When I click "Mark as Paid" and confirm, Then a payment record with method `CASH` is created and payment status changes to `PAID`.

---

**US-BO-04: Release clearance (Clerk)**

> As a clerk, I want to release a clearance after confirming payment so that the resident can receive their document.

**Acceptance Criteria:**
- Given a request has status `APPROVED` and payment status `PAID`, When I click "Release" and confirm, Then the clearance number is assigned (format: `YYYY-MM-NNNN`), status changes to `RELEASED`, and `issued_at` is set to the current timestamp.
- Given the release action succeeds, Then the PDF can be downloaded by both the clerk (for printing) and the resident (via portal).

---

**US-BO-05: Print clearance PDF (Clerk)**

> As a clerk, I want to print the clearance PDF from the back-office.

**Acceptance Criteria:**
- Given a request has status `RELEASED`, When I click "Print / Download PDF", Then a PDF is generated and downloaded for printing.
- Given the barangay settings have a logo uploaded, Then the logo appears in the PDF header.
- Given no logo is uploaded, Then the header shows only text.

---

### 3.5 Admin — Settings & User Management

---

**US-ADM-01: Manage staff accounts**

> As an admin, I want to create clerk and approver accounts so that barangay staff can use the back-office.

**Acceptance Criteria:**
- Given I navigate to `/backoffice/admin/users/new`, When I fill in name, email, role (CLERK or APPROVER), and submit, Then the account is created. A temporary password is shown once and must be changed on first login.
- Given I deactivate a staff account, Then that user can no longer log in.

---

**US-ADM-02: Configure barangay settings**

> As an admin, I want to configure the barangay profile so that the correct information appears on clearance PDFs.

**Acceptance Criteria:**
- Given I navigate to `/backoffice/admin/settings`, When I update barangay name, municipality, province, captain name, and upload a logo, and save, Then updated values are reflected immediately on all subsequent PDF generations.
- Given I upload a logo image larger than 2 MB, Then I see "Logo must be less than 2 MB."
- Given I upload a non-image file, Then I see "Only PNG, JPG, or GIF files are accepted."

---

**US-ADM-03: Configure clearance fees**

> As an admin, I want to set the Regular and Express clearance fees so that the correct amount is shown to residents.

**Acceptance Criteria:**
- Given I navigate to `/backoffice/admin/settings/fees`, When I set Regular fee to ₱50 and Express fee to ₱100 and save, Then all new clearance requests display the correct fee based on the selected urgency.
- Given a fee is set to 0, Then payments are marked as PAID automatically on submission (waived fee).

---

### 3.6 Reports

---

**US-RPT-01: View clearance issuance report**

> As a clerk or admin, I want to view a report of issued clearances so that I can track barangay activity.

**Acceptance Criteria:**
- Given I navigate to `/backoffice/reports`, When I set a date range and optionally filter by status, purok, purpose, or payment status, Then I see a table of matching clearance records.
- Given no records match the filters, Then I see "No records found for the selected filters."

---

## 4. Functional Requirements

### 4.1 Identity Module

- FR-ID-01: System shall support four roles: RESIDENT, CLERK, APPROVER, ADMIN.
- FR-ID-02: Residents shall self-register with: first name, last name, birthdate, address, purok/zone, contact number, email, password.
- FR-ID-03: Resident accounts shall default to `PENDING_VERIFICATION` status upon registration.
- FR-ID-04: CLERK or ADMIN shall be able to activate or reject pending resident accounts.
- FR-ID-05: ADMIN shall create CLERK and APPROVER accounts from the back-office Settings UI.
- FR-ID-06: Passwords shall be hashed using BCrypt (strength 12).
- FR-ID-07: Login shall return a JWT access token (15-min expiry) and a refresh token (7-day expiry).
- FR-ID-08: Refresh tokens shall be stored (hashed) in the database and revocable on logout.
- FR-ID-09: All protected endpoints shall require a valid JWT Bearer token in the `Authorization` header.
- FR-ID-10: The initial ADMIN account shall be seeded via Flyway migration with a forced password-change flag.

### 4.2 Residents Module

- FR-RES-01: CLERK shall create, view, update resident profiles.
- FR-RES-02: Resident profiles shall contain: id (UUID), first name, last name, birthdate, address, purok/zone, contact number, linked user account (optional FK).
- FR-RES-03: Residents list shall support search by full name, purok/zone, and address (case-insensitive, partial match).
- FR-RES-04: A resident profile may exist without a portal account (walk-in residents).
- FR-RES-05: A portal account shall be linked to at most one resident profile.

### 4.3 Clearance Module

- FR-CLR-01: Clearance request shall have the following fields: id (UUID), clearance_number (nullable until released), resident_id, purpose, purpose_other (nullable), urgency (REGULAR/EXPRESS), copies, notes, status, payment_status, submitted_at, approved_at, released_at, issued_at, rejection_reason, created_by.
- FR-CLR-02: Clearance status workflow: `FOR_APPROVAL` → `APPROVED` or `REJECTED`; `APPROVED` → `RELEASED`; `REJECTED` → `DRAFT` (resident edits and resubmits).
- FR-CLR-03: Payment status workflow: `UNPAID` → `PAID`.
- FR-CLR-04: Clearance number format: `YYYY-MM-NNNN` (e.g., `2025-02-0001`), assigned only at RELEASE.
- FR-CLR-05: Clearance number counter shall reset monthly per year-month.
- FR-CLR-06: The clearance PDF shall be generated on demand (not stored as a file) and shall include all fields specified in Section 10.
- FR-CLR-07: Only RELEASED clearances shall be downloadable by residents.
- FR-CLR-08: CLERK shall be able to create a clearance request on behalf of a walk-in resident.
- FR-CLR-09: Rejection shall require a non-empty reason text.
- FR-CLR-10: A rejected request returned to `DRAFT` shall retain its original submitted data for the resident to edit.

### 4.4 Payments Module

- FR-PAY-01: Each clearance request shall have exactly one payment record.
- FR-PAY-02: Payment record shall contain: id (UUID), clearance_request_id, idempotency_key (unique), amount, method (ONLINE/CASH), status (PENDING/SUCCESS/FAILED), provider (STUB/PAYMONGO/MAYA), provider_ref (nullable), created_at, updated_at.
- FR-PAY-03: The stub provider shall randomly return SUCCESS or always return SUCCESS (configurable via `payment.stub.always-success=true` in `application.yml`).
- FR-PAY-04: A CLERK shall mark a payment as PAID (CASH method) without triggering the stub provider.
- FR-PAY-05: The payment amount shall be derived from the active fee configuration at the time of payment initiation.
- FR-PAY-06: Payment provider shall be abstracted behind a `PaymentGateway` interface to allow Phase 2 plug-in.
- FR-PAY-07: Every payment initiation request (POST `/clearances/{id}/payments`) shall include a client-supplied `Idempotency-Key` header (UUID v4). The server shall store this key and return the same response for any duplicate request with the same key.
- FR-PAY-08: If the same `Idempotency-Key` is received and the original request is still in progress (PENDING), the server shall return HTTP 409 Conflict with a clear message.
- FR-PAY-09: If the same `Idempotency-Key` is received and the original request has completed (SUCCESS or FAILED), the server shall return HTTP 200 with the cached response body (no new gateway call).
- FR-PAY-10: `Idempotency-Key` values shall be scoped per user — the same key used by two different users shall be treated as distinct.
- FR-PAY-11: Stored idempotency keys shall expire after 24 hours. Expired keys may be reused without replaying the cached response.
- FR-PAY-12: The cash mark-as-paid action (CLERK) shall also be idempotent — a duplicate mark-as-paid on an already-PAID request shall return the existing payment record with no state change.

### 4.5 Audit Module

- FR-AUD-01: The system shall log the following events: user registration, account activation/rejection, login, clearance request created, submitted, approved, rejected, released, payment status change.
- FR-AUD-02: Each audit log entry shall contain: id, event_type, actor_user_id, target_entity_type, target_entity_id, old_value (JSON), new_value (JSON), ip_address, created_at.
- FR-AUD-03: Audit logs shall be append-only (no updates or deletes).

### 4.6 Settings Module

- FR-SET-01: ADMIN shall configure barangay name, municipality, province, captain name, captain position, and logo (PNG/JPG/GIF, max 2 MB).
- FR-SET-02: ADMIN shall set Regular fee amount and Express fee amount (decimal, Philippine Peso).
- FR-SET-03: Settings shall be stored in a single-row `barangay_settings` table, initialized by Flyway migration with placeholder values.
- FR-SET-04: Logo shall be stored as `BYTEA` in the database.

### 4.7 Reports Module

- FR-RPT-01: CLERK and ADMIN shall view an on-screen report of clearances filtered by: date range (issued_at), status, purok/zone, purpose, payment status.
- FR-RPT-02: Report shall display: clearance number, resident name, purpose, urgency, status, payment status, issued date.
- FR-RPT-03: Report shall be paginated (default 20 per page).

---

## 5. Non-Functional Requirements

### 5.1 Performance

- API response time for list endpoints: < 500ms at p95 under normal load (up to 50 concurrent users).
- PDF generation: < 3 seconds per document.
- Search results: < 300ms for resident name search.

### 5.2 Security

- All API endpoints (except `/api/v1/auth/login` and `/api/v1/auth/register`) shall require a valid JWT.
- Role-based access control enforced at the Spring Security method level (`@PreAuthorize`).
- Passwords stored as BCrypt hash (strength 12). Never logged or returned in responses.
- HTTPS enforced in production (Nginx reverse proxy terminates TLS).
- JWT secret stored as environment variable, never hardcoded.
- Input validation on all API endpoints (Bean Validation / Jakarta Validation).
- SQL injection prevention via JPA/Hibernate parameterized queries.
- XSS prevention via JSON API responses (no server-rendered HTML).

### 5.3 Maintainability

- Backend module structure: one Java package per module (`identity`, `residents`, `clearance`, `payments`, `audit`, `settings`, `reports`, `shared`).
- Controllers never call repositories directly.
- All database changes via Flyway versioned SQL migrations.
- API documented via OpenAPI 3 / Swagger UI.

### 5.4 Scalability

- Single-server Docker Compose for MVP. Architecture does not preclude horizontal scaling in Phase 2.
- Stateless API enables load balancing without session stickiness.
- Database connection pooling via HikariCP (default Spring Boot).

### 5.5 Reliability

- Database: PostgreSQL (Docker) with daily backup via `pg_dump` cron job on the host.
- Application restarts automatically via Docker `restart: unless-stopped` policy.
- PostgreSQL used in **all environments** (dev, test, prod) — no dialect switching, no compatibility surprises.

---

## 6. System Architecture Design

### 6.0 Backend Tech Stack Summary

| Concern | Technology | Version | Notes |
|---------|-----------|---------|-------|
| **Language** | Java | 21 (LTS) | Virtual threads via Project Loom available |
| **Framework** | Spring Boot | 3.3.x | Auto-configuration, embedded Tomcat |
| **Build Tool** | Maven | 3.9.x | `./mvnw` wrapper committed to repo |
| **Web Layer** | Spring MVC | (Spring Boot managed) | REST controllers, `@RestController` |
| **Security** | Spring Security | 6.x | JWT filter chain, `@PreAuthorize` RBAC |
| **JWT** | JJWT (io.jsonwebtoken) | 0.12.x | Access + refresh token signing/validation |
| **Password Hashing** | BCrypt | Spring Security built-in | Strength 12 |
| **ORM / Persistence** | Spring Data JPA + Hibernate | (Spring Boot managed) | Repositories, JPQL, native queries |
| **Database (dev)** | PostgreSQL 16 (Docker) | 16 | Local dev via `docker-compose.dev.yml`; same engine as prod |
| **Database (prod)** | PostgreSQL 16 | 16 | Production database; same image used in all environments |
| **DB Migrations** | Flyway | 9.x | Versioned `.sql` files under `db/migration/` |
| **Connection Pool** | HikariCP | (Spring Boot managed) | Default pool, configurable via `application.yml` |
| **Validation** | Jakarta Bean Validation | 3.x | `@Valid`, `@NotBlank`, `@Size`, etc. |
| **PDF Generation** | Apache PDFBox | 3.0.x | Clearance certificate generation (Apache License 2.0) |
| **Object Mapping** | MapStruct | 1.5.x | Compile-time DTO ↔ Entity mapping |
| **Boilerplate Reduction** | Lombok | 1.18.x | `@Data`, `@Builder`, `@Slf4j`, etc. |
| **API Documentation** | SpringDoc OpenAPI (Swagger UI) | 2.x | Auto-generated from annotations, served at `/swagger-ui.html` |
| **Testing** | JUnit 5 + Mockito + AssertJ | (Spring Boot managed) | Unit + integration tests |
| **Containerization** | Docker + Docker Compose | — | Single-server deployment |

### 6.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENTS                                 │
│                                                                   │
│   ┌─────────────────────┐    ┌──────────────────────────────┐   │
│   │  Resident Portal     │    │   Back-office (Clerk /        │   │
│   │  /portal/*           │    │   Approver / Admin)           │   │
│   │  (mobile-first)      │    │   /backoffice/*               │   │
│   └──────────┬──────────┘    └──────────────┬───────────────┘   │
└──────────────┼───────────────────────────────┼───────────────────┘
               │  HTTPS                        │  HTTPS
               ▼                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Next.js 14 App (Single App)                   │
│             Nginx reverse proxy (TLS termination)                │
└───────────────────────────────┬─────────────────────────────────┘
                                │  REST / JSON
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot 3.3 Modular Monolith                    │
│                                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │ identity │  │residents │  │clearance │  │  payments    │   │
│  │  module  │  │  module  │  │  module  │  │   module     │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘   │
│       │              │              │                │            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      audit module                         │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │               settings | reports | pdf | shared           │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────────────────────────┬─────────────────────────────────┘
                                │  JDBC / JPA
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      PostgreSQL 16                               │
│   users | residents | clearance_requests | payments |            │
│   audit_logs | barangay_settings | fee_config |                  │
│   refresh_tokens | clearance_number_sequence                     │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 Backend Module Structure

```
src/main/java/com/barangay/clearance/
├── identity/
│   ├── controller/      AuthController, UserController
│   ├── dto/             LoginRequest, RegisterRequest, TokenResponse, UserDTO
│   ├── entity/          User, RefreshToken
│   ├── repository/      UserRepository, RefreshTokenRepository
│   └── service/         AuthService, UserService, JwtService
│
├── residents/
│   ├── controller/      ResidentController
│   ├── dto/             ResidentDTO, ResidentSearchRequest
│   ├── entity/          Resident
│   ├── repository/      ResidentRepository
│   └── service/         ResidentService
│
├── clearance/
│   ├── controller/      ClearanceController, PortalClearanceController
│   ├── dto/             ClearanceRequestDTO, CreateClearanceRequest, ApproveRequest, RejectRequest
│   ├── entity/          ClearanceRequest, ClearanceNumberSequence
│   ├── repository/      ClearanceRequestRepository, ClearanceNumberSequenceRepository
│   └── service/         ClearanceService, ClearanceNumberService
│
├── payments/
│   ├── controller/      PaymentController
│   ├── dto/             PaymentDTO, InitiatePaymentRequest
│   ├── entity/          Payment
│   ├── repository/      PaymentRepository
│   ├── service/         PaymentService
│   └── gateway/         PaymentGateway (interface), StubPaymentGateway, PayMongoGateway (Phase 2)
│
├── audit/
│   ├── entity/          AuditLog
│   ├── repository/      AuditLogRepository
│   └── service/         AuditService
│
├── settings/
│   ├── controller/      SettingsController
│   ├── dto/             BarangaySettingsDTO, FeeConfigDTO
│   ├── entity/          BarangaySettings, FeeConfig
│   ├── repository/      BarangaySettingsRepository, FeeConfigRepository
│   └── service/         SettingsService
│
├── reports/
│   ├── controller/      ReportsController
│   ├── dto/             ReportFilterRequest, ReportRowDTO
│   └── service/         ReportsService
│
├── pdf/
│   └── service/         ClearancePdfService
│
└── shared/
    ├── exception/        GlobalExceptionHandler, AppException, ErrorResponse
    ├── security/         JwtAuthFilter, SecurityConfig
    └── util/             PageResponse, Constants
```

### 6.3 Frontend Route Map

```
/ → redirect based on role
/login
/register  (residents only)

/portal/                          (RESIDENT role required)
  /portal/dashboard               My Requests list
  /portal/requests/new            Submit new request
  /portal/requests/[id]           Request detail + status timeline + pay/download

/backoffice/                      (CLERK | APPROVER | ADMIN required)
  /backoffice/dashboard           Summary counts (pending, approved, released today)
  /backoffice/residents           Resident list + search
  /backoffice/residents/new       Create resident
  /backoffice/residents/[id]      Resident detail + edit
  /backoffice/clearances          All requests list + filters
  /backoffice/clearances/new      Create request for walk-in resident
  /backoffice/clearances/[id]     Request detail + action buttons
  /backoffice/reports             Reports with filters
  /backoffice/admin/users         User management  (ADMIN only)
  /backoffice/admin/users/new     Create staff account  (ADMIN only)
  /backoffice/admin/settings      Barangay settings + logo  (ADMIN only)
  /backoffice/admin/settings/fees Fee configuration  (ADMIN only)
```

---

## 7. Database Schema Design

### 7.1 `users`

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('RESIDENT','CLERK','APPROVER','ADMIN')),
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION'
                    CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','REJECTED','DEACTIVATED')),
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_status ON users(role, status);
```

---

### 7.2 `refresh_tokens`

```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
```

---

### 7.3 `residents`

```sql
CREATE TABLE residents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    birthdate       DATE         NOT NULL,
    address         VARCHAR(500) NOT NULL,
    purok_zone      VARCHAR(100),
    contact_number  VARCHAR(20),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_residents_name ON residents(lower(last_name), lower(first_name));
CREATE INDEX idx_residents_purok ON residents(lower(purok_zone));
CREATE INDEX idx_residents_user_id ON residents(user_id);
```

---

### 7.4 `clearance_requests`

```sql
CREATE TABLE clearance_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clearance_number    VARCHAR(20),           -- assigned at RELEASE: YYYY-MM-NNNN
    resident_id         UUID NOT NULL REFERENCES residents(id),
    purpose             VARCHAR(50) NOT NULL
                        CHECK (purpose IN (
                            'EMPLOYMENT','TRAVEL','LOAN_BUSINESS_LICENSING',
                            'SCHOOL_POLICE_OTHER','OTHER'
                        )),
    purpose_other       VARCHAR(255),          -- required when purpose = OTHER
    urgency             VARCHAR(10) NOT NULL CHECK (urgency IN ('REGULAR','EXPRESS')),
    copies              INTEGER NOT NULL DEFAULT 1 CHECK (copies >= 1 AND copies <= 10),
    notes               TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'FOR_APPROVAL'
                        CHECK (status IN ('DRAFT','FOR_APPROVAL','APPROVED','REJECTED','RELEASED')),
    payment_status      VARCHAR(10) NOT NULL DEFAULT 'UNPAID'
                        CHECK (payment_status IN ('UNPAID','PAID')),
    rejection_reason    TEXT,
    submitted_at        TIMESTAMPTZ,
    approved_at         TIMESTAMPTZ,
    released_at         TIMESTAMPTZ,
    issued_at           TIMESTAMPTZ,           -- same as released_at, stamped on PDF
    created_by_user_id  UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cr_resident ON clearance_requests(resident_id);
CREATE INDEX idx_cr_status ON clearance_requests(status);
CREATE INDEX idx_cr_issued_at ON clearance_requests(issued_at);
CREATE INDEX idx_cr_clearance_number ON clearance_requests(clearance_number);
```

---

### 7.5 `clearance_number_sequence`

```sql
CREATE TABLE clearance_number_sequence (
    year_month  CHAR(7) PRIMARY KEY,  -- e.g. '2025-02'
    last_seq    INTEGER NOT NULL DEFAULT 0
);
```

The service atomically increments `last_seq` and formats the number:
```sql
INSERT INTO clearance_number_sequence (year_month, last_seq) VALUES (:ym, 1)
ON CONFLICT (year_month) DO UPDATE SET last_seq = clearance_number_sequence.last_seq + 1
RETURNING last_seq;
```

---

### 7.6 `payments`

```sql
CREATE TABLE payments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clearance_request_id    UUID NOT NULL UNIQUE REFERENCES clearance_requests(id),
    idempotency_key         VARCHAR(255) NOT NULL,  -- client-supplied UUID v4 header
    amount                  NUMERIC(10,2) NOT NULL,
    method                  VARCHAR(10) NOT NULL CHECK (method IN ('ONLINE','CASH')),
    status                  VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    provider                VARCHAR(20) NOT NULL DEFAULT 'STUB'
                            CHECK (provider IN ('STUB','CASH','PAYMONGO','MAYA','GCASH')),
    provider_ref            VARCHAR(255),           -- external transaction ID (Phase 2)
    response_body           JSONB,                  -- cached response for idempotency replay
    initiated_by_user_id    UUID REFERENCES users(id),
    idempotency_expires_at  TIMESTAMPTZ NOT NULL,   -- NOW() + 24h; key reusable after expiry
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Unique constraint scoped per user: same key from two different users is allowed
CREATE UNIQUE INDEX idx_payments_idempotency ON payments(idempotency_key, initiated_by_user_id);
CREATE INDEX idx_payments_clearance ON payments(clearance_request_id);
CREATE INDEX idx_payments_idempotency_expiry ON payments(idempotency_expires_at);
```

---

### 7.7 `audit_logs`

```sql
CREATE TABLE audit_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type          VARCHAR(60) NOT NULL,
    actor_user_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    target_entity_type  VARCHAR(60),
    target_entity_id    UUID,
    old_value           JSONB,
    new_value           JSONB,
    ip_address          VARCHAR(45),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_actor ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_target ON audit_logs(target_entity_type, target_entity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
```

---

### 7.8 `barangay_settings`

```sql
CREATE TABLE barangay_settings (
    id                  INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),  -- single row
    barangay_name       VARCHAR(255) NOT NULL DEFAULT 'Barangay Name',
    municipality        VARCHAR(255) NOT NULL DEFAULT 'Municipality',
    province            VARCHAR(255) NOT NULL DEFAULT 'Province',
    captain_name        VARCHAR(255) NOT NULL DEFAULT 'Barangay Captain',
    captain_position    VARCHAR(100) NOT NULL DEFAULT 'Punong Barangay',
    logo_image          BYTEA,                 -- PNG/JPG/GIF, max 2 MB
    logo_content_type   VARCHAR(50),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Seeded by Flyway V2 migration
INSERT INTO barangay_settings (id) VALUES (1) ON CONFLICT DO NOTHING;
```

---

### 7.9 `fee_config`

```sql
CREATE TABLE fee_config (
    id              INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),  -- single row
    regular_fee     NUMERIC(10,2) NOT NULL DEFAULT 50.00,
    express_fee     NUMERIC(10,2) NOT NULL DEFAULT 100.00,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Seeded by Flyway V2 migration
INSERT INTO fee_config (id) VALUES (1) ON CONFLICT DO NOTHING;
```

---

## 8. API Endpoint Specification

**Base URL:** `/api/v1`
**Auth:** `Authorization: Bearer <access_token>` (all endpoints except auth)
**Content-Type:** `application/json`

---

### 8.1 Auth

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | Public | Resident self-registration |
| POST | `/auth/login` | Public | Login, returns tokens |
| POST | `/auth/refresh` | Public | Refresh access token |
| POST | `/auth/logout` | Any | Revoke refresh token |

**POST /auth/register**
```json
// Request
{
  "email": "juan@email.com",
  "password": "Str0ngP@ss",
  "firstName": "Juan",
  "lastName": "dela Cruz",
  "birthdate": "1990-05-15",
  "address": "123 Rizal St.",
  "purokZone": "Purok 3",
  "contactNumber": "09171234567"
}
// Response 201
{ "message": "Registration successful. Await clerk verification." }
```

**POST /auth/login**
```json
// Request
{ "email": "juan@email.com", "password": "Str0ngP@ss" }
// Response 200
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "role": "RESIDENT",
  "userId": "uuid"
}
```

**POST /auth/refresh**
```json
// Request
{ "refreshToken": "eyJ..." }
// Response 200
{ "accessToken": "eyJ..." }
```

---

### 8.2 Residents (Back-office)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/residents` | CLERK, ADMIN | List/search residents |
| POST | `/residents` | CLERK, ADMIN | Create resident |
| GET | `/residents/{id}` | CLERK, ADMIN | Get resident detail |
| PUT | `/residents/{id}` | CLERK, ADMIN | Update resident |
| GET | `/residents/pending-users` | CLERK, ADMIN | List pending portal registrations |
| POST | `/residents/users/{userId}/activate` | CLERK, ADMIN | Activate resident account |
| POST | `/residents/users/{userId}/reject` | CLERK, ADMIN | Reject resident account |

**GET /residents?q=dela+Cruz&purok=3&page=0&size=20**
```json
// Response 200
{
  "content": [
    {
      "id": "uuid",
      "firstName": "Juan",
      "lastName": "dela Cruz",
      "birthdate": "1990-05-15",
      "address": "123 Rizal St.",
      "purokZone": "Purok 3",
      "contactNumber": "09171234567",
      "hasPortalAccount": true
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

### 8.3 Portal — My Clearances (Resident)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/me/clearances` | RESIDENT | List own requests |
| POST | `/me/clearances` | RESIDENT | Submit new request |
| GET | `/me/clearances/{id}` | RESIDENT | Get request detail |
| PUT | `/me/clearances/{id}` | RESIDENT | Edit draft/rejected request |
| POST | `/me/clearances/{id}/pay` | RESIDENT | Initiate stub payment |
| GET | `/me/clearances/{id}/pdf` | RESIDENT | Download PDF (RELEASED only) |

**POST /me/clearances**
```json
// Request
{
  "purpose": "EMPLOYMENT",
  "purposeOther": null,
  "urgency": "REGULAR",
  "copies": 1,
  "notes": "For employment in Makati"
}
// Response 201
{
  "id": "uuid",
  "status": "FOR_APPROVAL",
  "paymentStatus": "UNPAID",
  "submittedAt": "2025-02-24T08:00:00Z"
}
```

---

### 8.4 Back-office — Clearances

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/clearances` | CLERK, APPROVER, ADMIN | List all requests (filterable) |
| POST | `/clearances` | CLERK, ADMIN | Create for walk-in resident |
| GET | `/clearances/{id}` | CLERK, APPROVER, ADMIN | Get detail |
| POST | `/clearances/{id}/approve` | APPROVER, ADMIN | Approve |
| POST | `/clearances/{id}/reject` | APPROVER, ADMIN | Reject with reason |
| POST | `/clearances/{id}/mark-paid` | CLERK, ADMIN | Mark as paid (cash) |
| POST | `/clearances/{id}/release` | CLERK, ADMIN | Release (assigns clearance number) |
| GET | `/clearances/{id}/pdf` | CLERK, ADMIN | Download/print PDF |

**POST /clearances/{id}/reject**
```json
// Request
{ "reason": "Incomplete residency proof" }
// Response 200
{ "id": "uuid", "status": "REJECTED", "rejectionReason": "Incomplete residency proof" }
```

**GET /clearances?status=FOR_APPROVAL&from=2025-02-01&to=2025-02-28&page=0&size=20**
```json
// Response 200
{
  "content": [ { "id": "uuid", "clearanceNumber": null, "residentName": "Juan dela Cruz", ... } ],
  "totalElements": 5,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

### 8.5 Payments

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/clearances/{id}/payments` | RESIDENT, CLERK, ADMIN | Initiate payment (stub or cash) |
| GET | `/clearances/{id}/payments` | CLERK, ADMIN | Get payment detail |

---

### 8.6 Settings (Admin)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/settings` | ADMIN | Get barangay settings |
| PUT | `/settings` | ADMIN | Update barangay settings |
| POST | `/settings/logo` | ADMIN | Upload logo (multipart/form-data) |
| GET | `/settings/fees` | ADMIN | Get fee config |
| PUT | `/settings/fees` | ADMIN | Update fees |

---

### 8.7 Admin — User Management

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/admin/users` | ADMIN | List staff users |
| POST | `/admin/users` | ADMIN | Create staff account |
| PUT | `/admin/users/{id}/deactivate` | ADMIN | Deactivate user |

---

### 8.8 Reports

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/reports/clearances` | CLERK, ADMIN | Clearance report with filters |

**GET /reports/clearances?from=2025-02-01&to=2025-02-28&status=RELEASED&page=0&size=20**

---

### 8.9 Error Response Format

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2025-02-24T08:00:00Z",
  "path": "/api/v1/me/clearances",
  "details": {
    "purpose": "Purpose is required"
  }
}
```

---

## 9. Clearance Workflow State Machine

```
                    ┌─────────────────┐
                    │  (resident      │
                    │   submits form) │
                    └────────┬────────┘
                             │ Submit
                             ▼
                    ┌─────────────────┐
                    │   FOR_APPROVAL  │◄──────────────────┐
                    └────────┬────────┘                   │
                             │                            │
              ┌──────────────┼──────────────┐            │
              │ Approve      │ Reject        │            │
              │ (APPROVER)   │ (APPROVER)    │            │
              ▼              ▼               │            │
    ┌──────────────┐  ┌───────────┐         │            │
    │   APPROVED   │  │ REJECTED  │         │            │
    └──────┬───────┘  └─────┬─────┘         │            │
           │                │ → DRAFT        │            │
           │                │ (resident      │            │
           │                │  edits &       │            │
           │                │  resubmits)    │            │
           │  Payment       └────────────────┘            │
           │  (RESIDENT pays online                       │
           │   OR CLERK marks paid)                       │
           ▼                                              │
    ┌──────────────┐                              ┌───────────┐
    │   APPROVED   │    Release (CLERK)            │   DRAFT   │
    │  PAID=true   │ ──────────────────────────►  │           │
    └──────────────┘                              │  (editing)│
                                                  └─────┬─────┘
           ▼                                            │ Resubmit
    ┌──────────────┐                                    │
    │   RELEASED   │◄───────────────────────────────────┘
    └──────────────┘
         (terminal)

Payment status: UNPAID ──► PAID
  Triggered by: resident online stub OR clerk mark-as-paid
```

### State Transition Guards

| From | To | Actor | Guard |
|------|----|-------|-------|
| — | FOR_APPROVAL | RESIDENT | Account status = ACTIVE |
| FOR_APPROVAL | APPROVED | APPROVER, ADMIN | — |
| FOR_APPROVAL | REJECTED | APPROVER, ADMIN | rejection_reason required |
| REJECTED | DRAFT | System | Automatic on rejection |
| DRAFT | FOR_APPROVAL | RESIDENT | All required fields present |
| APPROVED | RELEASED | CLERK, ADMIN | payment_status = PAID |
| UNPAID | PAID | RESIDENT, CLERK, ADMIN | clearance status = APPROVED |

---

## 10. PDF Generation Specification

### 10.1 Library

**Apache PDFBox 3.x** (Apache License 2.0)

Justification: iText 7 requires a commercial license for proprietary deployments (AGPL v3). PDFBox is fully open-source, supports coordinate-based layout control, and handles PNG/JPG image embedding for the logo.

**Maven dependency:**
```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.2</version>
</dependency>
```

### 10.2 Document Layout

```
┌─────────────────────────────────────────────────────┐
│  [LOGO]   REPUBLIKA NG PILIPINAS                    │
│           LUNGSOD/BAYAN NG {municipality}            │
│           LALAWIGAN NG {province}                    │
│           BARANGAY {barangay_name}                   │
├─────────────────────────────────────────────────────┤
│                                                     │
│              BARANGAY CLEARANCE                     │
│                                                     │
│  Clearance No.: {YYYY-MM-NNNN}   Date: {issued_at}  │
│  Validity: 6 months from date of issue              │
│                                                     │
├─────────────────────────────────────────────────────┤
│  TO WHOM IT MAY CONCERN:                           │
│                                                     │
│  This is to certify that {FULL NAME}, {age} years  │
│  old, born on {birthdate}, residing at {address},  │
│  Purok/Zone {purok_zone}, is a bona fide resident  │
│  of this barangay and has no derogatory record on  │
│  file in this office.                              │
│                                                     │
│  This clearance is issued upon the request of the  │
│  above-named person for the purpose of:            │
│                                                     │
│  {purpose / purpose_other}                         │
│                                                     │
├─────────────────────────────────────────────────────┤
│                                                     │
│            [QR CODE PLACEHOLDER - Phase 2]          │
│                                                     │
│  ___________________________                        │
│  {captain_name}                                    │
│  {captain_position}                                │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 10.3 PDF Specifications

- **Paper size:** A4 (595 x 842 pt)
- **Margins:** 50pt all sides
- **Font:** Helvetica (built-in PDFBox), 12pt body, 16pt heading
- **Color:** Black on white
- **Logo:** Max 80pt height, proportionally scaled; top-left aligned in header
- **Output:** Returned as `byte[]`, served via `ResponseEntity<byte[]>` with `Content-Type: application/pdf` and `Content-Disposition: attachment; filename="clearance-{number}.pdf"`
- **Generation trigger:** On-demand (not stored on disk); generated fresh each download

### 10.4 `ClearancePdfService` Interface

```java
public interface ClearancePdfService {
    byte[] generate(ClearanceRequest request, Resident resident, BarangaySettings settings);
}
```

---

## 11. Payment Module Design

### 11.1 Idempotency Design

#### Problem

Without idempotency, a network timeout or browser double-submit can cause:
- Two payment gateway calls for the same clearance (double charge in Phase 2)
- Two `Payment` rows if the DB constraint is not enforced transactionally
- `clearance.payment_status` toggling to PAID twice, creating duplicate audit log entries

#### Solution: Client-Supplied Idempotency Key

The client sends a `UUID v4` in the `Idempotency-Key` request header. The server:
1. Checks if a `Payment` row with the same `(idempotency_key, initiated_by_user_id)` already exists and has not expired.
2. If found and **PENDING**: returns `409 Conflict` — original request still in flight.
3. If found and **SUCCESS / FAILED**: returns `200 OK` with the cached `response_body` — no new gateway call.
4. If not found (or expired): proceeds with payment, stores the result in `response_body`.

#### Idempotency Flow Diagram

```
Client                            PaymentService                   DB / Gateway
  │                                     │                              │
  │  POST /clearances/{id}/payments     │                              │
  │  Idempotency-Key: <uuid>            │                              │
  │─────────────────────────────────►   │                              │
  │                                     │  SELECT * FROM payments      │
  │                                     │  WHERE idempotency_key = ?   │
  │                                     │  AND initiated_by = ?        │
  │                                     │  AND expires_at > NOW()      │
  │                                     │──────────────────────────►   │
  │                                     │                              │
  │                          ┌──────────┴──────────┐                  │
  │                    NOT   │                      │  FOUND           │
  │                  FOUND   │                      │                  │
  │                          │                      │                  │
  │              (new request)          │       (duplicate)            │
  │                          │          ├── status=PENDING             │
  │                          │          │   return 409 Conflict        │
  │                          │          │                              │
  │                          │          └── status=SUCCESS/FAILED      │
  │                          │              return 200 + cached body   │
  │                          │                                         │
  │          (new request path)         │                              │
  │                          │  INSERT payments (PENDING)              │
  │                          │──────────────────────────►              │
  │                          │                                         │
  │                          │  gateway.initiate(...)                  │
  │                          │──────────────────────────►  [GATEWAY]   │
  │                          │◄──────────────────────────              │
  │                          │  result: SUCCESS / FAILED               │
  │                          │                                         │
  │                          │  UPDATE payments SET                    │
  │                          │    status = result.status,              │
  │                          │    response_body = {...},               │
  │                          │    provider_ref = result.ref            │
  │                          │──────────────────────────►              │
  │                          │                                         │
  │◄─────────────────────────│  200 OK + PaymentDTO                    │
```

#### Idempotency Key Rules

| Rule | Detail |
|------|--------|
| Format | UUID v4, provided by client |
| Header name | `Idempotency-Key` |
| Scope | Per `(idempotency_key, initiated_by_user_id)` — same key from two users is distinct |
| TTL | 24 hours from `idempotency_expires_at` |
| On expiry | Key may be reused; no cached response is replayed |
| Cash payments | Also idempotent — duplicate mark-as-paid returns the existing payment record |
| Missing header | Returns `400 Bad Request`: "Idempotency-Key header is required" |
| Invalid format | Returns `400 Bad Request`: "Idempotency-Key must be a valid UUID v4" |

---

### 11.2 Provider Abstraction

```java
public interface PaymentGateway {
    PaymentResult initiate(PaymentRequest request);
    String getProviderCode();
}

public record PaymentRequest(
    String clearanceRequestId,
    String idempotencyKey,     // forwarded to gateway for provider-level idempotency (Phase 2)
    BigDecimal amount,
    String currency,           // "PHP"
    String description
) {}

public record PaymentResult(
    String providerRef,
    PaymentStatus status,      // SUCCESS, FAILED, PENDING
    String message
) {}
```

---

### 11.3 `PaymentService` — Idempotency Logic

```java
@Service
@Transactional
public class PaymentService {

    public PaymentDTO initiatePayment(UUID clearanceRequestId,
                                      String idempotencyKey,
                                      UUID initiatedByUserId) {

        // 1. Validate idempotency key format
        validateUuidFormat(idempotencyKey);

        // 2. Check for existing non-expired payment with this key+user
        Optional<Payment> existing = paymentRepository
            .findByIdempotencyKeyAndInitiatedByUserIdAndExpiresAfter(
                idempotencyKey, initiatedByUserId, Instant.now()
            );

        if (existing.isPresent()) {
            Payment p = existing.get();
            if (p.getStatus() == PaymentStatus.PENDING) {
                throw new ConflictException(
                    "Payment with this Idempotency-Key is already in progress.");
            }
            // SUCCESS or FAILED — return cached response, no new gateway call
            return paymentMapper.toDto(p);
        }

        // 3. Load clearance + validate state guard
        ClearanceRequest clearance = clearanceRepository.findById(clearanceRequestId)
            .orElseThrow(() -> new NotFoundException("Clearance request not found"));

        if (clearance.getStatus() != ClearanceStatus.APPROVED) {
            throw new BadRequestException(
                "Payment can only be initiated for APPROVED clearances.");
        }
        if (clearance.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Clearance is already paid.");
        }

        // 4. Resolve fee
        FeeConfig fees = feeConfigRepository.findById(1).orElseThrow();
        BigDecimal amount = clearance.getUrgency() == Urgency.EXPRESS
            ? fees.getExpressFee() : fees.getRegularFee();

        // 5. Create PENDING payment row (idempotency anchor)
        Payment payment = Payment.builder()
            .clearanceRequestId(clearanceRequestId)
            .idempotencyKey(idempotencyKey)
            .amount(amount)
            .method(PaymentMethod.ONLINE)
            .status(PaymentStatus.PENDING)
            .provider(gateway.getProviderCode())
            .initiatedByUserId(initiatedByUserId)
            .idempotencyExpiresAt(Instant.now().plus(Duration.ofHours(24)))
            .build();
        paymentRepository.save(payment);    // flush so duplicate key constraint fires here

        // 6. Call gateway
        PaymentResult result;
        try {
            result = gateway.initiate(new PaymentRequest(
                clearanceRequestId.toString(), idempotencyKey, amount, "PHP",
                "Barangay Clearance – " + clearance.getPurpose()
            ));
        } catch (Exception e) {
            // Gateway error → mark FAILED, still cache the response
            result = new PaymentResult(null, PaymentStatus.FAILED, e.getMessage());
        }

        // 7. Update payment with result + cache response body
        payment.setStatus(result.status());
        payment.setProviderRef(result.providerRef());
        payment.setResponseBody(buildResponseBody(payment, result));
        paymentRepository.save(payment);

        // 8. Update clearance payment_status if SUCCESS
        if (result.status() == PaymentStatus.SUCCESS) {
            clearance.setPaymentStatus(ClearancePaymentStatus.PAID);
            clearanceRepository.save(clearance);
            auditService.log(AuditEvent.PAYMENT_SUCCESS, initiatedByUserId, payment.getId());
        }

        return paymentMapper.toDto(payment);
    }

    // Cash mark-as-paid — also idempotent
    public PaymentDTO markAsPaidCash(UUID clearanceRequestId, UUID clerkUserId) {
        ClearanceRequest clearance = clearanceRepository.findById(clearanceRequestId)
            .orElseThrow(() -> new NotFoundException("Clearance request not found"));

        // Idempotent: already PAID → return existing payment
        if (clearance.getPaymentStatus() == ClearancePaymentStatus.PAID) {
            return paymentMapper.toDto(
                paymentRepository.findByClearanceRequestId(clearanceRequestId)
                    .orElseThrow()
            );
        }

        Payment payment = Payment.builder()
            .clearanceRequestId(clearanceRequestId)
            .idempotencyKey(UUID.randomUUID().toString())  // system-generated for cash
            .amount(resolveAmount(clearance))
            .method(PaymentMethod.CASH)
            .status(PaymentStatus.SUCCESS)
            .provider("CASH")
            .initiatedByUserId(clerkUserId)
            .idempotencyExpiresAt(Instant.now().plus(Duration.ofHours(24)))
            .build();
        paymentRepository.save(payment);

        clearance.setPaymentStatus(ClearancePaymentStatus.PAID);
        clearanceRepository.save(clearance);
        auditService.log(AuditEvent.PAYMENT_CASH_RECORDED, clerkUserId, payment.getId());

        return paymentMapper.toDto(payment);
    }
}
```

---

### 11.4 Stub Gateway (MVP)

```java
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "stub", matchIfMissing = true)
public class StubPaymentGateway implements PaymentGateway {

    @Value("${payment.stub.always-success:true}")
    private boolean alwaysSuccess;

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        boolean success = alwaysSuccess || new Random().nextBoolean();
        return new PaymentResult(
            "STUB-" + UUID.randomUUID(),
            success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
            success ? "Stub payment succeeded" : "Stub payment failed"
        );
    }

    @Override
    public String getProviderCode() { return "STUB"; }
}
```

---

### 11.5 API Contract (updated)

**POST /api/v1/clearances/{id}/payments**

```
Required headers:
  Authorization: Bearer <access_token>
  Idempotency-Key: <uuid-v4>          ← NEW: required

Request body:
  (none — amount and method are resolved server-side)

Responses:
  201 Created       — new payment initiated successfully
  200 OK            — duplicate key, returning cached response (idempotent replay)
  400 Bad Request   — missing or malformed Idempotency-Key
  409 Conflict      — same key exists with status PENDING (in-flight)
  409 Conflict      — clearance already PAID
  422 Unprocessable — clearance not in APPROVED state
```

```json
// Response body (201 or 200)
{
  "id": "uuid",
  "clearanceRequestId": "uuid",
  "amount": 50.00,
  "method": "ONLINE",
  "status": "SUCCESS",
  "provider": "STUB",
  "providerRef": "STUB-abc123",
  "idempotent": false,        // true when this is a replayed cached response
  "createdAt": "2025-02-24T08:00:00Z"
}
```

---

### 11.6 Fee Resolution

```java
// At payment initiation, resolve fee from active fee_config
FeeConfig fees = feeConfigRepository.findById(1).orElseThrow();
BigDecimal amount = clearance.getUrgency() == Urgency.EXPRESS
    ? fees.getExpressFee()
    : fees.getRegularFee();
```

---

## 12. Frontend UI/UX Specification

### 12.1 Technology Stack

| Concern | Technology |
|---------|-----------|
| Framework | Next.js 14 (App Router) |
| UI components | shadcn/ui |
| Styling | Tailwind CSS |
| Data fetching | TanStack Query v5 |
| Forms | React Hook Form + Zod |
| HTTP client | Axios with interceptor for token refresh |
| Icons | Lucide React |
| Date formatting | date-fns |

### 12.2 Resident Portal Pages

#### `/portal/dashboard` — My Requests

- Header: "My Clearance Requests" + "New Request" button
- List of requests as cards (mobile-first)
- Each card: status badge, purpose, urgency, submitted date, clearance number (or "Pending")
- Status badge colors: FOR_APPROVAL=yellow, APPROVED=blue, REJECTED=red, RELEASED=green
- Empty state: "You have no clearance requests. Click 'New Request' to get started."

#### `/portal/requests/new` — Submit Request

- Form fields:
  - **Purpose** (required): Select dropdown [Employment, Travel, Loan/Business/Licensing, School/Police Clearance, Other]
  - **Specify purpose** (conditional, required when Other selected): Text input
  - **Urgency** (required): Radio buttons [Regular — ₱{fee}, Express — ₱{fee}]
  - **Number of copies** (required): Number input, min 1, max 10
  - **Additional notes** (optional): Textarea
- Submit button: "Submit Request"
- Cancel link: back to dashboard

#### `/portal/requests/[id]` — Request Detail

- Status timeline (vertical stepper):
  1. Submitted ✓ (date)
  2. Under Review (current if FOR_APPROVAL)
  3. Approved / Rejected (with rejection reason if REJECTED)
  4. Payment (PAID badge or "Pay Now" button)
  5. Released (with "Download PDF" button)
- Clearance reference number (shown as "Pending" until released)
- Purpose, urgency, copies, notes display
- If REJECTED: rejection reason highlighted in red + "Edit & Resubmit" button
- If APPROVED + UNPAID: "Pay Now" button (calls stub) + fee amount shown
- If RELEASED: "Download PDF" button (primary, prominent)

---

### 12.3 Back-office Pages

#### `/backoffice/dashboard`

- Summary stat cards: Pending Review (count), Approved Awaiting Payment (count), Released Today (count)
- Recent activity table: last 10 requests across all statuses

#### `/backoffice/residents`

- Search bar (top): filters by name, purok, address on input (debounced 300ms)
- Table: Last Name, First Name, Purok/Zone, Address, Contact, Portal Account (Yes/No), Actions
- "New Resident" button → `/backoffice/residents/new`
- Pagination: 20 per page

#### `/backoffice/residents/[id]`

- Display all resident fields
- "Edit" button → inline edit mode
- Linked portal account status (if any): PENDING_VERIFICATION (with Activate/Reject buttons), ACTIVE, REJECTED

#### `/backoffice/clearances`

- Filter bar: Status dropdown, Payment Status dropdown, Date range picker, Urgency dropdown
- Table: Clearance No., Resident Name, Purpose, Urgency, Status badge, Payment badge, Submitted Date, Actions
- "New Request" button (for walk-in)
- Click row → `/backoffice/clearances/[id]`

#### `/backoffice/clearances/[id]`

- Request info panel: all fields
- Resident info panel: name, address, birthdate, purok
- Action buttons (shown conditionally based on role and status):
  - APPROVER sees: "Approve" / "Reject" (when FOR_APPROVAL)
  - CLERK sees: "Mark as Paid" (when APPROVED + UNPAID), "Release" (when APPROVED + PAID), "Download PDF" (when RELEASED)
- Audit trail section: chronological list of state changes

#### `/backoffice/admin/settings`

- Form: Barangay Name, Municipality, Province, Captain Name, Captain Position
- Logo upload: drag-and-drop + click, preview of current logo, file size/type validation
- Save button

#### `/backoffice/admin/settings/fees`

- Regular Fee (₱): number input
- Express Fee (₱): number input
- Save button

#### `/backoffice/admin/users`

- Table: Name, Email, Role, Status, Created Date, Actions (Deactivate)
- "Create User" button → modal or new page

---

### 12.4 Auth Flow (Frontend)

```
middleware.ts
  ├── /portal/* → require role=RESIDENT, redirect to /login if not authed
  ├── /backoffice/* → require role=CLERK|APPROVER|ADMIN, redirect to /login if not authed
  └── /login, /register → redirect to home if already authed

api.ts (Axios instance)
  ├── Request interceptor: attach Authorization: Bearer {accessToken}
  └── Response interceptor: on 401, call /auth/refresh, retry original request;
      if refresh fails → clear tokens, redirect to /login
```

---

## 13. Security Design

### 13.1 JWT Flow

```
1. POST /api/v1/auth/login
   → Server validates credentials
   → Issues: accessToken (JWT, 15 min) + refreshToken (opaque UUID, 7 days)
   → refreshToken hash stored in refresh_tokens table
   → Client stores: accessToken in memory, refreshToken in HttpOnly cookie

2. Every API request
   → Client sends: Authorization: Bearer {accessToken}
   → JwtAuthFilter validates signature + expiry
   → Sets SecurityContext with userId + role

3. Token refresh (transparent to user)
   → Client sends: POST /auth/refresh with refreshToken cookie
   → Server validates hash in DB, checks not revoked, checks expiry
   → Issues new accessToken (15 min)

4. Logout
   → POST /auth/logout
   → Server marks refresh token as revoked=true in DB
   → Client clears tokens from memory + cookie
```

### 13.2 Role-Endpoint Access Matrix

| Endpoint | RESIDENT | CLERK | APPROVER | ADMIN |
|----------|----------|-------|----------|-------|
| POST /auth/register | Public | Public | Public | Public |
| POST /auth/login | Public | Public | Public | Public |
| GET /me/clearances | ✓ | — | — | — |
| POST /me/clearances | ✓ | — | — | — |
| POST /me/clearances/{id}/pay | ✓ | — | — | — |
| GET /me/clearances/{id}/pdf | ✓ | — | — | — |
| GET /residents | — | ✓ | — | ✓ |
| POST /residents | — | ✓ | — | ✓ |
| GET /clearances | — | ✓ | ✓ | ✓ |
| POST /clearances/{id}/approve | — | — | ✓ | ✓ |
| POST /clearances/{id}/reject | — | — | ✓ | ✓ |
| POST /clearances/{id}/mark-paid | — | ✓ | — | ✓ |
| POST /clearances/{id}/release | — | ✓ | — | ✓ |
| GET /clearances/{id}/pdf | — | ✓ | — | ✓ |
| GET /reports/clearances | — | ✓ | — | ✓ |
| GET/PUT /settings | — | — | — | ✓ |
| GET/POST /admin/users | — | — | — | ✓ |

### 13.3 Spring Security Configuration

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
            .requestMatchers("/api/v1/me/**").hasRole("RESIDENT")
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers(POST, "/api/v1/clearances/*/approve").hasAnyRole("APPROVER","ADMIN")
            .requestMatchers(POST, "/api/v1/clearances/*/reject").hasAnyRole("APPROVER","ADMIN")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

---

## 14. Testing Strategy

### 14.1 Approach (Solo Developer, 1–2 months)

Focus on high-value tests. Avoid over-testing boilerplate.

**Priority order:**
1. Service layer unit tests (business logic)
2. API integration tests (Spring MVC Test)
3. PDF generation smoke test
4. Manual E2E testing via Swagger UI and browser

### 14.2 Unit Tests

Test the core business logic in service classes:

| Test Class | Coverage Target |
|-----------|----------------|
| `AuthServiceTest` | Registration, login, refresh, logout |
| `ClearanceServiceTest` | State transitions, guard validation |
| `ClearanceNumberServiceTest` | Sequential number generation, monthly reset |
| `PaymentServiceTest` | Stub invocation, cash payment, fee resolution |
| `ClearancePdfServiceTest` | PDF generation non-null, contains expected text |

Tools: JUnit 5, Mockito, AssertJ

### 14.3 Integration Tests

Use `@SpringBootTest` with H2 in-memory database (test profile):

| Test Class | Coverage Target |
|-----------|----------------|
| `AuthControllerIT` | Full login/register/refresh flow |
| `ClearanceControllerIT` | Full workflow: submit → approve → pay → release |
| `ResidentControllerIT` | CRUD + search |
| `SettingsControllerIT` | Update settings, upload logo |

Tools: Spring MVC Test (`MockMvc`), H2, `@WithMockUser`

### 14.4 Example Test Structure

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClearanceWorkflowIT {

    @Test
    void fullWorkflow_submitApprovePayRelease() throws Exception {
        // 1. Register + activate resident
        // 2. Submit clearance request
        // 3. Approve as APPROVER
        // 4. Mark as paid as CLERK
        // 5. Release as CLERK
        // 6. Assert clearance number assigned, status=RELEASED
        // 7. Download PDF — assert 200 and content-type=application/pdf
    }
}
```

### 14.5 Test Configuration (`application-test.yml`)

Tests run against a real PostgreSQL instance spun up by **Testcontainers**. No H2 or in-memory database is used — this ensures test queries (JSONB, `ON CONFLICT`, `gen_random_uuid()`) are identical to production.

**Maven dependency:**
```xml
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

```yaml
# application-test.yml
spring:
  jpa:
    show-sql: false
payment:
  stub:
    always-success: true
```

```java
// Base class for all integration tests
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

---

## 15. Deployment Architecture

### 15.1 Docker Compose

PostgreSQL is used in **all environments** — local dev, testing, and production. There is no H2 dependency anywhere in the project.

#### Local Development — `docker-compose.dev.yml`

Run PostgreSQL locally for backend development. The Spring Boot app and Next.js frontend run directly on the host (`./mvnw spring-boot:run` and `npm run dev`).

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: barangay_clearance
      POSTGRES_USER: barangay_user
      POSTGRES_PASSWORD: devpassword
    ports:
      - "5432:5432"          # exposed to host for local app + pgAdmin
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U barangay_user"]
      interval: 5s
      retries: 5

volumes:
  postgres_dev_data:
```

```bash
# Start local DB
docker compose -f docker-compose.dev.yml up -d

# Run backend (connects to localhost:5432)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Run frontend
npm run dev
```

#### Production — `docker-compose.yml`

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: barangay_clearance
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    image: barangay-clearance-api:latest
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://postgres:5432/barangay_clearance
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      PAYMENT_PROVIDER: stub
    ports:
      - "8080:8080"

  frontend:
    image: barangay-clearance-web:latest
    restart: unless-stopped
    environment:
      NEXT_PUBLIC_API_URL: http://backend:8080/api/v1
    ports:
      - "3000:3000"

  nginx:
    image: nginx:alpine
    restart: unless-stopped
    depends_on:
      - backend
      - frontend
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./certs:/etc/nginx/certs:ro
    ports:
      - "80:80"
      - "443:443"

volumes:
  postgres_data:
```

### 15.2 Environment Variables (`.env`)

```bash
DB_USER=barangay_user
DB_PASSWORD=<strong_password>
JWT_SECRET=<256-bit-random-hex>
```

### 15.3 Nginx Configuration

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;
    ssl_certificate /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header Host $host;
    }
}

server {
    listen 80;
    return 301 https://$host$request_uri;
}
```

### 15.4 Flyway Migration Files

```
src/main/resources/db/migration/
  V1__initial_schema.sql        -- all CREATE TABLE statements
  V2__seed_settings.sql         -- INSERT barangay_settings, fee_config defaults
  V3__seed_admin.sql            -- INSERT initial admin user (bcrypt hash of temp password)
```

### 15.5 Build Process

```bash
# Backend
./mvnw clean package -DskipTests
docker build -t barangay-clearance-api:latest .

# Frontend
npm run build
docker build -t barangay-clearance-web:latest .

# Deploy
docker-compose --env-file .env up -d
```

---

## 16. Phase 2 Roadmap

| Feature | Deferred From MVP | Effort | Architecture Change |
|---------|-------------------|--------|---------------------|
| Email notifications (status change) | Yes | Low | None — add `@EventListener` on domain events |
| SMS notifications (Semaphore/Vonage) | Yes | Medium | None — add `NotificationService` impl |
| QR code on PDF for verification | Yes | Low | None — fill existing placeholder in PDF layout |
| Real payment gateway (PayMongo, Maya) | Yes | Medium | None — implement `PaymentGateway` interface |
| Payment webhook endpoint | Yes | Medium | None — add `/payments/webhook/{provider}` endpoint |
| Resident deduplication (name+birthdate) | Yes | Medium | None — add dedup batch job + clerk UI |
| CSV / Excel report export | Yes | Low | None — add export endpoint with StreamingResponseBody |
| Multi-tenant management UI | Yes | High | Minor — add `tenant_id` FK to all tables |
| DPA 2012 compliance module | Yes | High | Minor — add consent fields, data access log |
| Offline / PWA support | Yes | Medium | None — add next-pwa plugin |
| Analytics dashboard | Yes | Medium | None — add aggregation queries + chart UI |
| Mobile app | Yes | High | None — API is already REST; build React Native app |

### 16.1 Domain Events (prepared for Phase 2)

The following Spring application events are published in MVP code but have no listeners in Phase 2. Adding a listener is the only change needed:

```java
// Published by ClearanceService
new ClearanceStatusChangedEvent(requestId, previousStatus, newStatus, residentUserId)

// Published by PaymentService
new PaymentCompletedEvent(paymentId, clearanceRequestId, status)

// Published by AuthService
new UserRegisteredEvent(userId, email)
```

Phase 2 notification listener example (zero changes to existing code):
```java
@Component
public class EmailNotificationListener {
    @EventListener
    public void onStatusChanged(ClearanceStatusChangedEvent event) {
        // send email
    }
}
```

---

## 17. Technical Decisions Log (ADRs)

### ADR-001 — Modular Monolith over Microservices

**Status:** Accepted
**Date:** 2026-02-24

**Context:** Solo developer, 1–2 month timeline, single barangay, ~100 requests/day maximum.

**Decision:** Single Spring Boot application with package-level module separation.

**Consequences:**
- (+) Single deployment, simple Docker Compose, no network overhead between modules.
- (+) Package boundaries still enforce separation; microservice extraction feasible later.
- (−) Cannot scale modules independently. Acceptable at this scale.

**Rejected alternatives:** Microservices (over-engineering), separate Maven modules (build complexity without sufficient benefit for solo MVP).

---

### ADR-002 — PostgreSQL (Docker) in All Environments, No H2

**Status:** Accepted

**Context:** Relational data with FK relationships. Audit logs benefit from JSONB. Clearance number sequence requires atomic `ON CONFLICT DO UPDATE`. An earlier design used H2 with `MODE=PostgreSQL` for local dev and tests.

**Decision:** PostgreSQL 16 via Docker in **all environments** — local dev (`docker-compose.dev.yml`), integration tests (Testcontainers), and production (`docker-compose.yml`). H2 is not used anywhere.

**Consequences:**
- (+) Single database dialect across all environments — no compatibility surprises between H2 mode and real PostgreSQL behaviour.
- (+) JSONB, `gen_random_uuid()`, `ON CONFLICT DO UPDATE`, and GIN indexes work identically in dev, test, and prod.
- (+) Integration tests catch real PostgreSQL-specific issues (e.g., constraint names, case-sensitive identifiers) before they reach production.
- (+) Testcontainers spins up a real `postgres:16-alpine` container per test run — no persistent state between test suites.
- (−) Integration tests require Docker to be running. Acceptable — Docker is a prerequisite for the project anyway.
- (−) Local dev requires `docker compose -f docker-compose.dev.yml up -d` before running the backend. One extra step, documented in the README.

---

### ADR-003 — Flyway over Liquibase

**Status:** Accepted

**Context:** Both tools are supported by Spring Boot. Project needs versioned migrations.

**Decision:** Flyway with plain `.sql` migration files.

**Consequences:**
- (+) SQL files are universally readable; no Flyway-specific DSL.
- (+) Simpler mental model (sequential versioned files) vs. Liquibase changeset IDs.
- (−) No built-in SQL rollback (Teams feature). Rollbacks handled by new forward migrations.

---

### ADR-004 — JWT with Refresh Tokens over Session Auth

**Status:** Accepted

**Context:** Separate Next.js frontend and Spring Boot API. Need stateless API for scalability.

**Decision:** JWT access token (15-min) + database-backed revocable refresh token (7-day).

**Consequences:**
- (+) Stateless access — no DB lookup on every request.
- (+) Logout supported by revoking refresh token in DB.
- (+) Short access token expiry limits blast radius of stolen token.
- (−) Stolen access token valid for up to 15 min after logout. Acceptable for this threat model.

---

### ADR-005 — Single Next.js App with Route Groups

**Status:** Accepted

**Context:** Two distinct UX personas (resident portal vs. back-office) with different layouts and access rules.

**Decision:** One Next.js app; `/portal/*` and `/backoffice/*` route groups; `middleware.ts` enforces role-based access.

**Consequences:**
- (+) Single codebase, single build, single Docker container.
- (+) Shared API client, auth logic, and types.
- (−) Bundle may grow over time. Mitigated by Next.js automatic per-route code splitting.

**Rejected:** Two separate Next.js apps (doubles build/deploy overhead for a solo developer).

---

### ADR-006 — Apache PDFBox over iText 7

**Status:** Accepted

**Context:** PDF clearance certificate generation required. Two main options: iText 7 and Apache PDFBox.

**Decision:** Apache PDFBox 3.x.

**Consequences:**
- (+) Apache License 2.0 — no commercial licensing cost. iText 7 is AGPL v3 which requires commercial license for proprietary deployments.
- (+) Coordinate-based layout control appropriate for fixed-format government document.
- (+) Image embedding for barangay logo.
- (−) More verbose API than iText's higher-level layout engine. Wrapped by `ClearancePdfService`.

---

### ADR-007 — shadcn/ui over MUI or Ant Design

**Status:** Accepted

**Context:** Frontend needs a consistent, accessible component library.

**Decision:** shadcn/ui + Tailwind CSS.

**Consequences:**
- (+) Full ownership of component source — no library lock-in.
- (+) Built on Radix UI primitives — WAI-ARIA accessible by default.
- (+) Only used components included in the bundle (design-level tree-shaking).
- (−) Components must be added per-component via CLI, not installed as a package. Non-issue for focused MVP.

**Rejected:** MUI (Material Design aesthetic doesn't fit government portal), Ant Design (similar reasons + large bundle).

---

### ADR-008 — Clearance Number Assigned at RELEASE

**Status:** Accepted

**Context:** Number could be assigned at submission, approval, or release.

**Decision:** Assign clearance number only when status changes to RELEASED.

**Consequences:**
- (+) No gaps in the sequence — every number corresponds to an actually issued document.
- (+) `issued_at` date on the document is always the date of physical release.
- (−) Resident sees "Pending" for clearance number until released. The UUID serves as tracking ID in earlier states. Communicated clearly in the portal UI.

---

### ADR-009 — Logo Stored as BYTEA in PostgreSQL

**Status:** Accepted

**Context:** Barangay logo needed in PDF header. Storage options: filesystem, object storage (S3/MinIO), or database BYTEA.

**Decision:** Store as `BYTEA` in `barangay_settings`.

**Consequences:**
- (+) Zero additional infrastructure. Backed up automatically with the database.
- (+) Atomic with settings updates (no separate backup needed for logo file).
- (+) Appropriate for size (typical logos 10–200 KB).
- (−) Logo served through API, not static file server. Acceptable — logo is rarely fetched (only settings page + PDF generation).

**Rejected:** Filesystem (not portable across container restarts without volume config; separate backup needed), MinIO (adds 4th Docker service and significant config complexity for a single small file).

---

### ADR-010 — Fee-at-Time-of-Payment Resolution

**Status:** Accepted

**Context:** Admin can change fees at any time. Should a clearance request use the fee active at submission time or at payment time?

**Decision:** Fee is resolved from `fee_config` at the time `POST /clearances/{id}/payments` is called (payment initiation).

**Consequences:**
- (+) Simpler implementation — no need to snapshot the fee at submission time.
- (+) Admin fee changes take effect immediately for all unpaid requests.
- (−) A resident might submit at ₱50 and pay at ₱100 if admin raised the fee in between. Acceptable for a barangay context where fee changes are infrequent and announced. Can be addressed in Phase 2 by snapshotting the fee at submission.

---

### ADR-011 — Client-Supplied Idempotency Key for Payment Initiation

**Status:** Accepted

**Context:** The `POST /clearances/{id}/payments` endpoint initiates a payment via an external gateway (stub in MVP, real provider in Phase 2). Two duplicate-request scenarios must be handled safely:
1. **Network timeout / client retry** — the client doesn't know if the first request succeeded and retries.
2. **Browser double-submit** — the resident clicks "Pay Now" twice in quick succession.

Without idempotency control, either scenario can result in:
- Duplicate `Payment` rows in the database (violating the one-payment-per-clearance invariant)
- Double charges when a real payment gateway is integrated
- Inconsistent `clearance.payment_status` if the second request races with the first

**Decision:** Implement client-supplied idempotency keys scoped per user.

- Client generates a UUID v4 before each payment attempt and sends it as the `Idempotency-Key` header.
- The server stores `(idempotency_key, initiated_by_user_id)` in the `payments` table with a 24-hour TTL.
- On duplicate receipt: PENDING → 409; SUCCESS/FAILED → 200 with cached response body.
- Cash payments (clerk mark-as-paid) are inherently idempotent via the `clearance.payment_status = PAID` guard.

**Consequences:**
- (+) Eliminates double-charge risk when real gateways are integrated in Phase 2 — the `idempotency_key` is also forwarded to the gateway as a provider-level idempotency token (PayMongo and Maya both support this).
- (+) Safe client retries — the frontend can retry on network errors without fear of side effects.
- (+) The `response_body JSONB` column serves as a complete audit trail of what was returned to the client, making disputes easy to resolve.
- (+) Scope per user prevents one user's key from interfering with another user's payment.
- (−) Client must generate and persist a UUID v4 before submitting the payment request. This is handled by the frontend in a single line (`crypto.randomUUID()`) and is standard practice.
- (−) A scheduled job (or Postgres TTL via `pg_cron`) should periodically purge expired idempotency keys to prevent unbounded table growth. For MVP: a weekly `DELETE FROM payments WHERE idempotency_expires_at < NOW() AND status != 'PENDING'` suffices.

**Alternatives considered:**
- Server-generated idempotency key (rejected — the client cannot safely retry without knowing the key; the key must be generated before the first attempt).
- Database `UNIQUE` constraint on `clearance_request_id` only (rejected — this already exists as the one-payment-per-clearance guard, but it fires too late: it prevents saving a second row but doesn't distinguish a duplicate from a new legitimate retry after a FAILED payment).
- Distributed lock (Redis, DB advisory lock) during payment processing (rejected — adds infrastructure complexity; the key-based approach is simpler and stateless).

---

*End of Document — Version 1.0.0 — Ready for Development*
