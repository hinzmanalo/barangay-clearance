package com.barangay.clearance.integration;

import com.barangay.clearance.identity.dto.RegisterRequest;
import com.barangay.clearance.residents.dto.CreateResidentRequest;
import com.barangay.clearance.residents.dto.UpdateResidentRequest;
import com.barangay.clearance.residents.entity.Resident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@code /api/v1/residents/**} endpoints.
 *
 * <p>
 * Tests exercise pagination, search, CRUD operations, and the
 * register→pending→activate/reject lifecycle for portal accounts.
 * </p>
 */
class ResidentControllerIT extends BaseIntegrationTest {

    private static final String BASE = "/api/v1/residents";

    @BeforeEach
    void setUp() {
        truncateAllTables();
    }

    // ── Test 1: List residents as CLERK ──────────────────────────────────────

    /**
     * CLERK may list residents and receive a paginated response.
     */
    @Test
    void listResidents_asClerk_returns200PaginatedResponse() throws Exception {
        // Seed one resident so the list is non-empty
        performPost(BASE, buildCreateRequest("Alice", "Reyes"), asClerk())
                .andExpect(status().isCreated());

        performGet(BASE, asClerk())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    // ── Test 2: Search residents ──────────────────────────────────────────────

    /**
     * Searching by a partial name only returns matching residents.
     */
    @Test
    void searchResidents_byName_returnsFilteredResults() throws Exception {
        performPost(BASE, buildCreateRequest("Carlos", "Mendoza"), asClerk())
                .andExpect(status().isCreated());
        performPost(BASE, buildCreateRequest("Rosa", "Santos"), asClerk())
                .andExpect(status().isCreated());

        // Search for "Carlos" — only one match expected
        performGet(BASE + "?q=Carlos", asClerk())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName", is("Carlos")));
    }

    // ── Test 3: Create resident as CLERK ─────────────────────────────────────

    /**
     * CLERK can create a walk-in resident and receives 201 with the created DTO.
     */
    @Test
    void createResident_asClerk_returns201() throws Exception {
        performPost(BASE, buildCreateRequest("Juan", "dela Cruz"), asClerk())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName", is("Juan")))
                .andExpect(jsonPath("$.lastName", is("dela Cruz")));
    }

    // ── Test 4: Get resident by ID ────────────────────────────────────────────

    /**
     * Fetching a resident by its UUID returns the correct record.
     */
    @Test
    void getResidentById_existingId_returns200() throws Exception {
        UUID residentId = createResidentAndGetId("Maria", "Garcia");

        performGet(BASE + "/" + residentId, asClerk())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(residentId.toString())))
                .andExpect(jsonPath("$.firstName", is("Maria")));
    }

    // ── Test 5: Update resident ───────────────────────────────────────────────

    /**
     * Updating a resident via PUT returns 200 with the updated values.
     */
    @Test
    void updateResident_validPayload_returns200() throws Exception {
        UUID residentId = createResidentAndGetId("Pedro", "Lim");

        UpdateResidentRequest update = new UpdateResidentRequest();
        update.setContactNumber("09171234567");

        performPut(BASE + "/" + residentId, update, asClerk())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactNumber", is("09171234567")));
    }

    // ── Test 6: List pending users as ADMIN ──────────────────────────────────

    /**
     * After a portal registration, the new account appears in the pending list.
     */
    @Test
    void listPendingUsers_afterRegistration_returnsPendingResident() throws Exception {
        // Register a portal account → creates PENDING_VERIFICATION user
        performPost("/api/v1/auth/register", buildRegisterRequest("pending@test.com"), null)
                .andExpect(status().isCreated());

        performGet(BASE + "/pending-users", asAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].userId").isNotEmpty());
    }

    // ── Test 7: Activate user as ADMIN ───────────────────────────────────────

    /**
     * Activating a pending portal account returns 204 No Content.
     */
    @Test
    void activateUser_pendingUser_returns204() throws Exception {
        performPost("/api/v1/auth/register", buildRegisterRequest("activate@test.com"), null)
                .andExpect(status().isCreated());

        UUID userId = userRepository.findByEmail("activate@test.com")
                .orElseThrow()
                .getId();

        performPost(BASE + "/users/" + userId + "/activate", null, asAdmin())
                .andExpect(status().isNoContent());
    }

    // ── Test 8: Reject user as ADMIN ─────────────────────────────────────────

    /**
     * Rejecting a pending portal account returns 204 No Content.
     */
    @Test
    void rejectUser_pendingUser_returns204() throws Exception {
        performPost("/api/v1/auth/register", buildRegisterRequest("reject@test.com"), null)
                .andExpect(status().isCreated());

        UUID userId = userRepository.findByEmail("reject@test.com")
                .orElseThrow()
                .getId();

        performPost(BASE + "/users/" + userId + "/reject", null, asAdmin())
                .andExpect(status().isNoContent());
    }

    // ── Test 9: List without token → 401 ─────────────────────────────────────

    /**
     * Requests without an Authorization header receive 401.
     */
    @Test
    void listResidents_noToken_returns401() throws Exception {
        performGet(BASE, null)
                .andExpect(status().isUnauthorized());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Creates a walk-in resident and returns its UUID.
     */
    private UUID createResidentAndGetId(String firstName, String lastName) throws Exception {
        MvcResult result = performPost(BASE, buildCreateRequest(firstName, lastName), asClerk())
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
        return UUID.fromString(id);
    }

    /**
     * Builds a minimal {@link CreateResidentRequest}.
     */
    private CreateResidentRequest buildCreateRequest(String firstName, String lastName) {
        CreateResidentRequest req = new CreateResidentRequest();
        req.setFirstName(firstName);
        req.setLastName(lastName);
        req.setBirthDate(LocalDate.of(1985, 6, 15));
        req.setGender(Resident.Gender.FEMALE);
        req.setAddress("456 Sample Road, Barangay Demo");
        return req;
    }

    /**
     * Builds a valid {@link RegisterRequest} for portal-account lifecycle tests.
     */
    private RegisterRequest buildRegisterRequest(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("Password1!");
        req.setFirstName("Portal");
        req.setLastName("TestUser");
        req.setBirthDate(LocalDate.of(1992, 3, 20));
        req.setGender(Resident.Gender.MALE);
        req.setAddress("789 Portal Ave, Barangay Sample");
        return req;
    }
}
