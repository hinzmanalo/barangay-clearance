# Deployment Issues Summary

**Date:** March 5, 2026

This document consolidates all deployment-related issues encountered during the development of the Barangay Clearance System, including root causes, resolutions, and lessons learned.

---

## Overview

Six major issues were encountered during development that impacted deployment readiness and testing infrastructure:

1. **Docker API Version Mismatch** — Integration tests failing on macOS Docker Desktop 29.x
2. **Integration Test Foreign Key Constraints** — FK violations during test table truncation
3. **Integration Tests Hanging in Suite Execution** — Testcontainers lifecycle instability
4. **Refresh Token Rotation Missing** — Security issue affecting authentication flow
5. **Render Build Failure: `mvnw` Not Found** — Dockerfile referenced uncommitted Maven wrapper
6. **Render Environment Not Auto-Detecting Docker** — Render showed Build/Start Command fields instead of Docker mode

---

## Issue 1: Docker API Version Mismatch

### Date Identified

March 4, 2026

### Severity

**Critical** — Prevented all integration tests from running on macOS Docker Desktop 29.x

### Symptoms

- All integration tests (`*IT.java`) fail during Spring Test Context initialization
- Error: `IllegalStateException: Could not find a valid Docker environment`
- Testcontainers unable to bootstrap PostgreSQL container via `@ServiceConnection`
- `DockerClientProviderStrategy` exhausts all detection strategies without finding valid Docker

### Root Cause Analysis

Three interconnected problems:

1. **Testcontainers Version Too Old (1.20.1)**
   - Bundled shaded `docker-java` library version was outdated
   - Defaulted to Docker API `/v1.32`, incompatible with Docker Desktop 29.x

2. **Docker Desktop 29.x Strict API Enforcement**
   - macOS Docker Desktop 29.2.0+ enforces strict API version support
   - Only accepts `/v1.44+` (rejects `/v1.32` with HTTP 400 Bad Request)
   - Confirmed via direct curl testing:
     - `/v1.44/info` → HTTP 200 ✓
     - `/v1.32/info` → HTTP 400 ✗

3. **Incorrect Maven Surefire Configuration**
   - Old pom.xml forced TCP-based Docker connection: `DOCKER_HOST=tcp://localhost:2375`
   - System properties were not configured to properly override environment variables
   - Surefire configuration bypassed Testcontainers' normal Unix socket detection

### Resolution

**Three-part fix:**

1. **Upgraded Testcontainers BOM**

   ```xml
   <version>1.20.1</version> → <version>1.21.0</version>
   ```

   - Includes docker-java 3.4.2 (vs 3.3.x in 1.20.1)
   - Enables proper API version negotiation with Docker daemon

2. **Removed Forced TCP Configuration**
   - Deleted old Surefire env vars: `DOCKER_HOST=tcp://localhost:2375`, `DOCKER_API_VERSION=1.53`
   - Restored default behavior: Testcontainers uses Unix socket at `/var/run/docker.sock`

