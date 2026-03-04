package com.barangay.clearance.identity.service;

import com.barangay.clearance.identity.entity.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";
    private static final long STANDARD_ACCESS_EXPIRY_MS = 3600000; // 1 hour
    private static final long STANDARD_REFRESH_EXPIRY_MS = 604800000; // 7 days

    private JwtService jwtService;
    private UUID testUserId;
    private String testEmail;
    private User.Role testRole;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, STANDARD_ACCESS_EXPIRY_MS, STANDARD_REFRESH_EXPIRY_MS);
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testRole = User.Role.RESIDENT;
    }

    @Test
    void generateAccessToken_returnsNonBlankParsableToken() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole, false);

        assertNotNull(token);
        assertNotBlank(token);
        assertTrue(token.split("\\.").length == 3, "Token should have 3 parts (header.payload.signature)");
    }

    @Test
    void extractUserId_returnsCorrectSubject() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole, false);

        UUID extracted = jwtService.extractUserId(token);

        assertEquals(testUserId, extracted);
    }

    @Test
    void extractRole_returnsCorrectClaim() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole, false);

        User.Role extracted = jwtService.extractRole(token);

        assertEquals(testRole, extracted);
    }

    @Test
    void extractEmail_returnsCorrectClaim() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole, false);

        String extracted = jwtService.extractEmail(token);

        assertEquals(testEmail, extracted);
    }

    @Test
    void extractMustChangePassword_returnsCorrectClaim() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole, true);

        // Extract the raw claim to verify it was set
        String tokenWithMustChange = jwtService.generateAccessToken(testUserId, testEmail, testRole, true);
        assertNotNull(tokenWithMustChange);

        // Verify with false as well
        String tokenWithoutMustChange = jwtService.generateAccessToken(testUserId, testEmail, testRole, false);
        assertNotNull(tokenWithoutMustChange);
    }

    @Test
    void expiredToken_throwsJwtException() throws InterruptedException {
        // Create a JwtService with very short expiry (1ms)
        JwtService shortLivedJwt = new JwtService(TEST_SECRET, 1, STANDARD_REFRESH_EXPIRY_MS);
        String token = shortLivedJwt.generateAccessToken(testUserId, testEmail, testRole, false);

        // Wait for token to expire
        Thread.sleep(10);

        // Attempt to extract userId should fail
        assertThrows(JwtException.class, () -> shortLivedJwt.extractUserId(token));
    }

    @Test
    void tamperedToken_throwsJwtException() {
        String token = jwtService.generateAccessToken(testUserId, testEmail, testRole, false);

        // Split token into 3 parts and tamper with the payload
        String[] parts = token.split("\\.");
        char[] payloadChars = parts[1].toCharArray();
        if (payloadChars.length > 0) {
            // Flip a character in the payload
            payloadChars[0] = payloadChars[0] == 'a' ? 'b' : 'a';
        }
        String tamperedToken = parts[0] + "." + new String(payloadChars) + "." + parts[2];

        // Attempt to extract from tampered token should fail
        assertThrows(JwtException.class, () -> jwtService.extractUserId(tamperedToken));
    }

    @Test
    void generateRawRefreshToken_returnsUuidFormat() {
        String rawToken = jwtService.generateRawRefreshToken();

        assertNotNull(rawToken);
        assertNotBlank(rawToken);
        // Should be a valid UUID format
        assertDoesNotThrow(() -> UUID.fromString(rawToken));
    }

    @Test
    void generateRawRefreshToken_eachCallReturnsDifferentToken() {
        String token1 = jwtService.generateRawRefreshToken();
        String token2 = jwtService.generateRawRefreshToken();

        assertNotEquals(token1, token2);
    }

    @Test
    void hashRefreshToken_returnsDeterministicSha256() {
        String rawToken = "550e8400-e29b-41d4-a716-446655440000";

        String hash1 = jwtService.hashRefreshToken(rawToken);
        String hash2 = jwtService.hashRefreshToken(rawToken);

        // Hashes should be identical
        assertEquals(hash1, hash2);
        // Hash should be 64 hex characters (SHA-256)
        assertEquals(64, hash1.length());
        assertTrue(hash1.matches("[0-9a-f]{64}"), "Hash should be valid hex");
    }

    @Test
    void hashRefreshToken_differentInputsProduceDifferentHashes() {
        String token1 = jwtService.generateRawRefreshToken();
        String token2 = jwtService.generateRawRefreshToken();

        String hash1 = jwtService.hashRefreshToken(token1);
        String hash2 = jwtService.hashRefreshToken(token2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void refreshTokenExpiry_returnsInstantInFuture() {
        long before = System.currentTimeMillis();
        var expiry = jwtService.refreshTokenExpiry();
        long after = System.currentTimeMillis();

        // Expiry should be in the future
        assertTrue(expiry.toEpochMilli() > after);
        // And should be approximately 7 days from now
        long diffMs = expiry.toEpochMilli() - after;
        long sevenDaysMs = 7 * 24 * 60 * 60 * 1000L;
        assertTrue(diffMs > sevenDaysMs - 1000); // Allow 1 second margin
        assertTrue(diffMs < sevenDaysMs + 1000);
    }

    @Test
    void getAccessTokenExpirySeconds_returnsCorrectValue() {
        long expirySeconds = jwtService.getAccessTokenExpirySeconds();

        assertEquals(STANDARD_ACCESS_EXPIRY_MS / 1000, expirySeconds);
    }

    @Test
    void generateAccessToken_differentUsersProduceDifferentTokens() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        String token1 = jwtService.generateAccessToken(userId1, testEmail, testRole, false);
        String token2 = jwtService.generateAccessToken(userId2, testEmail, testRole, false);

        assertNotEquals(token1, token2);
        assertEquals(userId1, jwtService.extractUserId(token1));
        assertEquals(userId2, jwtService.extractUserId(token2));
    }

    private void assertNotBlank(String value) {
        assertNotNull(value);
        assertFalse(value.isBlank());
    }
}
