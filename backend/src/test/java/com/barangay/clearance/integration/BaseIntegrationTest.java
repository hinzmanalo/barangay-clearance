package com.barangay.clearance.integration;

import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.identity.repository.UserRepository;
import com.barangay.clearance.identity.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Base class for all backend integration tests.
 *
 * <p>
 * Starts a single PostgreSQL container for the entire JVM using the singleton
 * container pattern. Flyway migrations run automatically via the {@code test}
 * profile. Each test subclass is responsible for truncating tables in its own
 * {@code @BeforeEach} by calling {@link #truncateAllTables()}.
 * </p>
 *
 * <h3>Token helpers</h3>
 * <ul>
 * <li>{@link #asAdmin()}, {@link #asClerk()}, {@link #asApprover()} — generate
 * a {@code Bearer} token for a fixed static UUID with the matching role. No DB
 * record needed since {@code JwtAuthFilter} is stateless.</li>
 * <li>{@link #asResident(UUID)} — generates a {@code Bearer} token for the
 * supplied resident user UUID.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);

    /** Fixed UUIDs for staff tokens — no corresponding DB row required. */
    protected static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    protected static final UUID CLERK_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    protected static final UUID APPROVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    // ── Singleton Testcontainers container ───────────────────────────────────

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    // ── Injected beans ───────────────────────────────────────────────────────

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JwtService jwtService;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected DataSource dataSource;

    // ── Token helpers ────────────────────────────────────────────────────────

    /**
     * Returns a {@code Bearer <token>} string for an ADMIN-role JWT.
     * The token subject is {@link #ADMIN_ID}; no DB row required.
     */
    protected String asAdmin() {
        return "Bearer " + jwtService.generateAccessToken(
                ADMIN_ID, "admin@test.internal", User.Role.ADMIN, false);
    }

    /**
     * Returns a {@code Bearer <token>} string for a CLERK-role JWT.
     * The token subject is {@link #CLERK_ID}; no DB row required.
     */
    protected String asClerk() {
        return "Bearer " + jwtService.generateAccessToken(
                CLERK_ID, "clerk@test.internal", User.Role.CLERK, false);
    }

    /**
     * Returns a {@code Bearer <token>} string for an APPROVER-role JWT.
     * The token subject is {@link #APPROVER_ID}; no DB row required.
     */
    protected String asApprover() {
        return "Bearer " + jwtService.generateAccessToken(
                APPROVER_ID, "approver@test.internal", User.Role.APPROVER, false);
    }

    /**
     * Returns a {@code Bearer <token>} string for a RESIDENT-role JWT.
     *
     * @param userId the UUID that appears as the JWT subject (must match the
     *               resident's {@code userId} column if service-layer calls
     *               resolve the resident from the token)
     */
    protected String asResident(UUID userId) {
        return "Bearer " + jwtService.generateAccessToken(
                userId, "resident@test.internal", User.Role.RESIDENT, false);
    }

    // ── MockMvc wrappers ─────────────────────────────────────────────────────

    /**
     * Performs a GET request with an {@code Authorization} header.
     *
     * @param url   the request URL
     * @param token a pre-formatted {@code Bearer <token>} string, or {@code null}
     *              to omit the header
     */
    protected ResultActions performGet(String url, String token) throws Exception {
        var req = get(url).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            req = req.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mockMvc.perform(req).andDo(print());
    }

    /**
     * Performs a POST request with a JSON body and an {@code Authorization} header.
     *
     * @param url   the request URL
     * @param body  the request body (will be serialised with {@link ObjectMapper});
     *              pass {@code null} to send a request with no body
     * @param token a pre-formatted {@code Bearer <token>} string, or {@code null}
     */
    protected ResultActions performPost(String url, Object body, String token) throws Exception {
        var req = post(url);
        if (body != null) {
            req = req.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        }
        if (token != null) {
            req = req.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mockMvc.perform(req).andDo(print());
    }

    /**
     * Performs a POST request with a JSON body, an {@code Authorization} header,
     * and an {@code Idempotency-Key} header.
     *
     * @param url            the request URL
     * @param body           the request body; {@code null} for no body
     * @param token          a pre-formatted {@code Bearer <token>} string, or
     *                       {@code null}
     * @param idempotencyKey value for the {@code Idempotency-Key} header, or
     *                       {@code null}
     */
    protected ResultActions performPost(String url, Object body, String token,
            String idempotencyKey) throws Exception {
        var req = post(url);
        if (body != null) {
            req = req.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        }
        if (token != null) {
            req = req.header(HttpHeaders.AUTHORIZATION, token);
        }
        if (idempotencyKey != null) {
            req = req.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(req).andDo(print());
    }

    /**
     * Performs a PATCH request with a JSON body and an {@code Authorization}
     * header.
     */
    protected ResultActions performPatch(String url, Object body, String token) throws Exception {
        var req = patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (token != null) {
            req = req.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mockMvc.perform(req).andDo(print());
    }

    /**
     * Performs a PUT request with a JSON body and an {@code Authorization} header.
     */
    protected ResultActions performPut(String url, Object body, String token) throws Exception {
        var req = put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (token != null) {
            req = req.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mockMvc.perform(req).andDo(print());
    }

    /**
     * Performs a multipart POST request (file upload) with an {@code Authorization}
     * header.
     *
     * @param url   the request URL
     * @param file  the file part to attach
     * @param token a pre-formatted {@code Bearer <token>} string, or {@code null}
     */
    protected ResultActions performMultipart(String url, MockMultipartFile file,
            String token) throws Exception {
        MockMultipartHttpServletRequestBuilder req = MockMvcRequestBuilders.multipart(url).file(file);
        if (token != null) {
            req = (MockMultipartHttpServletRequestBuilder) req.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mockMvc.perform(req).andDo(print());
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    /**
     * Cleanup after each test: explicitly close dangling connections and
     * terminate any active transactions. This prevents lock contention and
     * connection pool exhaustion when running multiple IT tests in sequence.
     */
    @AfterEach
    void tearDown() {
        try {
            // Force termination of any idle/active connections to the test DB
            jdbcTemplate.execute(
                    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity " +
                            "WHERE pid <> pg_backend_pid() AND datname = current_database()");
            logger.debug("Terminated dangling connections");
        } catch (Exception e) {
            logger.warn("Error terminating connections: {}", e.getMessage());
        }
    }

    /**
     * Truncates all application tables in dependency order, preserving the
     * singleton {@code barangay_settings} and {@code fee_config} rows seeded by
     * Flyway.
     *
     * <p>
     * Call this from your subclass {@code @BeforeEach} method so each test
     * starts with an empty dataset.
     * </p>
     */
    protected void truncateAllTables() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE audit_logs, payments, clearance_requests, " +
                        "clearance_number_sequence, residents, refresh_tokens, users " +
                        "RESTART IDENTITY CASCADE");
    }

    /**
     * Inserts the fixed staff rows (ADMIN, CLERK, APPROVER) back into the
     * {@code users} table using the same UUIDs as {@link #ADMIN_ID},
     * {@link #CLERK_ID}, and {@link #APPROVER_ID}.
     *
     * <p>
     * Call this after {@link #truncateAllTables()} in any test that creates
     * walk-in clearances or any other resource whose {@code requested_by} /
     * {@code user_id} column has a FK to {@code users(id)}.
     * </p>
     */
    protected void seedStaffUsers() {
        jdbcTemplate.execute(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, status) VALUES " +
                        "('" + ADMIN_ID
                        + "', 'admin@test.internal',    'noop', 'Admin',    'User', 'ADMIN',    'ACTIVE'), " +
                        "('" + CLERK_ID
                        + "', 'clerk@test.internal',    'noop', 'Clerk',    'User', 'CLERK',    'ACTIVE'), " +
                        "('" + APPROVER_ID
                        + "', 'approver@test.internal', 'noop', 'Approver', 'User', 'APPROVER', 'ACTIVE') " +
                        "ON CONFLICT (id) DO NOTHING");
    }
}
