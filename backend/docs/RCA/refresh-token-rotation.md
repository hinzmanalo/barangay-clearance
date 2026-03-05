# Refresh Token Rotation Fix — Root Cause & Resolution

**Date:** March 4, 2026  
**Test:** `AuthControllerIT.refresh_rotatedToken_returns401`  
**Status:** ✅ RESOLVED

---

## Issue

`AuthControllerIT.refresh_rotatedToken_returns401:161` was failing:

- **Expected:** `401 Unauthorized` when reusing a consumed refresh token
- **Actual:** `200 OK` (token reuse was allowed)

### Test Scenario

```
1. User logs in → receives refreshToken₁
2. First /refresh with refreshToken₁ → 200 OK (issue new access token)
3. Second /refresh with refreshToken₁ (reused) → Expected 401, Got 200 ❌
```

The test expected token rotation: once a token is consumed, it must be invalidated. Reusing it should fail with 401 Unauthorized.

---

## Root Cause

The `AuthService.refresh(RefreshRequest)` method did not implement token rotation.

**Original Implementation (lines 130–159):**

```java
@Transactional(readOnly = true)  // ❌ readOnly = no DB writes possible
public TokenResponse refresh(RefreshRequest request) {
    String hash = jwtService.hashRefreshToken(request.getRefreshToken());

    RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));

    if (refreshToken.isRevoked()) {
        throw AppException.unauthorized("Refresh token has been revoked");
    }
    if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
        throw AppException.unauthorized("Refresh token has expired");
    }

    User user = userRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> AppException.unauthorized("User not found"));

    log.debug("Access token refreshed for user: {}", user.getId());
    auditService.log(user.getId(), AuditAction.USER_TOKEN_REFRESHED, "User", user.getId(), null);
    String newAccessToken = jwtService.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole(), user.isMustChangePassword());

    return TokenResponse.builder()
            .accessToken(newAccessToken)
            .tokenType("Bearer")
            .expiresIn(jwtService.getAccessTokenExpirySeconds())
            .build();  // ❌ No refreshToken returned, old token never revoked
}
```

**Problems:**

1. **No token revocation:** The old refresh token was never marked as revoked, allowing unlimited reuse.
2. **readOnly transaction:** The `@Transactional(readOnly = true)` annotation prevented any writes, so even if the code attempted to revoke the token, it would fail.
3. **No new token issued:** The response did not include a new refresh token, violating the token rotation pattern.

---

## Resolution

Implemented proper **token rotation** in `AuthService.refresh()`:

### Changes Made

1. **Changed transaction scope** from `readOnly = true` to `@Transactional` (write-capable)
2. **Revoke the consumed token:** Mark the old refresh token as revoked after successful validation
3. **Generate new token:** Create and persist a new refresh token
4. **Return new token:** Include the new refresh token in the response

**Updated Implementation:**

```java
/**
 * Issue a new access token from a valid refresh token with rotation.
 * The old refresh token is revoked; a new one is generated and returned.
 */
@Transactional  // ✅ Write-capable
public TokenResponse refresh(RefreshRequest request) {
    String hash = jwtService.hashRefreshToken(request.getRefreshToken());

    RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));

    if (refreshToken.isRevoked()) {
        throw AppException.unauthorized("Refresh token has been revoked");
    }
    if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
        throw AppException.unauthorized("Refresh token has expired");
    }

    User user = userRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> AppException.unauthorized("User not found"));

    // ✅ Revoke the old refresh token (token rotation)
    refreshToken.setRevoked(true);
    refreshTokenRepository.save(refreshToken);

    // ✅ Generate new refresh token
    String rawRefresh = jwtService.generateRawRefreshToken();
    String hashedRefresh = jwtService.hashRefreshToken(rawRefresh);
    Instant expiresAt = jwtService.refreshTokenExpiry();

    RefreshToken newRefreshToken = RefreshToken.builder()
            .userId(user.getId())
            .tokenHash(hashedRefresh)
            .expiresAt(expiresAt)
            .revoked(false)
            .build();
    refreshTokenRepository.save(newRefreshToken);

    log.debug("Access token refreshed with token rotation for user: {}", user.getId());
    auditService.log(user.getId(), AuditAction.USER_TOKEN_REFRESHED, "User", user.getId(), null);
    String newAccessToken = jwtService.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole(), user.isMustChangePassword());

    return TokenResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(rawRefresh)  // ✅ New refresh token returned
            .tokenType("Bearer")
            .expiresIn(jwtService.getAccessTokenExpirySeconds())
            .build();
}
```

---

## Security Rationale

Token rotation is a critical defense against **token replay attacks**:

- If a refresh token is intercepted during network transmission, the attacker can use it once before the legitimate owner uses it.
- Token rotation ensures that after the first use, the original token is invalidated.
- Subsequent attempts to reuse the intercepted token fail with `401 Unauthorized`.

This pattern is recommended by OAuth 2.0 and OpenID Connect specifications for long-lived refresh tokens.

---

## Test Results

All 7 `AuthControllerIT` tests now pass:

✅ `register_validPayload_returns201`  
✅ `login_validCredentials_returns200WithTokens`  
✅ `refresh_validToken_returns200WithNewAccessToken` — First refresh succeeds  
✅ `logout_thenRefresh_returns401` — Revoked token is rejected  
✅ `refresh_rotatedToken_returns401` — **Reused token is rejected** ✓  
✅ `changePassword_validRequest_returns200AndInvalidatesAllTokens`  
✅ `changePassword_invalidCurrent_returns400`

---

## File Changes

- **[AuthService.java](../src/main/java/com/barangay/clearance/identity/service/AuthService.java#L130-L159)** — Token rotation implemented in `refresh()` method
- **[Security.md](../Security.md#5-refresh-token-strategy)** — Documentation updated with token rotation details

---

## Related Documentation

- [Security.md — Section 5: Refresh Token Strategy](../Security.md#5-refresh-token-strategy)
- [JJWT Docs — Token Rotation](https://github.com/jwtk/jjwt)
- [OAuth 2.0 Security Best Practices — Refresh Token Rotation](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics#section-4.13.2)