3. **Added Explicit Maven Surefire System Properties**

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-surefire-plugin</artifactId>
       <version>3.2.5</version>
       <configuration>
           <systemPropertyVariables>
               <DOCKER_HOST>unix:///var/run/docker.sock</DOCKER_HOST>
               <api.version>1.44</api.version>
           </systemPropertyVariables>
       </configuration>
   </plugin>
   ```

   - System properties override environment variables in docker-java's `DefaultDockerClientConfig`
   - `DOCKER_HOST` explicitly targets Unix socket
   - `api.version` hard-codes API `/v1.44` (compatible with Docker Desktop 29.x)

### Key Lesson

Docker environment configuration in containerized test frameworks depends on:

1. Understanding specific library versions and bundled dependencies (shading)
2. Knowing the host Docker daemon's API version constraints
3. Identifying exact property key names expected by underlying libraries (not just env-friendly aliases)

**Rule:** When Testcontainers fails to detect Docker on a new environment version, inspect the shaded docker-java bytecode to determine exact property keys and precedence order.

---

## Issue 2: Integration Test Foreign Key Constraint Violations

### Date Identified

March 4, 2026

### Severity

**High** — Multiple integration tests failing with DataIntegrityViolationException (500 Internal Server Error)

### Affected Tests

- `PaymentControllerIT.initiate_clearanceNotApproved_returns400` → 500
- `ClearanceWorkflowIT.happyPath_registerActivateSubmitApprovePayRelease` → 500

### Root Cause Analysis

The `truncateAllTables()` cleanup routine wiped the `users` table but did not recreate fixed staff user rows. Subsequent test operations that attempted to write staff UUIDs to FK-constrained columns failed:

**Affected FK Columns:**
| Table | Column | Constraint |
|-------|--------|-----------|
| `clearance_requests` | `requested_by` | `NOT NULL REFERENCES users(id)` |
| `clearance_requests` | `reviewed_by` | `REFERENCES users(id)` |
| `payments` | `initiated_by_user_id` | `NOT NULL REFERENCES users(id)` |
| `refresh_tokens` | `user_id` | `NOT NULL REFERENCES users(id) ON DELETE CASCADE` |
| `residents` | `user_id` | `REFERENCES users(id) ON DELETE SET NULL` |
| `audit_logs` | `user_id` | `REFERENCES users(id) ON DELETE SET NULL` |

### Detailed Issue Breakdown

**Issue 2a:** `PaymentControllerIT.initiate_clearanceNotApproved_returns400`

- Fixed `CLERK_ID` (`00000000-0000-0000-0000-000000000002`) was used as the `requested_by` value when creating a walk-in clearance
- After `truncateAllTables()`, this UUID no longer had a corresponding row in the `users` table
- FK violation thrown: `DataIntegrityViolationException` → 500 instead of expected 400

**Issue 2b:** `ClearanceWorkflowIT.happyPath_registerActivateSubmitApprovePayRelease`

- Test flow: Register → Activate → Submit → Approve → Pay → Release
- `/approve` call wrote `APPROVER_ID` to `clearance_requests.reviewed_by`
- `/mark-paid` call wrote `CLERK_ID` to `payments.initiated_by_user_id`
- Both writes failed due to missing FK rows → 500 errors

### Resolution

Added `seedStaffUsers()` helper method to `BaseIntegrationTest`:

```java
protected void seedStaffUsers() {
    // Re-insert fixed staff user IDs after truncateAllTables()
    // ADMIN_ID = 00000000-0000-0000-0000-000000000001
    // CLERK_ID = 00000000-0000-0000-0000-000000000002
    // APPROVER_ID = 00000000-0000-0000-0000-000000000003
    // These UUIDs are used by all JWT helpers and must exist in DB for FK integrity
}
```

**Updated Test Classes:**

- `PaymentControllerIT.setUp()` calls `seedStaffUsers()` after `truncateAllTables()`
- `ClearanceWorkflowIT.setUp()` calls `seedStaffUsers()` after `truncateAllTables()`

### Common Pattern

The JWT helpers (`asClerk()`, `asApprover()`, etc.) are **stateless** — they generate valid tokens using fixed UUIDs without needing DB rows. However, any **write operation** that persists one of those UUIDs into a column with a FK to `users(id)` requires the corresponding row to exist.

**Rule:** Any integration test that performs staff-initiated writes must call `seedStaffUsers()` after `truncateAllTables()` in its `@BeforeEach`.

---

## Issue 3: Integration Tests Hanging in Suite Execution

### Date Identified

March 4, 2026

### Severity

**High** — Integration test suite hangs or fails intermittently when running all tests together

### Symptoms

- Individual IT classes pass in isolation: `./mvnw test -Dtest=SomeIT`
- Full suite fails or hangs: `./mvnw -Dtest='*IT' test`
- Intermittent failures suggest timing/ordering dependency
- Cross-test execution context pollution

### Root Cause Analysis

Testcontainers PostgreSQL container lifecycle was not deterministic across multiple test classes:

1. **Class-level JUnit Container Lifecycle**
   - Container started/stopped per test class
   - Spring test context caching could retain stale datasource state from earlier class
   - Subsequent class could run after container port changes

2. **Spring DataSource Caching**
   - Spring cached datasource bean based on connection string
   - Container lifecycle changes created new host port mappings
   - Cached datasource still referenced old stale endpoint

3. **Suite Execution Order Issues**
   - Different test runners/IDEs execute classes in different orders
   - Inconsistent container lifecycle across order variations
   - Connection-refused errors appeared as test hangs/timeouts

### Resolution

Made container lifecycle and datasource binding deterministic:

1. **JVM-Lifetime Singleton Container**
   - Removed per-class JUnit container lifecycle annotations
   - Started PostgreSQL container once in static initializer
   - Container lives for entire JVM lifetime

2. **Dynamic Property Source Binding**
   - Added `@DynamicPropertySource` to rebind datasource properties at runtime:
     - `spring.datasource.url`
     - `spring.datasource.username`
     - `spring.datasource.password`
     - `spring.datasource.driver-class-name`
   - Properties bound directly from running container instance

3. **Per-Test Cleanup Retained**
   - Kept existing per-test database cleanup (truncation)
   - Kept test-pool tuning to reduce contention
   - Only changed container lifecycle, not test isolation

### Why This Works

- One container instance per test JVM
- Dynamic property binding from live container
- All IT classes resolve same valid PostgreSQL endpoint
- No stale host-port drift between classes
- Stabilized multi-class integration execution

### Verification

After applying the fix:

```bash
./mvnw -Dtest='*IT' test
# Exit code: 0 ✓
```

---

## Issue 4: Refresh Token Rotation Missing

### Date Identified

March 4, 2026

### Severity

**Critical** — Security vulnerability in authentication token handling

### Test Failure

`AuthControllerIT.refresh_rotatedToken_returns401` — Expected 401, got 200 OK

### Test Scenario

```
1. User logs in → receives refreshToken₁
2. First /refresh call with refreshToken₁ → 200 OK, new access token issued
3. Second /refresh call with refreshToken₁ (token reuse attempt)
   → Expected: 401 Unauthorized (token rotation enforced)
   → Actual: 200 OK ❌ (token reuse allowed - SECURITY ISSUE)
