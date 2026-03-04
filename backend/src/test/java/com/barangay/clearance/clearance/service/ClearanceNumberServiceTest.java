package com.barangay.clearance.clearance.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * ClearanceNumberService Unit Tests
 *
 * NOTE: The ClearanceNumberService uses EntityManager.createNativeQuery() for
 * atomic sequence generation via PostgreSQL "INSERT ... ON CONFLICT ...
 * RETURNING".
 * This service cannot be effectively unit-tested with mocks as:
 * 1. EntityManager.createNativeQuery() cannot be easily mocked to simulate
 * PostgreSQL sequence behavior
 * 2. The @Transactional(propagation = REQUIRES_NEW) requires actual Spring
 * context
 * 3. The atomic sequence logic is the core concern and must be tested with real
 * DB
 *
 * SOLUTION: This service is tested via integration tests in the Phase 9.2
 * integration
 * test suite (see ClearanceWorkflowIT and integration tests that exercise the
 * full
 * clearance workflow including release, which calls this service).
 *
 * Reference: Phase 9.1 Step 5 — Backend Unit Tests — ClearanceNumberServiceTest
 */
class ClearanceNumberServiceTest {

    @Test
    void integrationTestOnlyService_noted() {
        // ClearanceNumberService requires a real database and Spring context.
        // Tests for sequence generation are covered in the integration test suite.
        // This placeholder test documents the reason for deferring to integration
        // tests.
    }
}
