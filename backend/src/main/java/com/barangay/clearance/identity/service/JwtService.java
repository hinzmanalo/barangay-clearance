package com.barangay.clearance.identity.service;

import com.barangay.clearance.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${app.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    /**
     * Generate a signed JWT access token embedding userId, email, and role.
     */
    public String generateAccessToken(UUID userId, String email, User.Role role, boolean mustChangePassword) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("mustChangePassword", mustChangePassword)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpiryMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a random opaque refresh token UUID.
     */
    public String generateRawRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Hash the raw refresh token with SHA-256.
     */
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available for refresh token hashing", e);
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Compute the expiry instant for a new refresh token.
     */
    public Instant refreshTokenExpiry() {
        return Instant.now().plusMillis(refreshTokenExpiryMs);
    }

    /**
     * Return access token expiry in seconds from now.
     */
    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMs / 1000;
    }

    /**
     * Extract userId from a valid, signed access token.
     */
    public UUID extractUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    /**
     * Extract role from a valid, signed access token.
     */
    public User.Role extractRole(String token) {
        return User.Role.valueOf(getClaims(token).get("role", String.class));
    }

    /**
     * Extract email from a valid, signed access token.
     */
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * Validate and parse claims — throws JwtException on failure.
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