```

### Root Cause Analysis

`AuthService.refresh(RefreshRequest)` did not implement token rotation:

**Problems in Original Implementation:**

1. **No Token Revocation**
   - Old refresh token was never marked as revoked
   - Allowed unlimited reuse of consumed token
   - Violates OAuth 2.0 token rotation pattern

2. **Read-Only Transaction**

   ```java
   @Transactional(readOnly = true)  // ❌ Prevents database writes
   public TokenResponse refresh(RefreshRequest request) { ... }
   ```

   - Even if code attempted to revoke token, writes would fail
   - No new token issued to client

3. **No New Token Return**
   - Response did not include new refresh token
   - Client forced to reuse consumed token
   - Violates standard token rotation pattern

### Security Implications

- **Token Replay Attacks:** Compromised refresh token could be reused indefinitely
- **No Token Revocation:** Organization unable to invalidate old tokens
- **Session Hijacking:** Lost tokens remain valid forever
- **Compliance Violation:** OAuth 2.0 and best practices mandate token rotation

### Resolution

Implemented proper **token rotation** in `AuthService.refresh()`:

**Updated Implementation:**

```java
/**
 * Issue a new access token from a valid refresh token with rotation.
 * The old refresh token is revoked; a new one is generated and returned.
 */
@Transactional  // ✅ Write-capable
public TokenResponse refresh(RefreshRequest request) {
    String hash = jwtService.hashRefreshToken(request.getRefreshToken());

    RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));

    if (refreshToken.isRevoked()) {
        throw AppException.unauthorized("Refresh token has been revoked");
    }

    if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
        throw AppException.unauthorized("Refresh token has expired");
    }

    User user = userRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> AppException.unauthorized("User not found"));

    // ✅ Revoke the consumed token
    refreshToken.setRevoked(true);
    refreshTokenRepository.save(refreshToken);

    // ✅ Generate new tokens
    String newAccessToken = jwtService.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole(), user.isMustChangePassword());

    RefreshToken newRefreshToken = jwtService.generateRefreshToken(user.getId());
    refreshTokenRepository.save(newRefreshToken);

    logger.info("Token rotated for user: {}", user.getId());
    auditService.log(user.getId(), AuditAction.USER_TOKEN_REFRESHED,
                     "User", user.getId(), null);

    return TokenResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken.getToken())  // ✅ Return new token
            .tokenType("Bearer")
            .expiresIn(jwtService.getAccessTokenExpirySeconds())
            .build();
}
```

**Changes:**

1. Changed `@Transactional(readOnly = true)` to `@Transactional`
2. Added `refreshToken.setRevoked(true)` and `refreshTokenRepository.save(refreshToken)`
3. Generate new refresh token via `jwtService.generateRefreshToken()`
4. Include new refresh token in `TokenResponse`

### Verification

Test now passes:

```
AuthControllerIT.refresh_rotatedToken_returns401 ✓
1. First refresh → 200 OK (token rotated)
2. Second refresh with old token → 401 Unauthorized ✓
```

---

## Issue 5: Render Build Failure — `mvnw` Not Found

### Date Identified

March 5, 2026

### Severity

**High** — Blocked all Render deployments; backend could not build

### Symptoms

```
error: failed to solve: failed to compute cache key: failed to calculate checksum of ref
...: "/mvnw": not found
```

- Render Docker build failed immediately at the `COPY mvnw` instruction
- Build log shows the file does not exist in the cloned repo

### Root Cause Analysis

The original `backend/Dockerfile` was written assuming `mvnw` and `.mvn/` were present in the repo:

```dockerfile
# Original — broken on Render
FROM eclipse-temurin:21-jdk-alpine AS builder
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
```

However, both `.mvn/` and `mvnw` are listed in `.gitignore` and were never committed. The files exist locally from the initial `mvn wrapper:wrapper` scaffolding but are excluded from the repository. Render clones the repo fresh — the wrapper files are absent, causing the `COPY` instruction to fail.

### Resolution

Replaced the build stage base image with the official Maven Docker image (`maven:3.9-eclipse-temurin-21-alpine`), which ships `mvn` pre-installed. Removed all references to `mvnw` and `.mvn/`:

```dockerfile
# Fixed — uses Maven image directly
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q
```

### Key Lesson

Never reference `mvnw` or `.mvn/` in a Dockerfile if those files are gitignored. Always use the official Maven Docker image (`maven:3.9-eclipse-temurin-21-alpine`) for CI/CD builds where only committed files are available.

---

## Issue 6: Render Not Auto-Detecting Docker Environment

### Date Identified

March 5, 2026

### Severity

**Medium** — Caused confusion during initial Render setup; would have resulted in incorrect build configuration

### Symptoms

- Render's **New Web Service** wizard displayed **Build Command** and **Start Command** fields
- Expected behavior: Render detects `Dockerfile` and switches to Docker mode automatically
- Without correction, Render would attempt a native build using the wrong commands

### Root Cause Analysis

Render auto-detects the runtime by scanning the repo root for a `Dockerfile`. Since our `Dockerfile` lives at `backend/Dockerfile` (not at the repo root), Render's auto-detection found no `Dockerfile` and defaulted to native (non-Docker) mode.

Additionally, the **Root Directory** must be set to `backend` before Render re-scans — if the root directory field is filled in after the initial scan, the runtime may not re-evaluate.

### Resolution

Two complementary fixes were applied:

1. **Manual runtime override in Render dashboard**
   - On the new service creation page, change **Language / Runtime** to **Docker** explicitly
   - Set **Root Directory** to `backend`
   - Set **Dockerfile Path** to `./Dockerfile`

2. **Added `render.yaml` at repo root** (infrastructure-as-code)
   - Explicitly declares `runtime: docker`, `dockerfilePath: ./backend/Dockerfile`, and `dockerContext: ./backend`
   - Render detects this file automatically via **New → Blueprint**
   - Eliminates manual dashboard configuration entirely

```yaml
services:
  - type: web
    name: barangay-clearance-backend
    runtime: docker
    dockerfilePath: ./backend/Dockerfile
    dockerContext: ./backend
    branch: main
