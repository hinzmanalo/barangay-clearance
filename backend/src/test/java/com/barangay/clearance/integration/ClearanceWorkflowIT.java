package com.barangay.clearance.integration;

import com.barangay.clearance.clearance.dto.RejectRequest;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Purpose;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Urgency;
import com.barangay.clearance.identity.dto.LoginRequest;
import com.barangay.clearance.identity.dto.RegisterRequest;
import com.barangay.clearance.residents.entity.Resident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for the clearance request lifecycle.
 *
 * <h3>Happy path</h3>
 * 
 * <pre>
 * Register → Activate → Login → Submit → Approve → Mark-Paid → Release → Download PDF
 * </pre>
 *
 * <h3>Rejection path</h3>
 * 
 * <pre>
 * Register → Activate → Login → Submit → Reject → Resubmit → Reject without reason (400)
 * </pre>
 *
 * <p>
 * <strong>State machine note:</strong> The actual implementation skips DRAFT.
 * New requests start directly at {@code FOR_APPROVAL} when submitted via the
 * portal.
 * </p>
 */
class ClearanceWorkflowIT extends BaseIntegrationTest {

    private static final String RESIDENT_EMAIL = "resident.workflow@example.com";
    private static final String RESIDENT_PASSWORD = "Password1!";

    @BeforeEach
    void setUp() {
        truncateAllTables();
        // Re-seed staff rows so FK columns (reviewed_by, initiated_by_user_id)
        // on clearance_requests and payments are satisfied after truncation.
        seedStaffUsers();
    }

    // ── Happy path ─────────────────────────────────────────────────────────

    /**
     * Full happy-path walkthrough: register, activate, submit, approve, pay
     * (cash), release, and then download the clearance PDF.
     */
    @Test
    void happyPath_registerActivateSubmitApprovePayRelease() throws Exception {

        // Step 1 — Register a resident portal account
        performPost("/api/v1/auth/register", buildRegisterRequest(RESIDENT_EMAIL), null)
                .andExpect(status().isCreated());

        // Step 2 — Resolve the newly created user's ID from the DB
        UUID userId = userRepository.findByEmail(RESIDENT_EMAIL).orElseThrow().getId();

        // Step 3 — Admin activates the pending account
        performPost("/api/v1/residents/users/" + userId + "/activate", null, asAdmin())
                .andExpect(status().isNoContent());

        // Step 4 — Resident logs in and obtains an access token
        String residentAccessToken = loginAndGetAccessToken(RESIDENT_EMAIL, RESIDENT_PASSWORD);

        // Step 5 — Resident submits a clearance request via the portal
        MvcResult submitResult = performPost("/api/v1/me/clearances",
                buildClearanceBody(), "Bearer " + residentAccessToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FOR_APPROVAL")))
                .andReturn();

        UUID clearanceId = UUID.fromString(
                objectMapper.readTree(submitResult.getResponse().getContentAsString())
                        .get("id").asText());

        // Step 6 — Approver approves the request
        performPost("/api/v1/clearances/" + clearanceId + "/approve", null, asApprover())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Step 7 — Clerk records cash payment
        performPost("/api/v1/clearances/" + clearanceId + "/mark-paid", null, asClerk())
                .andExpect(status().isOk());

        // Verify payment status is PAID on the clearance
        performGet("/api/v1/clearances/" + clearanceId, asClerk())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus", is("PAID")));

        // Step 8 — Clerk releases the clearance (assigns clearance number)
        performPost("/api/v1/clearances/" + clearanceId + "/release", null, asClerk())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RELEASED")))
                .andExpect(jsonPath("$.clearanceNumber").isNotEmpty());

        // Step 9 — Staff downloads the PDF for the released clearance
        mockMvc.perform(get("/api/v1/clearances/" + clearanceId + "/pdf")
                .header(AUTHORIZATION, asClerk()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        containsString(MediaType.APPLICATION_PDF_VALUE)));
    }

    // ── Rejection path ──────────────────────────────────────────────────────

    /**
     * Rejection and resubmission path: submit, reject with reason, resubmit
     * (returns to FOR_APPROVAL), then verify blank reason returns 400.
     */
    @Test
    void rejectionPath_submitRejectResubmitThenBlankReasonReturns400() throws Exception {

        // Setup — Register and activate a resident
        final String email = "reject.path@example.com";
        performPost("/api/v1/auth/register", buildRegisterRequest(email), null)
                .andExpect(status().isCreated());

        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        performPost("/api/v1/residents/users/" + userId + "/activate", null, asAdmin())
                .andExpect(status().isNoContent());

        String residentToken = loginAndGetAccessToken(email, RESIDENT_PASSWORD);

        // Step 4 — Submit clearance
        MvcResult submitResult = performPost("/api/v1/me/clearances",
                buildClearanceBody(), "Bearer " + residentToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FOR_APPROVAL")))
                .andReturn();

        UUID clearanceId = UUID.fromString(
                objectMapper.readTree(submitResult.getResponse().getContentAsString())
                        .get("id").asText());

        // Step 5 — Approver rejects with a reason
        performPost("/api/v1/clearances/" + clearanceId + "/reject",
                new RejectRequest("Insufficient supporting documents"), asApprover())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        // Step 6 — Resident resubmits; state returns to FOR_APPROVAL
        performPut("/api/v1/me/clearances/" + clearanceId,
                buildClearanceBody(), "Bearer " + residentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FOR_APPROVAL")));

        // Step 7 — Blank reason must return 400 (bean validation)
        performPost("/api/v1/clearances/" + clearanceId + "/reject",
                new RejectRequest(""), asApprover())
                .andExpect(status().isBadRequest());
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Logs in and returns the raw access token without the "Bearer " prefix.
     */
    private String loginAndGetAccessToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = performPost("/api/v1/auth/login", req, null)
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    /**
     * Returns a map that Jackson will serialise to a valid clearance request body.
     */
    private Map<String, Object> buildClearanceBody() {
        return Map.of(
                "purpose", Purpose.EMPLOYMENT.name(),
                "urgency", Urgency.STANDARD.name(),
                "copies", 1);
    }

    /**
     * Builds a valid {@link RegisterRequest} for the given email.
     */
    private RegisterRequest buildRegisterRequest(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(RESIDENT_PASSWORD);
        req.setFirstName("Workflow");
        req.setLastName("Resident");
        req.setBirthDate(LocalDate.of(1988, 4, 10));
        req.setGender(Resident.Gender.FEMALE);
        req.setAddress("100 Workflow Lane, Barangay Integration");
        return req;
    }
}
