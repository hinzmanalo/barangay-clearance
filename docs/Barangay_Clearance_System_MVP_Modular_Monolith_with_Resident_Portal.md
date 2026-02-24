# Barangay Clearance System

## Modular Monolith MVP (Microservice-Ready Architecture) — with Resident Portal

---

# 🎯 MVP Goal

Digitize the end-to-end barangay clearance process:

Resident → Online Request → Review/Approval → Payment (Stub) → Release → Report

The system is built as a **modular monolith** so it can evolve into microservices later.

---

# 👥 User Roles

## Resident (Portal User)

- Register / login (basic)
- Create a clearance request
- Track request status
- View payment status and initiate stub payment
- Download/print **only when released** (or optional: view “release instructions”)

## CLERK / ADMIN

- Manage residents (registry)
- Review incoming requests (validate resident info)
- Create/modify clearance requests
- Initiate / override payments (if cashier flow)
- Print and release clearances
- Reports / logbook

## APPROVER (Captain / Secretary)

- Approve or reject clearance requests

---

# ✅ MVP Feature Scope (Updated)

## 1) Identity & Access (MVP)

- Login for **Resident / Clerk / Approver**
- Role-based access:
  - RESIDENT
  - CLERK
  - APPROVER
  - ADMIN (optional)
- Password hashing (BCrypt)
- Basic audit logging (who did what + timestamp)

> MVP simplification: Residents can self-register, but their account is linked to a resident profile and may require clerk verification (optional toggle).

---

## 2) Resident Registry (Back-office)

- CRUD resident profile
- Search/filter (name, purok/zone, address)
- Dedup warning (name + birthdate)
- Link resident profile to resident portal account

---

## 3) Resident Portal (Simple)

**Pages / Screens**

- Register (name, birthdate, address, contact, email/username, password)
- Login
- “My Requests” list
- “Request Clearance” form:
  - purpose (dropdown + Other)
  - optional notes
- Request detail page:
  - status timeline
  - payment status
  - reference number (clearance request no.)
  - if released: download PDF (optional for MVP) or show pickup instructions

**Portal rules**

- A resident can only see **their own** requests.
- Resident cannot approve/release.
- Printing is allowed only when:
  - status = RELEASED (or APPROVED + PAID if you prefer)

---

## 4) Clearance Issuance (Core)

Owns:

- Clearance request lifecycle
- Clearance numbering (YYYY-xxxxx)
- Approval workflow
- Printing (PDF)
- Release tracking

Workflow:
DRAFT → FOR_APPROVAL → APPROVED/REJECTED → RELEASED

Payment status:
UNPAID → PAID

---

## 5) Payments Module (Stubbed Provider for MVP)

Owns:

- Payment intent creation
- Payment status tracking
- Provider abstraction (stub now)

Payment statuses:

- PENDING
- SUCCESS
- FAILED

MVP behavior:

- Resident (portal) or Clerk (back-office) clicks “Pay”
- Stub provider simulates SUCCESS/FAILED
- On SUCCESS → clearance.payment_status = PAID

Designed to later plug:

- PayMongo
- Maya
- GCash
- Other providers

---

## 6) Reports / Logbook (Back-office)

- Issued clearances by date range
- Filters: status, purok, purpose, paid/unpaid
- Export CSV (optional)

---

## 7) Audit Module

- Logs key actions:
  - Resident registration / profile link
  - Request created/submitted
  - Approved/rejected/released
  - Payment changes

---

# 📦 MVP Modules (Clean Boundaries)

## 1️⃣ identity module

Owns: users/roles/auth
Tables: users

## 2️⃣ residents module

Owns: resident profile
Tables: residents

## 3️⃣ clearance module

Owns: requests, approval, release, printing
Tables: clearance_requests

## 4️⃣ payments module

Owns: payment intents, provider integration
Tables: payments

## 5️⃣ audit module

Owns: audit trail
Tables: audit_logs

---

# 🏗 Architecture Overview

## Modular Monolith (Single Deployable App)

Recommended stack:

