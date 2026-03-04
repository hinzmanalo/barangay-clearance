package com.barangay.clearance.integration;

import com.barangay.clearance.identity.dto.LoginRequest;
import com.barangay.clearance.identity.dto.RefreshRequest;
import com.barangay.clearance.identity.dto.RegisterRequest;
import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.residents.entity.Resident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@code /api/v1/auth/**} endpoints.
 *
 * <p>
 * Each test truncates all tables and operates on a known email/password pair.
 * Some tests seed an ACTIVE user directly via the repository to avoid the
 * register → activate dance for auth-flow tests.
 * </p>
 */
class AuthControllerIT extends BaseIntegrationTest {

    private static final String BASE = "/api/v1/auth";
    private static final String TEST_EMAIL = "auth.it@example.com";
    private static final String TEST_PASSWORD = "Password1!";

    @BeforeEach
    void setUp() {
        truncateAllTables();
    }

    // ── Test 1: Register with valid payload ──────────────────────────────────

    /**
     * Valid registration returns 201 with no body.
     */
    @Test
    void register_validPayload_returns201() throws Exception {
        performPost(BASE + "/register", buildRegisterRequest(TEST_EMAIL, TEST_PASSWORD), null)
                .andExpect(status().isCreated());
    }

    // ── Test 2: Login with seeded ACTIVE user ────────────────────────────────

    /**
     * Logging in with valid credentials returns 200 and a token pair.
     */
    @Test
    void login_activeUser_returns200WithTokens() throws Exception {
        seedActiveUser(TEST_EMAIL, TEST_PASSWORD);

        LoginRequest req = new LoginRequest();
        req.setEmail(TEST_EMAIL);
        req.setPassword(TEST_PASSWORD);

        performPost(BASE + "/login", req, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    // ── Test 3: Refresh with valid token ─────────────────────────────────────

    /**
     * Refreshing with a valid refresh token returns 200 and a new access token.
     */
    @Test
    void refresh_validToken_returns200WithNewAccessToken() throws Exception {
        seedActiveUser(TEST_EMAIL, TEST_PASSWORD);
        String refreshToken = loginAndGetRefreshToken(TEST_EMAIL, TEST_PASSWORD);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(refreshToken);

        performPost(BASE + "/refresh", req, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    // ── Test 4: Logout then refresh fails ────────────────────────────────────

    /**
     * After logout, reusing the revoked refresh token returns 401.
     */
    @Test
    void logout_thenRefresh_returns401() throws Exception {
        seedActiveUser(TEST_EMAIL, TEST_PASSWORD);
        String refreshToken = loginAndGetRefreshToken(TEST_EMAIL, TEST_PASSWORD);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(refreshToken);

        // Logout should succeed
        performPost(BASE + "/logout", req, null)
                .andExpect(status().isOk());

        // Subsequent refresh must fail
        performPost(BASE + "/refresh", req, null)
                .andExpect(status().isUnauthorized());
    }

    // ── Test 5: Register duplicate email → 409 ───────────────────────────────

    /**
     * Registering with an email that already exists returns 409.
     */
    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = buildRegisterRequest(TEST_EMAIL, TEST_PASSWORD);

        performPost(BASE + "/register", req, null)
                .andExpect(status().isCreated());

        performPost(BASE + "/register", req, null)
                .andExpect(status().isConflict());
    }

    // ── Test 6: Login wrong password → 401 ───────────────────────────────────

    /**
     * Login with a wrong password returns 401.
     */
    @Test
    void login_wrongPassword_returns401() throws Exception {
        seedActiveUser(TEST_EMAIL, TEST_PASSWORD);

        LoginRequest req = new LoginRequest();
        req.setEmail(TEST_EMAIL);
        req.setPassword("WrongPassword99!");

        performPost(BASE + "/login", req, null)
                .andExpect(status().isUnauthorized());
    }

    // ── Test 7: Refresh with rotated (already used) token → 401 ──────────────

    /**
     * Once a refresh token has been consumed (rotated), reusing it returns 401.
     */
    @Test
    void refresh_rotatedToken_returns401() throws Exception {
        seedActiveUser(TEST_EMAIL, TEST_PASSWORD);
        String originalRefreshToken = loginAndGetRefreshToken(TEST_EMAIL, TEST_PASSWORD);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(originalRefreshToken);

        // First refresh — consumes the original token and issues a new one
        performPost(BASE + "/refresh", req, null)
                .andExpect(status().isOk());

        // Second use of the original token — must fail
        performPost(BASE + "/refresh", req, null)
                .andExpect(status().isUnauthorized());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Builds a valid {@link RegisterRequest} for the given credentials.
     */
    private RegisterRequest buildRegisterRequest(String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFirstName("Test");
        req.setLastName("User");
        req.setBirthDate(LocalDate.of(1990, 1, 1));
        req.setGender(Resident.Gender.MALE);
        req.setAddress("123 Test Street, Barangay Test");
        return req;
    }

    /**
     * Saves a bcrypt-hashed ACTIVE user directly to the repository (no resident
     * record needed for auth tests).
     */
    private void seedActiveUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName("Test")
                .lastName("User")
                .role(User.Role.RESIDENT)
                .status(User.UserStatus.ACTIVE)
                .mustChangePassword(false)
                .build();
        userRepository.save(user);
    }

    /**
     * Logs in and returns the raw refresh token from the response body.
     */
    private String loginAndGetRefreshToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = performPost(BASE + "/login", req, null)
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("refreshToken").asText();
    }
}
