# Architecture Decision Records (ADR)

> **Project:** Barangay Clearance System
> **Format:** MADR (Markdown Architectural Decision Records)
> **Status definitions:** Proposed | Accepted | Deprecated | Superseded

---

## Index

| ID                                                                             | Title                                                      | Status   |
| ------------------------------------------------------------------------------ | ---------------------------------------------------------- | -------- |
| [ADR-001](#adr-001-modular-monolith-over-microservices)                        | Modular Monolith over Microservices                        | Accepted |
| [ADR-002](#adr-002-stateless-jwt-authentication)                               | Stateless JWT Authentication                               | Accepted |
| [ADR-003](#adr-003-refresh-token-non-rotation)                                 | Refresh Token Non-Rotation                                 | Accepted |
| [ADR-004](#adr-004-sha-256-hashing-of-refresh-tokens)                          | SHA-256 Hashing of Refresh Tokens                          | Accepted |
| [ADR-005](#adr-005-method-level-authorization-with-preauthorize)               | Method-Level Authorization with @PreAuthorize              | Accepted |
| [ADR-006](#adr-006-clearance-state-machine-enforced-in-service-layer)          | Clearance State Machine Enforced in Service Layer          | Accepted |
| [ADR-007](#adr-007-atomic-clearance-number-generation-via-postgresql)          | Atomic Clearance Number Generation via PostgreSQL          | Accepted |
| [ADR-008](#adr-008-singleton-tables-for-configuration)                         | Singleton Tables for Configuration                         | Accepted |
| [ADR-009](#adr-009-payment-gateway-strategy-pattern)                           | Payment Gateway Strategy Pattern                           | Accepted |
| [ADR-010](#adr-010-idempotent-payments-via-composite-unique-index)             | Idempotent Payments via Composite Unique Index             | Accepted |
| [ADR-011](#adr-011-resident-identity-from-jwt-not-request-parameters)          | Resident Identity from JWT, Not Request Parameters         | Accepted |
| [ADR-012](#adr-012-cross-module-denormalization-for-resident-name)             | Cross-Module Denormalization for Resident Name             | Accepted |
| [ADR-013](#adr-013-flyway-for-schema-migration)                                | Flyway for Schema Migration                                | Accepted |
| [ADR-014](#adr-014-mapstruct-for-entity-dto-mapping)                           | MapStruct for Entity-DTO Mapping                           | Accepted |
| [ADR-015](#adr-015-postgresql-uuid-primary-keys)                               | PostgreSQL UUID Primary Keys                               | Accepted |
| [ADR-016](#adr-016-server-side-pdf-generation-with-pdfbox)                     | Server-Side PDF Generation with PDFBox                     | Accepted |
| [ADR-017](#adr-017-profile-driven-security-configuration)                      | Profile-Driven Security Configuration                      | Accepted |
| [ADR-018](#adr-018-resident-account-activation-workflow)                       | Resident Account Activation Workflow                       | Accepted |
| [ADR-019](#adr-019-cross-module-uuid-references-over-jpa-entity-relationships) | Cross-Module UUID References over JPA Entity Relationships | Accepted |

---

## ADR-001: Modular Monolith over Microservices

**Date:** 2025-01
**Status:** Accepted

### Context

The Barangay Clearance System needed an architectural pattern that balances simplicity, maintainability, and potential future scalability. The system has 6 bounded contexts: identity, residents, clearance, payments, PDF, and settings.

### Decision

Implement a **modular monolith** — a single deployable Spring Boot application with strict internal module boundaries. No cross-module JPA entity references. Each module owns its own controller, service, repository, entity, and DTO layers.

### Consequences

**Positive:**

- Single deployment artifact simplifies operations for a barangay with limited IT staff
- No distributed transaction complexity — all writes are within a single PostgreSQL instance
- Module boundaries allow a future microservices split without major refactoring
- Development and testing are significantly simpler than a distributed system

**Negative:**

- Entire application must be redeployed to update any single module
- All modules share the same JVM heap; a memory leak in one module affects all
- Horizontal scaling applies to the entire application, not individual hot modules

**Mitigation:**

- Module boundaries are strictly enforced by code convention (verified during code review)
- The system's expected load (dozens to hundreds of concurrent users per barangay) does not require per-module scaling

---

## ADR-002: Stateless JWT Authentication

**Date:** 2025-01
**Status:** Accepted

### Context

The system needs authentication for a web application serving both a resident portal and a back-office dashboard. Options considered:

1. **Server-side sessions** (HttpSession / Spring Session + Redis)
2. **Stateless JWT** (access token + refresh token)
3. **OAuth2 / OpenID Connect** (external identity provider)

### Decision

Use **stateless JWT** with HMAC-SHA256 signed access tokens (15-minute TTL) and opaque refresh tokens (7-day TTL) stored as hashes in PostgreSQL.

### Rationale

- **Sessions** require a shared session store (Redis) for horizontal scaling — added infrastructure complexity not justified by the expected load
- **OAuth2/OIDC** requires an external identity provider (Keycloak, Auth0) — operational overhead not appropriate for a barangay's IT capability
- **JWT** is stateless: the access token is validated by the `JwtAuthFilter` without any database hit, enabling future horizontal scaling without session affinity

### Consequences

**Positive:**

- No per-request database lookup for authentication
- Access tokens are self-contained — role and user ID are embedded in the token
- Horizontally scalable without a shared session store

**Negative:**

- Access tokens cannot be revoked until they expire (15 minutes maximum blast radius for a compromised token)
- Token validation logic must be kept consistent across services if the system is ever split

**Mitigation:**

- 15-minute TTL limits the compromise window to an acceptable range for the use case
- Refresh token revocation provides a mechanism to force re-authentication when needed (e.g., password change revokes all refresh tokens)

---

## ADR-003: Refresh Token Non-Rotation

**Date:** 2025-01
**Status:** Accepted

### Context

Refresh tokens can be rotated (issue a new refresh token on each refresh) or not. Rotation is more secure (stolen-token detection) but more complex.

### Decision

**Do not rotate** refresh tokens on use. The same refresh token is returned on every `/auth/refresh` call until it expires or is explicitly revoked.

### Rationale

- Rotation requires tracking the "previous" token to detect replay attacks (the refresh response must be atomic with the token invalidation) — complex to implement correctly
- For an MVP serving a small barangay, the security trade-off is acceptable
- Refresh tokens are opaque UUIDs with 7-day TTL — the window for a stolen refresh token to be exploited is bounded

### Consequences

**Positive:**

- Simpler implementation — no rotation state to track
- Fewer database writes per token refresh

**Negative:**

- A stolen refresh token can be used until it naturally expires (7 days) or the user changes their password
- No automatic detection of refresh token theft

**Future Path:**

- Implement token rotation with a "token family" approach (revoking all siblings on replay detection) when the security requirement warrants it

---

## ADR-004: SHA-256 Hashing of Refresh Tokens

**Date:** 2025-01
**Status:** Accepted

### Context

Refresh tokens must be stored server-side for revocation. Storing the raw token in the database means a database breach exposes all valid refresh tokens.

### Decision

Store only the **SHA-256 hex hash** of the refresh token in the `refresh_tokens.token_hash` column. Return the raw token to the client once. Subsequent lookups hash the client-provided token and compare against `token_hash`.

### Rationale

- SHA-256 is a one-way function — a database breach exposes only hashes, not valid tokens
- SHA-256 is deterministic and collision-resistant — hash comparison is reliable
- No key management required (unlike HMAC-based approaches for the refresh token itself)

### Consequences

**Positive:**

- Database breach does not expose usable refresh tokens
- No performance concern — SHA-256 computation is fast

**Negative:**

- Slight CPU overhead per refresh request (one SHA-256 computation)
- Loss of the raw token is permanent — no recovery mechanism

---

## ADR-005: Method-Level Authorization with @PreAuthorize

**Date:** 2025-01
**Status:** Accepted

### Context

Spring Security supports authorization at two levels:

1. **URL patterns** in `SecurityConfig` (`requestMatchers(...).hasRole(...)`)
2. **Method level** via `@PreAuthorize("hasRole(...)")` on controller methods

### Decision

Use `@PreAuthorize` at the **controller method level** as the primary authorization mechanism. URL-level security in `SecurityConfig` is limited to broad categories (authenticated vs. public).

### Rationale

- Method-level security co-locates the security rule with the business logic it protects — easier to audit
- URL-pattern matching is brittle when routes change — a renamed path easily bypasses a matcher
- `@PreAuthorize` supports SpEL expressions for complex conditions (e.g., `hasAnyRole('CLERK', 'ADMIN')`)
- AOP-based; works even when methods are called internally (unlike filter-based URL security)

### Consequences

**Positive:**

- Security rules are self-documenting — visible at the method site
- Resilient to URL refactoring
- Supports complex role expressions

**Negative:**

- Security rules are distributed across controller classes — no single place to audit all rules
- Requires `@EnableMethodSecurity` (enabled globally in `SecurityConfig`)

---

## ADR-006: Clearance State Machine Enforced in Service Layer

**Date:** 2025-01
**Status:** Accepted

### Context

The clearance workflow has strict state transitions (DRAFT → FOR_APPROVAL → APPROVED → RELEASED, etc.). This could be enforced at the database level (triggers), the service layer, or via a dedicated state machine library.

### Decision

Enforce state transitions in `ClearanceService` using **explicit guard checks** before every state change. No database triggers. No external state machine library.

### Rationale

- **Database triggers** are opaque, hard to test, and tightly couple business logic to the schema
- **State machine libraries** (Spring State Machine, etc.) add complexity and a learning curve for a relatively simple 5-state machine
- **Service-layer guards** are transparent, testable with unit tests, and co-located with the business logic

### Consequences

**Positive:**

- State transitions are explicit, readable, and unit-testable
- Business logic is entirely in Java — no database-specific code

**Negative:**

- Guards are code convention, not a framework guarantee — a future developer could bypass them with a direct repository call
- Must be duplicated for every transition entry point

**Mitigation:**

- Document the state machine clearly (see [ARCHITECTURE.md](ARCHITECTURE.md#8-clearance-state-machine))
- Code reviews must verify that no state changes bypass `ClearanceService`

---

## ADR-007: Atomic Clearance Number Generation via PostgreSQL

**Date:** 2025-01
**Status:** Accepted

### Context

Clearance numbers (`YYYY-MM-NNNN`) must be unique per month. Options considered:

1. **Application-level locking** (synchronized Java block or Redis distributed lock)
2. **PostgreSQL sequence** (global sequence — would require resetting per month)
3. **PostgreSQL `ON CONFLICT DO UPDATE RETURNING`** on a month-keyed sequence table

### Decision

Use a `clearance_number_sequence` table with `year_month` as the primary key and `last_seq` as an incrementing counter. Generate numbers via:

```sql
INSERT INTO clearance_number_sequence (year_month, last_seq)
VALUES (:yearMonth, 1)
ON CONFLICT (year_month)
DO UPDATE SET last_seq = clearance_number_sequence.last_seq + 1
RETURNING last_seq;
```

Execute in a `@Transactional(propagation = REQUIRES_NEW)` method to ensure the sequence commit is independent of the parent transaction.

### Rationale

- **`ON CONFLICT DO UPDATE RETURNING`** is atomic at the database level — no application-level locking needed
- PostgreSQL sequences cannot be per-month without manual reset; the custom table approach is more flexible
- `REQUIRES_NEW` ensures that even if the outer transaction rolls back (e.g., PDF generation fails), the sequence number is not "lost" and reused — it's incremented and consumed

### Consequences

**Positive:**

- Guaranteed unique clearance numbers under concurrent load
- No Redis or external coordination service required
- Self-healing — if the sequence row doesn't exist for a month, it's created atomically

**Negative:**

- `REQUIRES_NEW` creates a nested transaction — requires careful understanding of Spring transaction semantics
- Sequence numbers are never "recycled" — a failed release will leave a gap in the numbering

**Note on Gaps:**

- Gaps in clearance numbers are acceptable — they occur when a release operation fails after the sequence has been incremented but before the clearance record is updated. This is a known and documented trade-off.

---

## ADR-008: Singleton Tables for Configuration

**Date:** 2025-01
**Status:** Accepted

### Context

Barangay settings and fee configuration are application-wide singletons. Options:

1. **Key-value table** (`config_key`, `config_value`) — flexible but requires parsing
2. **Singleton row** in a typed table with a `CHECK (id = 1)` constraint
3. **External config file** — not editable at runtime

### Decision

Use **typed singleton tables** (`barangay_settings`, `fee_config`) with `CHECK (id = 1)` to enforce exactly one row at the database level. Update semantics: `PUT` (upsert) only — no `POST` or `DELETE`.

### Rationale

- Type safety — column types are defined in schema (NUMERIC for fees, BYTEA for logo)
- `CHECK (id = 1)` means the database itself prevents a second row — no application-level guard needed
- Easy to query and cache — a single `findById(1)` is the entire data access pattern
- No parsing of string config values required

### Consequences

**Positive:**

- Schema enforces data integrity at the database level
- Typed columns enable validation without application logic
- Simple repository access pattern

**Negative:**

- Adding a new config field requires a Flyway migration
- The `CHECK (id = 1)` pattern is unconventional — developers must understand the intent

---

## ADR-009: Payment Gateway Strategy Pattern

**Date:** 2025-01
**Status:** Accepted

### Context

The system needs a payment integration. For the MVP, barangays primarily collect cash. An online payment gateway (PayMongo or Maya) may be added later.

### Decision

Define `PaymentGateway` as a **Java interface**. Implement `StubPaymentGateway` as the initial (always-success) provider. Configure the active implementation via `payment.provider` property.

### Rationale

- **Strategy Pattern** decouples the payment logic from the gateway implementation
- The `StubPaymentGateway` enables full end-to-end testing of the payment and release workflow without real money
- Adding PayMongo or Maya requires only: implement `PaymentGateway`, add to Spring context with `@ConditionalOnProperty`, update configuration — no changes to `PaymentService`

### Consequences

**Positive:**

- Zero `PaymentService` changes required when adding a real gateway
- Full workflow testable from day one
- Clean separation of concerns

**Negative:**

- `StubPaymentGateway` must be clearly disabled in production to prevent test payments being treated as real
- Developers must remember to implement idempotency in any new gateway

---

## ADR-010: Idempotent Payments via Composite Unique Index

**Date:** 2025-01
**Status:** Accepted

### Context

Network failures and browser retries can cause duplicate payment requests. An idempotency key is a standard pattern to handle this.

### Decision

Require clients to provide an `Idempotency-Key: <UUID v4>` header. Enforce idempotency via a **composite unique index** on `payments (idempotency_key, initiated_by_user_id)` with a 24-hour TTL enforced in application logic.

### Rationale

- **Composite unique index** as the final guard means even a race condition between two concurrent requests is caught at the database level — one will fail with a unique constraint violation, which the service translates to a 409
- Scoping to `(key, userId)` allows different users to use the same key independently (no accidental cross-user conflicts)
- 24-hour TTL prevents keys from being "used up" forever, enabling retry within the retry window

### Consequences

**Positive:**

- Database constraint as final guard — no race condition possible
- Standard protocol — compliant with payment industry best practices
- Duplicate requests within 24 hours return cached responses

**Negative:**

- Clients must generate and manage idempotency keys (UUID v4)
- Key collision is theoretically possible (UUID v4 birthday problem) — negligible in practice

---

## ADR-011: Resident Identity from JWT, Not Request Parameters

**Date:** 2025-01
**Status:** Accepted

### Context

Portal (resident) endpoints need to ensure residents can only access their own data. The resident ID could come from:

1. A path parameter (`/api/v1/residents/{residentId}/clearances`)
2. A query parameter (`?residentId=...`)
3. Derived from the authenticated JWT

### Decision

For all portal endpoints, the resident's identity is **always resolved from the JWT** (`userId` claim → look up linked resident). Path and query parameters for resident ID are not used for scoping in portal routes.

### Rationale

- If the resident ID is taken from the URL, a malicious resident could substitute another resident's ID and access their data (horizontal privilege escalation / IDOR — Insecure Direct Object Reference)
- JWT claims are signed and cannot be tampered with by the client
- This is a well-established pattern for multi-tenant or user-scoped APIs

### Consequences

**Positive:**

- Eliminates IDOR vulnerabilities on portal endpoints by design
- Single source of truth for resident identity — the JWT

**Negative:**

- Slightly more complex service code (must resolve `userId` → `residentId` mapping)
- Portal endpoints cannot act on behalf of another resident (intentional)

---

## ADR-012: Cross-Module Denormalization for Resident Name

**Date:** 2025-01
**Status:** Accepted

### Context

`ClearanceRequestDTO` must include the resident's full name for display purposes. Options:

1. **JPA Join** (`@ManyToOne` from `ClearanceRequest` to `Resident`) — crosses module boundary
2. **API call** from clearance module to resident module — internal REST call in a monolith
3. **Denormalization** — post-fetch enrichment by calling `ResidentService` with a UUID

### Decision

**Denormalize** the resident name in `ClearanceRequestDTO` by fetching it post-query from `ResidentService` using `residentId`. No JPA relationship crosses the module boundary.

### Rationale

- A `@ManyToOne` JPA relationship from `ClearanceRequest` to `Resident` would create a compile-time coupling between modules that violates the modular monolith boundary
- Internal REST calls add latency and error handling complexity with no benefit in a monolith
- Post-fetch enrichment is a well-understood pattern — the `residentId` UUID is the cross-module reference

### Consequences

**Positive:**

- Module boundaries are preserved — clearance module has no JPA dependency on resident entities
- Future microservices split would only require replacing the direct `ResidentService` call with an HTTP call

**Negative:**

- N+1 query risk for list endpoints — mitigated by batching resident lookups where possible
- Resident name at the time of the query may differ from the name at the time of the request (name changes)

---

## ADR-013: Flyway for Schema Migration

**Date:** 2025-01
**Status:** Accepted

### Context

The database schema evolves over time. Schema management options:

1. **Hibernate `ddl-auto: create-drop`** — development convenience, destructive in production
2. **Hibernate `ddl-auto: update`** — dangerous in production (cannot drop columns)
3. **Flyway** — version-controlled, auditable migrations
4. **Liquibase** — similar to Flyway, XML/YAML format

### Decision

Use **Flyway** with `ddl-auto: validate` in all profiles except test. Flyway runs on startup and applies pending migrations automatically.

### Rationale

- `validate` ensures the JPA schema matches the database — catches drift immediately
- Flyway migrations are versioned SQL files — reviewable in pull requests, auditable in `flyway_schema_history` table
- Flyway is simpler to configure than Liquibase for a PostgreSQL-only project
- Spring Boot auto-configuration makes Flyway nearly zero-config

### Consequences

**Positive:**

- Reproducible schema — any environment can be brought to any version
- Migration history is stored in the database (`flyway_schema_history`)
- Rollback is explicit (write a new migration) — no accidental reversions

**Negative:**

- Every schema change requires a new migration file
- Migrations are irreversible by default — accidental migrations in production require a compensating migration

---

## ADR-014: MapStruct for Entity-DTO Mapping

**Date:** 2025-01
**Status:** Accepted

### Context

Entity-to-DTO conversion is repetitive boilerplate. Options:

1. **Manual mapping** — verbose, error-prone
2. **ModelMapper** — reflection-based, runtime overhead, mapping errors discovered at runtime
3. **MapStruct** — annotation-processor-based, compile-time safe, zero runtime overhead

### Decision

Use **MapStruct** with `@Mapper(componentModel = "spring")` for all entity↔DTO conversions.

### Rationale

- Compile-time generation means mapping errors (wrong field name, type mismatch) are caught at build time, not at runtime
- No reflection overhead — generated code is plain Java method calls
- Spring component model means mappers are injectable as `@Autowired` beans
- Works cleanly with Lombok-generated getters/setters (requires `lombok-mapstruct-binding` for correct annotation processor order)

### Consequences

**Positive:**

- Type-safe mapping with compile-time error detection
- No runtime performance overhead

**Negative:**

- Requires `./mvnw clean compile` after every entity or DTO change
- `lombok-mapstruct-binding` must be correctly ordered in `annotationProcessorPaths` — easy to misconfigure

---

## ADR-015: PostgreSQL UUID Primary Keys

**Date:** 2025-01
**Status:** Accepted

### Context

Primary key strategy options:

1. **Auto-increment INTEGER/BIGINT** — sequential, predictable, enumerable
2. **UUID v4** — random, non-guessable, globally unique

### Decision

Use **UUID v4** (`gen_random_uuid()`) as primary keys for all tables except singletons (`barangay_settings`, `fee_config`) and the sequence table (`clearance_number_sequence`).

### Rationale

- UUIDs are non-guessable — a resident cannot enumerate other residents' clearances by incrementing an ID in the URL
- UUIDs are globally unique — enables future data merging across barangay instances without key conflicts
- PostgreSQL's `gen_random_uuid()` is built-in (requires `pgcrypto` extension for UUID generation)

### Consequences

**Positive:**

- Non-enumerable — reduces surface area for IDOR attacks
- No coordination required for ID generation (no central sequence)
- Safe to expose in URLs

**Negative:**

- UUIDs are larger than integers (16 bytes vs 4/8 bytes) — slightly larger indexes
- UUID v4 is random — index insertions cause B-tree page splits more frequently than sequential IDs (at the scale of a barangay, this is negligible)

---

## ADR-016: Server-Side PDF Generation with PDFBox

**Date:** 2025-01
**Status:** Accepted

### Context

Clearance PDFs can be generated:

1. **Server-side** (Java: iTextPDF, PDFBox, JasperReports)
2. **Client-side** (JavaScript: jsPDF, Puppeteer/Headless Chrome)
3. **Template engine** (Thymeleaf → HTML → Puppeteer/wkhtmltopdf)

### Decision

Use **Apache PDFBox 3.x** for server-side PDF generation in `ClearancePdfServiceImpl`.

### Rationale

- **Server-side** ensures PDF output is consistent regardless of browser or OS — no rendering discrepancies
- **PDFBox** is pure Java, open-source (Apache License), and has no native dependencies — compatible with any JVM environment
- **iTextPDF** has complex licensing (AGPL for open-source use; commercial license required)
- **JasperReports** is heavyweight for a simple document layout
- **Puppeteer/Headless Chrome** requires Chrome installation in the server environment — not suitable for a barangay's constrained infrastructure

### Consequences

**Positive:**

- Consistent output across all environments
- No licensing concerns (Apache License 2.0)
- No external dependencies (no Chrome, no native libs)

**Negative:**

- PDF layout is programmatic (no visual template designer) — changes to the layout require code changes
- PDFBox API is lower-level than JasperReports — more boilerplate for layout

---

## ADR-017: Profile-Driven Security Configuration

**Date:** 2025-01
**Status:** Accepted

### Context

Frontend developers need to work against the API without managing JWT tokens. The production environment requires full JWT enforcement.

### Decision

Use **Spring profiles** to switch security configurations:

- `@Profile("!no-auth")` → `SecurityConfig` — JWT authentication enabled (default)
- `@Profile("no-auth")` → `LocalSecurityConfig` — all requests permitted, no JWT validation

### Rationale

- Spring's profile mechanism is a clean, Spring-idiomatic way to swap configurations
- Avoids conditional logic inside `SecurityConfig` (simpler, more readable)
- The `no-auth` profile can never be accidentally activated in production if the `prod` profile is required

### Consequences

**Positive:**

- Frontend development is frictionless — no token management needed
- Security configuration is explicit and clearly differentiated by profile
- Production cannot accidentally run with security disabled (profile name is not `prod`)

**Negative:**

- Two `SecurityConfig` classes must be kept in sync for non-security settings (CORS, CSRF, etc.)
- Developers must remember to test with auth enabled before submitting changes

---

## ADR-018: Resident Account Activation Workflow

**Date:** 2025-01
**Status:** Accepted

### Context

New resident registrations via the portal need a vetting step to prevent fake or fraudulent accounts from submitting clearance requests. Options:

1. **Email verification** — resident clicks a link in their email to activate
2. **Staff activation** — a clerk or admin manually activates the account after identity verification
3. **Automatic activation** — all registrations are immediately active

### Decision

Use **staff activation** (`PENDING_VERIFICATION` → `ACTIVE`). New registrations create an account with status `PENDING_VERIFICATION`. A clerk or admin must explicitly activate the account via `POST /residents/users/{userId}/activate`.

### Rationale

- **Email verification** requires an email server and a token management system — infrastructure complexity not present in the MVP
- **Automatic activation** allows anyone to submit clearance requests without identity verification — inappropriate for official government documents
- **Staff activation** aligns with the physical barangay workflow: a resident visits the barangay hall, presents identification, and the clerk verifies their identity before granting portal access

### Consequences

**Positive:**

- Prevents fraudulent clearance requests from unverified identities
- No email infrastructure required for the MVP
- Matches the existing barangay operational process

**Negative:**

- Residents cannot immediately submit clearance requests after registration — they must wait for staff activation
- Adds a manual step to the onboarding process

**Future Path:**

- Email verification can be added as an alternative or complementary verification step when email infrastructure is available

---

## ADR-019: Cross-Module UUID References over JPA Entity Relationships

**Date:** 2026-02
**Status:** Accepted

### Context

Several modules need to reference data owned by another module. For example, `ClearanceRequest` (in the `clearance` module) must identify which `Resident` (in the `residents` module) the request belongs to. Two approaches were considered:

1. **JPA entity relationship** — declare a `@ManyToOne Resident resident` field, letting Hibernate manage the join
2. **UUID reference** — store only the foreign key (`UUID residentId`) and resolve data via a service call when needed

### Decision

Store only the **UUID foreign key** across module boundaries. Never declare a `@ManyToOne` (or any JPA association) that references an entity from a different module.

### Rationale

A JPA cross-module relationship creates an invisible compile-time and runtime coupling between modules:

- Hibernate eagerly or lazily joins across module tables, breaking the encapsulation boundary
- Any schema change in the `residents` table (e.g., a new column, a renamed column) risks breaking the `clearance` entity mapping
- The `clearance` module's persistence context begins managing `Resident` lifecycle, violating module ownership
- Unit testing the `clearance` module in isolation requires mocking or loading the `residents` schema

Storing a UUID keeps the dependency explicit and one-directional: the `clearance` module knows a resident ID exists; it calls `ResidentService.getById(residentId)` only when it actually needs resident data, making the cross-module call visible and intentional.

### Consequences

**Positive:**

- Modules remain independently testable and reasonably portable
- Schema changes in one module do not cascade into another module's entity mappings
- Cross-module data access is explicit and auditable — a service call rather than a transparent Hibernate join
- Consistent with the modular monolith constraint established in ADR-001

**Negative:**

- No JPA lazy-loading or JPQL joins across modules — data from two modules must be assembled in the service or response-mapping layer
- Referential integrity between modules is not enforced by Hibernate; it relies on application logic and database-level foreign key constraints in migrations

**Mitigation:**

- Database-level `FOREIGN KEY` constraints in Flyway migrations preserve referential integrity at the persistence layer
- Service-layer composition (e.g., `ClearanceResponseMapper` enriching a response with resident name) handles multi-module data assembly in one place
