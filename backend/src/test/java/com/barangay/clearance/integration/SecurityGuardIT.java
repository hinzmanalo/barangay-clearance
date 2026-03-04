package com.barangay.clearance.integration;

import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.residents.entity.Resident;
import com.barangay.clearance.residents.repository.ResidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests verifying role-based access control (RBAC) guards.
 *
 * <p>
 * Covers unauthorised action attempts (wrong role, no token) and confirms that
 * legitimate roles CAN access the resources they should.
 * </p>
 */
class SecurityGuardIT extends BaseIntegrationTest {

    @Autowired
    private ResidentRepository residentRepository;

    /** Clearance seeded in FOR_APPROVAL state for approve-attempt tests. */
    private UUID forApprovalClearanceId;

    /** UUID of the RESIDENT user seeded for ownership tests (tests 6–8). */
    private UUID residentUserId;

    @BeforeEach
    void setUp() throws Exception {
        truncateAllTables();
        // Seed the fixed ADMIN, CLERK, APPROVER staff users so FK constraints
        // don't fail when creating clearances via their tokens
        seedStaffUsers();

        // Seed a RESIDENT user + linked resident profile for ownership tests
        User residentUser = userRepository.save(User.builder()
                .email("guard.resident@test.internal")
                .passwordHash(passwordEncoder.encode("Password1!"))
                .firstName("Guard")
                .lastName("Resident")
                .role(User.Role.RESIDENT)
                .status(User.UserStatus.ACTIVE)
                .mustChangePassword(false)
                .build());
        residentUserId = residentUser.getId();

        // Link a resident profile to the user so portal endpoints can resolve it
        residentRepository.save(Resident.builder()
                .userId(residentUserId)
                .firstName("Guard")
                .lastName("Resident")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(Resident.Gender.MALE)
                .address("1 Security Lane, Barangay Guard")
                .status(Resident.ResidentStatus.ACTIVE)
                .build());

        // Create a walk-in clearance in FOR_APPROVAL state for approve-attempt tests
        forApprovalClearanceId = createForApprovalClearance();
    }

    // ── Test 1: RESIDENT tries to approve → 403 ──────────────────────────────

    /**
     * A RESIDENT cannot approve clearances — the approve endpoint requires
     * APPROVER or ADMIN.
     */
    @Test
    void approve_asResident_returns403() throws Exception {
        performPost("/api/v1/clearances/" + forApprovalClearanceId + "/approve",
                null, asResident(residentUserId))
                .andExpect(status().isForbidden());
    }

    // ── Test 2: CLERK tries to approve → 403 ─────────────────────────────────

    /**
     * A CLERK cannot approve clearances — approval requires APPROVER or ADMIN.
     */
    @Test
    void approve_asClerk_returns403() throws Exception {
        performPost("/api/v1/clearances/" + forApprovalClearanceId + "/approve",
                null, asClerk())
                .andExpect(status().isForbidden());
    }

    // ── Test 3: CLERK accesses settings → 403 ────────────────────────────────

    /**
     * A CLERK cannot read barangay settings — settings require ADMIN.
     */
    @Test
    void getSettings_asClerk_returns403() throws Exception {
        performGet("/api/v1/settings", asClerk())
                .andExpect(status().isForbidden());
    }

    // ── Test 4: APPROVER accesses settings → 403 ─────────────────────────────

    /**
     * An APPROVER cannot read barangay settings — settings require ADMIN.
     */
    @Test
    void getSettings_asApprover_returns403() throws Exception {
        performGet("/api/v1/settings", asApprover())
                .andExpect(status().isForbidden());
    }

    // ── Test 5: No token on residents list → 401 ─────────────────────────────

    /**
     * Accessing a protected endpoint without any token returns 401.
     */
    @Test
    void listResidents_noToken_returns401() throws Exception {
        performGet("/api/v1/residents", null)
                .andExpect(status().isUnauthorized());
    }

    // ── Test 6: RESIDENT lists own clearances → 200 ──────────────────────────

    /**
     * A RESIDENT with a linked resident profile can list their own clearances
     * (empty list is fine — 200 is the important assertion).
     */
    @Test
    void listMyClearances_asResident_returns200() throws Exception {
        performGet("/api/v1/me/clearances", asResident(residentUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── Test 7: RESIDENT accesses another resident's clearance → 404 ─────────

    /**
     * A RESIDENT cannot view a clearance that belongs to a different resident.
     * Returns 404 (not found under their resident profile).
     */
    @Test
    void getMyClearance_ownedByAnotherResident_returns404() throws Exception {
        // forApprovalClearanceId is a walk-in clearance not linked to residentUserId
        performGet("/api/v1/me/clearances/" + forApprovalClearanceId,
                asResident(residentUserId))
                .andExpect(status().isNotFound());
    }

    // ── Test 8: RESIDENT accesses settings → 403 ─────────────────────────────

    /**
     * A RESIDENT has no access to barangay settings — requires ADMIN role.
     */
    @Test
    void getSettings_asResident_returns403() throws Exception {
        performGet("/api/v1/settings", asResident(residentUserId))
                .andExpect(status().isForbidden());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Creates a walk-in resident and a clearance in FOR_APPROVAL state using
     * backoffice endpoints. Returns the clearance UUID.
     */
    private UUID createForApprovalClearance() throws Exception {
        // Create a walk-in resident
        Map<String, Object> residentBody = Map.of(
                "firstName", "WalkIn",
                "lastName", "Resident",
                "birthDate", "1985-03-15",
                "gender", "MALE",
                "address", "2 WalkIn Street, Barangay Test");
        MvcResult residentResult = performPost("/api/v1/residents", residentBody, asClerk())
                .andExpect(status().isCreated())
                .andReturn();
        UUID walkInResidentId = UUID.fromString(
                objectMapper.readTree(residentResult.getResponse().getContentAsString())
                        .get("id").asText());

        // Create a walk-in clearance for that resident
        Map<String, Object> clearanceBody = Map.of(
                "residentId", walkInResidentId.toString(),
                "purpose", "EMPLOYMENT",
                "urgency", "STANDARD",
                "copies", 1);
        MvcResult clearanceResult = performPost("/api/v1/clearances", clearanceBody, asClerk())
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                objectMapper.readTree(clearanceResult.getResponse().getContentAsString())
                        .get("id").asText());
    }
}
