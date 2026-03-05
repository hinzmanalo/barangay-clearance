# Docker API Version Mismatch — Root Cause & Resolution

**Date:** March 4, 2026

**Resolution Credit:** GPT 5.3 Codex (Claude models contributed to investigation but GPT 5.3 Codex provided the definitive diagnosis and fix)

---

## Issue — Testcontainers Integration Tests Fail on macOS Docker Desktop 29.x

### Symptoms

- All integration tests (`*IT.java`) fail immediately during Spring Test Context initialization
- Error: `IllegalStateException: Could not find a valid Docker environment`
- Affects: Testcontainers PostgreSQL container bootstrap via `@ServiceConnection`
- Logs show: `DockerClientProviderStrategy` exhausting all detection strategies without finding valid Docker

### Root Cause Analysis

**Three-part problem identified:**

1. **Testcontainers Version Too Old (1.20.1)**
   - Bundled shaded `docker-java` library version was outdated
   - Defaulted to Docker API `/v1.32`, which is no longer compatible with modern Docker Desktop

2. **Docker Desktop 29.x API Enforcement**
   - macOS Docker Desktop 29.2.0+ strictly enforces API version support
   - Only accepts `/v1.44+` (rejects `/v1.32` with HTTP 400)
   - Verified via direct `curl`: `/v1.44/info` → HTTP 200 ✓ | `/v1.32/info` → HTTP 400 ✗

3. **Maven Surefire Configuration Interference**
   - Old pom.xml forced TCP-based Docker connection: `DOCKER_HOST=tcp://localhost:2375`
   - Surefire env override bypassed Testcontainers' normal Unix socket detection
   - System properties were not configured to override docker-java's environment variable defaults

### Resolution

**Three-part fix implemented:**

1. **Upgraded Testcontainers BOM**

   ```xml
   <version>1.20.1</version> → <version>1.21.0</version>
   ```

   - Reason: 1.21.0 bundles updated `docker-java 3.4.2` (vs 3.3.x in 1.20.1)
   - Result: Testcontainers now attempts proper API version negotiation with Docker daemon

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

   - Reason: System properties override environment variables in docker-java's `DefaultDockerClientConfig`
   - `DOCKER_HOST` targets Unix socket explicitly
   - `api.version` hard-codes API `/1.44` (matches Docker Desktop 29.x capability)

### Why Claude Models Struggled, GPT 5.3 Codex Succeeded

**Claude's investigation approach:**

- Traced symptom → tested curl commands → verified Docker daemon was reachable
- Correctly identified API version mismatch via curl tests (`/v1.32` → 400, `/v1.44` → 200)
- Proposed solutions but did not pinpoint the exact property key (`api.version` vs `DOCKER_API_VERSION`)
- Lacked awareness of docker-java's shading behavior and property precedence order

**GPT 5.3 Codex's breakthrough:**

- Performed bytecode inspection on shaded docker-java libraries (Testcontainers 1.20.1 and 1.21.0)
- Discovered exact property key: `"api.version"` (not `DOCKER_API_VERSION`)
- Identified property precedence chain: System Properties > Environment Variables > Defaults
- Provided definitive pom.xml plugin configuration with correct system property variable names

### Key Lesson

Docker environment configuration in containerized test frameworks depends on understanding:

1. The specific library version and its bundled dependencies (shading)
2. The host Docker daemon's API version constraints
3. The precise property key names expected by the underlying library (not environment-friendly aliases)

**Rule:** When Testcontainers fails to detect Docker on a new environment version, inspect the shaded docker-java bytecode to determine exact property keys and precedence order.

---

## Validation

✅ Direct curl tests confirm Unix socket connectivity and `/v1.44` API support  
✅ pom.xml syntax validated and Maven compilation succeeds  
✅ All 6 integration test classes compile without errors  
✅ Post-fix: Integration tests now initialize Testcontainers container successfully (test assertions may still fail on application logic, but Docker initialization passes)
