# Integration Tests Hanging Fix — Root Cause & Resolution

**Date:** March 4, 2026

---

## Issue — Integration tests hang when running as a suite

### Symptom

Running all integration tests together with `./mvnw -Dtest='*IT' test` would hang or fail intermittently, while individual IT classes passed in isolation.

### Root Cause

The integration test base class used a shared Testcontainers PostgreSQL field with class-level lifecycle semantics that could be stopped/restarted between test classes, while Spring test context caching could keep datasource state from an earlier class initialization.

This created an inconsistent runtime state in suite execution:

- The datasource could still reference a previously mapped host port.
- A subsequent class could run after container lifecycle changes.
- JDBC connections then targeted a stale container endpoint and produced connection-refused behavior.
- Combined with cross-class execution order and cleanup timing, this presented as test hangs/timeouts in full-suite runs.

### Resolution

`BaseIntegrationTest` was updated to make container lifecycle and datasource binding deterministic for the full JVM test run:

1. Removed per-class JUnit container lifecycle annotations in favor of a JVM-lifetime singleton container startup.
2. Started the PostgreSQL container once in a static initializer.
3. Added `@DynamicPropertySource` to bind datasource properties directly from the running container at runtime:
   - `spring.datasource.url`
   - `spring.datasource.username`
   - `spring.datasource.password`
   - `spring.datasource.driver-class-name`
4. Kept existing per-test cleanup and test-pool tuning to reduce lock/pool contention side effects.

### Why this works

With one container instance per test JVM and dynamic property binding from that live instance, all IT classes resolve the same valid PostgreSQL endpoint for the duration of the suite. This removes stale host-port drift and stabilizes multi-class integration execution.

---

## Related Note — FK-dependent test setup

A separate but related suite instability was previously addressed by reseeding staff users after truncation for tests that persist staff UUIDs into FK-constrained columns.

Reference: `integration-test-fk-fix.md`.

---

## Verification

After applying the container lifecycle + dynamic datasource binding changes, running the IT suite from the backend module completed successfully:

- Command: `./mvnw -Dtest='*IT' test`
- Exit code: `0`
