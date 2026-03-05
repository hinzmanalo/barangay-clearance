package com.barangay.clearance.integration;

import com.barangay.clearance.payments.entity.Payment;
import com.barangay.clearance.payments.repository.PaymentRepository;
import com.barangay.clearance.residents.dto.CreateResidentRequest;
import com.barangay.clearance.residents.entity.Resident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for payment initiation and idempotency endpoints.
 *
 * <p>
 * Each test sets up the exact clearance state it needs via the staff
 * (CLERK + APPROVER) API endpoints. Walk-in residents are created where
 * resident owner access is not required.
 * </p>
 */
class PaymentControllerIT extends BaseIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        truncateAllTables();
        // Re-seed staff user rows so clearance_requests.requested_by FK is satisfied
        // when walk-in clearances are created with the fixed CLERK_ID.
        seedStaffUsers();
    }

    // ── Test 1: Initiate payment with fresh idempotency key ──────────────────

    /**
     * Initiating a payment on an APPROVED clearance with a fresh UUID key returns
     * 201 and {@code idempotent: false}.
     */
    @Test
    void initiate_freshKey_approvedClearance_returns201NotIdempotent() throws Exception {
        UUID clearanceId = createApprovedWalkInClearance();
        String key = UUID.randomUUID().toString();

        performPost("/api/v1/clearances/" + clearanceId + "/payments",
                null, asClerk(), key)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idempotent", is(false)));
    }

    // ── Test 2: Replay SUCCESS payment returns idempotent true ───────────────

    /**
     * Replaying the same idempotency key on a SUCCESS payment returns 200 and
     * {@code idempotent: true}.
     */
    @Test
    void initiate_replaySuccessPayment_returns200Idempotent() throws Exception {
        UUID clearanceId = createApprovedWalkInClearance();
        String key = UUID.randomUUID().toString();

        // First call — creates the payment (SUCCESS with stub gateway)
        performPost("/api/v1/clearances/" + clearanceId + "/payments",
                null, asClerk(), key)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idempotent", is(false)));

        // Second call with same key — must be served from cache
        performPost("/api/v1/clearances/" + clearanceId + "/payments",
                null, asClerk(), key)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotent", is(true)));
    }

    // ── Test 3: Replay PENDING payment returns 409 ───────────────────────────

    /**
     * If an active PENDING payment already exists for the given key and user,
     * a repeat request returns 409 Conflict.
     */
    @Test
    void initiate_replayPendingPayment_returns409() throws Exception {
        UUID clearanceId = createApprovedWalkInClearance();
        String key = UUID.randomUUID().toString();

        // Directly insert a PENDING payment within the idempotency TTL window
        Payment pending = Payment.builder()
                .clearanceRequestId(clearanceId)
                .amount(BigDecimal.valueOf(50.00))
                .idempotencyKey(key)
                .initiatedByUserId(CLERK_ID)
                .paymentMethod(Payment.PaymentMethod.STUB)
                .provider("STUB")
                .status(Payment.PaymentStatus.PENDING)
                .idempotencyExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        paymentRepository.saveAndFlush(pending);

        // Attempt to initiate payment with the same key → 409
        performPost("/api/v1/clearances/" + clearanceId + "/payments",
                null, asClerk(), key)
                .andExpect(status().isConflict());
    }

    // ── Test 4: Missing Idempotency-Key header returns 400 ───────────────────

    /**
     * Omitting the {@code Idempotency-Key} header returns 400 Bad Request
     * (Spring's {@code @RequestHeader} enforcement).
     */
    @Test
    void initiate_missingIdempotencyKeyHeader_returns400() throws Exception {
        UUID clearanceId = createApprovedWalkInClearance();

        // performPost(…, null token for idempotency key) — header omitted entirely
        performPost("/api/v1/clearances/" + clearanceId + "/payments",
                null, asClerk(), null)
                .andExpect(status().isBadRequest());
    }

    // ── Test 5: Payment on non-APPROVED clearance returns 400 ────────────────

    /**
     * Initiating payment on a clearance that is still in FOR_APPROVAL status
     * returns 400.
     */
    @Test
    void initiate_clearanceNotApproved_returns400() throws Exception {
        // Create a walk-in clearance but do NOT approve it
        UUID clearanceId = createForApprovalWalkInClearance();
        String key = UUID.randomUUID().toString();

        performPost("/api/v1/clearances/" + clearanceId + "/payments",
                null, asClerk(), key)
                .andExpect(status().isBadRequest());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Creates a walk-in resident + clearance and advances it to APPROVED state.
     *
     * @return the UUID of the approved clearance
     */
    private UUID createApprovedWalkInClearance() throws Exception {
        UUID clearanceId = createForApprovalWalkInClearance();

        performPost("/api/v1/clearances/" + clearanceId + "/approve", null, asApprover())
                .andExpect(status().isOk());

        return clearanceId;
    }

    /**
     * Creates a walk-in resident + clearance in FOR_APPROVAL state.
     *
     * @return the UUID of the new clearance
     */
    private UUID createForApprovalWalkInClearance() throws Exception {
        // Create a walk-in resident
        UUID residentId = createWalkInResident();

        // Create a walk-in clearance request (starts at FOR_APPROVAL directly)
        Map<String, Object> body = Map.of(
                "residentId", residentId.toString(),
                "purpose", "EMPLOYMENT",
                "urgency", "STANDARD",
                "copies", 1);

        MvcResult result = performPost("/api/v1/clearances", body, asClerk())
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("id").asText());
    }

    /**
     * Creates a minimal walk-in resident and returns its UUID.
     */
    private UUID createWalkInResident() throws Exception {
        CreateResidentRequest req = new CreateResidentRequest();
        req.setFirstName("PayTest");
        req.setLastName("Resident");
        req.setBirthDate(LocalDate.of(1990, 5, 20));
        req.setGender(Resident.Gender.MALE);
        req.setAddress("99 Pay Street, Barangay Test");

        MvcResult result = performPost("/api/v1/residents", req, asClerk())
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("id").asText());
    }
}