- Spring Boot 3.x
- Spring Security
- JPA (Postgres/MySQL)
- Flyway/Liquibase migrations
- PDF generation (server-side)
- UI options: Angular, Shadcn UI
- Docker
- APIs documented via Swagger

Each backend module contains:

- controller/
- dto/
- entity/
- repository/
- service/

Controllers never access repositories directly.

---

# 🧭 System Diagram (Monolith now, Microservices later)

```mermaid
flowchart TB
  subgraph Clients
    RP[Resident Portal (Web/Mobile Web)]
    BO[Back-office (Clerk/Approver Web)]
  end

  RP --> API[Spring Boot Monolith API]
  BO --> API

  subgraph Monolith[Modular Monolith]
    API --> ID[identity module]
    API --> RES[residents module]
    API --> CLR[clearance module]
    API --> PAY[payments module]
    API --> AUD[audit module]

    CLR -->|resident profile lookup| RES
    CLR -->|payment status| PAY
    ID --> AUD
    RES --> AUD
    CLR --> AUD
    PAY --> AUD
  end

  subgraph DB[Single Database (schema-per-module)]
    T1[(users)]
    T2[(residents)]
    T3[(clearance_requests)]
    T4[(payments)]
    T5[(audit_logs)]
  end

  ID --> T1
  RES --> T2
  CLR --> T3
  PAY --> T4
  AUD --> T5

  PAY --> EXT[Stub Payment Provider]
```

---

# 🔁 Data Flow (End-to-End)

## Resident Portal flow

1. Resident registers/logs in
2. Resident submits clearance request → status FOR_APPROVAL, payment_status UNPAID
3. Resident tracks status updates
4. When approved, resident can initiate payment (stub)
5. If payment success → PAID
6. Clerk releases after validation → status RELEASED
7. Resident sees “RELEASED” and can download PDF (optional) or pickup instructions

## Back-office flow

1. Clerk reviews incoming requests (verify resident identity if needed)
2. Approver approves/rejects
3. Clerk prints and releases
4. Reports/logbook updated automatically

---

# 📡 API Endpoints (MVP)

## Auth

- POST /api/v1/auth/register (Resident)
- POST /api/v1/auth/login
- POST /api/v1/auth/logout (optional)

## Residents (Back-office)

- POST /api/v1/residents
- GET /api/v1/residents?query=
- PUT /api/v1/residents/{id}

## Resident Portal (Requests)

- POST /api/v1/me/clearances (create request for logged-in resident)
- GET /api/v1/me/clearances (list my requests)
- GET /api/v1/me/clearances/{id}

## Clearances (Back-office)

- POST /api/v1/clearances (create on behalf of resident)
- POST /api/v1/clearances/{id}/submit
- POST /api/v1/clearances/{id}/approve
- POST /api/v1/clearances/{id}/reject
- POST /api/v1/clearances/{id}/release
- GET /api/v1/clearances/{id}/print

## Payments

- POST /api/v1/clearances/{id}/payments (clerk or resident, depending on rules)
- POST /api/v1/payments/webhook/{provider} (Phase 2; keep placeholder)

## Reports

- GET /api/v1/reports/clearances?from=&to=

---

# 🧱 Database Strategy (Microservice-Ready)

- Single database (MVP)
- Schema-per-module or table prefixes
- No cross-module repository access
- Cross-module communication via interfaces
- Domain events prepared for future Kafka/RabbitMQ

---

# ✅ Design Principles Enforced

- Clear module boundaries
- Interface-based cross-module communication
- Event-driven ready (in-process now)
- Payment provider abstraction
- Schema isolation
- Transaction boundaries in application layer
- APIs documented via Swagger

---

# 📈 Phase 2 (Future Enhancements)

- Full KYC / ID upload and verification workflow
- Real payment gateway integration + webhooks
- QR code verification for printed documents
- SMS/Email notifications
- Multi-document types (Indigency, Residency, Business Permit, etc.)
- Analytics dashboard
- Mobile app (optional)

---

End of Document