```

### Key Lesson

When the `Dockerfile` is not at the repo root, Render will not auto-detect Docker mode. Always use `render.yaml` to codify the build configuration — it prevents misconfiguration and makes the deployment reproducible.

---

## Deployment Infrastructure Issues

### Status

Phases 0–9 complete. The following code work is required before production deployment:

### Required Pre-Deployment Work

1. **Backend Dockerfile** (`backend/Dockerfile`) ✅ Complete
   - Multi-stage build: `maven:3.9-eclipse-temurin-21-alpine` → `eclipse-temurin:21-jre-alpine`
   - Uses `mvn` directly (no `mvnw` — see Issue 5)
   - Non-root user: `spring:spring`

2. **Frontend Dockerfile** (`frontend/Dockerfile`) ✅ Complete
   - `output: 'standalone'` added to `next.config.mjs`
   - Multi-stage build with Node.js
   - Non-root user: `nextjs:nextjs`

3. **`render.yaml`** ✅ Complete
   - Codifies Docker runtime, Dockerfile path, and build context
   - Prevents Render environment misconfiguration (see Issue 6)

4. **Environment Variable Configuration**
   - Create `backend/.env.example` — document all Render env vars
   - Create `frontend/.env.example` — document `NEXT_PUBLIC_API_URL`

5. **Application Configuration Updates**
   - `application-prod.yml` — finalize CORS and payment env-var wiring
   - `SecurityConfig.java` — make CORS allowed origins configurable via env var

6. **Database Deployment**
   - No new migrations needed
   - Flyway automatically runs all 9 existing migrations on first startup against Neon DB

### Deployment Targets

- **Backend:** Render (Spring Boot container)
- **Frontend:** Vercel (Next.js)
- **Database:** Neon (PostgreSQL)

### No Code Changes Required For

- Database schema
- Flyway migrations
- Core business logic

---

## Summary Table

| Issue                            | Date        | Severity | Status      | Resolution                                           |
| -------------------------------- | ----------- | -------- | ----------- | ---------------------------------------------------- |
| Docker API Version Mismatch      | Mar 4, 2026 | Critical | ✅ Resolved | Testcontainers 1.21.0 + System Properties            |
| FK Constraint Violations         | Mar 4, 2026 | High     | ✅ Resolved | `seedStaffUsers()` helper in BaseIntegrationTest     |
| Hanging Integration Tests        | Mar 4, 2026 | High     | ✅ Resolved | JVM-lifetime container + @DynamicPropertySource      |
| Refresh Token Rotation           | Mar 4, 2026 | Critical | ✅ Resolved | Token revocation + new token issue                   |
| Render: `mvnw` Not Found         | Mar 5, 2026 | High     | ✅ Resolved | Use `maven:3.9-eclipse-temurin-21-alpine` image      |
| Render: Docker Not Auto-Detected | Mar 5, 2026 | Medium   | ✅ Resolved | Added `render.yaml` with explicit Docker config      |
| Deployment Dockerfiles           | Mar 5, 2026 | Medium   | ✅ Resolved | `backend/Dockerfile` + `frontend/Dockerfile` created |
| Environment Configuration        | Phase 10    | Medium   | Pending     | Set env vars in Render + Vercel dashboards           |

---

## Lessons Learned

1. **Container Lifecycle Testing**
   - Multi-class test suite execution requires deterministic container lifecycle
   - JVM-lifetime singleton containers with dynamic property binding eliminate cross-test state pollution
   - Class-level container lifecycle causes host-port remapping issues

2. **Foreign Key Integrity in Tests**
   - Table truncation cleanup must respect FK constraints
   - Stateless JWT generation (no DB hit) is incompatible with write operations
   - Integration tests need fixtures for all referenced FK rows

3. **Token Rotation Security**
   - OAuth 2.0 and security best practices mandate token rotation
   - Old tokens must be revoked, not just issued new ones
   - Refresh token responses must include new token, not require token reuse

4. **Docker Compatibility Management**
   - API version agreements are strict in modern Docker Desktop
   - Environment variable property keys vary by library shading/versions
   - System property precedence must override environment variables
   - Test discovered through direct curl verification of API version support

5. **Dockerfile Portability**
   - Never reference `mvnw` or `.mvn/` in a Dockerfile if those files are gitignored
   - Use official language Docker images (`maven:3.9-eclipse-temurin-21-alpine`) for hermetic builds
   - The build context must be set to the module subdirectory (`backend/`), not the repo root

6. **Render Platform Configuration**
   - Render does not auto-detect Docker when the `Dockerfile` is in a subdirectory
   - Use `render.yaml` at the repo root to declare runtime, Dockerfile path, and build context as code
   - Infrastructure-as-code (IaC) eliminates manual dashboard missteps and makes deployments reproducible

---

## References

- [Docker API Version Mismatch RCA](./docker-api-version-mismatch.md)
- [Integration Test FK Fix RCA](./integration-test-fk-fix.md)
- [Integration Tests Hanging Fix RCA](./integration-test-hanging-fix.md)
- [Refresh Token Rotation Fix RCA](./refresh-token-rotation.md)
- [Phase 10 Deployment Documentation](./plans/barangay-clearance/phase-10-deployment.md)
- [Deployment Implementation Plan](./plans/barangay-clearance/deployment-implementation-plan.md)
- [Deployment Design — Free Tier](./plans/barangay-clearance/deployment-design-free-tier.md)
- [`render.yaml`](../render.yaml)
- [`backend/Dockerfile`](../backend/Dockerfile)
- [`frontend/Dockerfile`](../frontend/Dockerfile)
