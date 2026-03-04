package com.barangay.clearance.identity.service;

import com.barangay.clearance.identity.dto.*;
import com.barangay.clearance.identity.entity.RefreshToken;
import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.identity.repository.RefreshTokenRepository;
import com.barangay.clearance.identity.repository.UserRepository;
import com.barangay.clearance.residents.service.ResidentService;
import com.barangay.clearance.shared.audit.AuditAction;
import com.barangay.clearance.shared.audit.AuditService;
import com.barangay.clearance.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ResidentService residentService;
    private final AuditService auditService;

    /**
     * Register a new resident. Account starts as PENDING_VERIFICATION.
     * A linked Resident profile is created by ResidentService (Phase 2).
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt with already-registered email: {}", request.getEmail());
            throw AppException.conflict("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.RESIDENT)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .mustChangePassword(false)
                .build();

        User savedUser = userRepository.save(user);
        residentService.createFromRegistration(request, savedUser);
        log.info("Resident registered: {}", savedUser.getEmail());
        auditService.log(savedUser.getId(), AuditAction.USER_REGISTERED, "User",
                savedUser.getId(), "Resident registered: " + savedUser.getEmail());
    }

    /**
     * Login and issue access + refresh tokens.
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    auditService.log(null, AuditAction.USER_LOGIN_FAILED, "User", null,
                            "Attempted email: " + request.getEmail());
                    throw AppException.unauthorized("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            auditService.log(null, AuditAction.USER_LOGIN_FAILED, "User", null,
                    "Attempted email: " + request.getEmail());
            throw AppException.unauthorized("Invalid email or password");
        }

        switch (user.getStatus()) {
            case PENDING_VERIFICATION -> {
                log.warn("Login denied for {} - account status: PENDING_VERIFICATION", user.getEmail());
                throw AppException.forbidden("Account pending verification. Please wait for staff approval.");
            }
            case REJECTED -> {
                log.warn("Login denied for {} - account status: REJECTED", user.getEmail());
                throw AppException.forbidden("Account has been rejected.");
            }
            case DEACTIVATED -> {
                log.warn("Login denied for {} - account status: DEACTIVATED", user.getEmail());
                throw AppException.forbidden("Account is deactivated.");
            }
            case INACTIVE -> {
                log.warn("Login denied for {} - account status: INACTIVE", user.getEmail());
                throw AppException.forbidden("Account is inactive.");
            }
            default -> {
                /* ACTIVE — proceed */ }
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.isMustChangePassword());

        String rawRefresh = jwtService.generateRawRefreshToken();
        String hashedRefresh = jwtService.hashRefreshToken(rawRefresh);
        Instant expiresAt = jwtService.refreshTokenExpiry();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashedRefresh)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("User logged in: {}", user.getEmail());
        auditService.log(user.getId(), AuditAction.USER_LOGIN, "User", user.getId(), "Login: " + user.getEmail());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefresh)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .mustChangePassword(user.isMustChangePassword() ? true : null)
                .build();
    }

    /**
     * Issue a new access token from a valid refresh token.
     */
    @Transactional(readOnly = true)
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
                .build();
    }

    /**
     * Revoke the given refresh token.
     */
    @Transactional
    public void logout(RefreshRequest request) {
        String hash = jwtService.hashRefreshToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            log.info("Refresh token revoked for user: {}", rt.getUserId());
            auditService.log(rt.getUserId(), AuditAction.USER_LOGOUT, "User", rt.getUserId(), null);
        });
    }

    /**
     * Change password for authenticated user.
     */
    @Transactional
    public TokenResponse changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Password change failed for user {}: current password mismatch", userId);
            throw AppException.badRequest("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", userId);
        auditService.log(userId, AuditAction.USER_PASSWORD_CHANGED, "User", userId, null);

        // Revoke all existing refresh tokens and issue new ones
        refreshTokenRepository.deleteByUserId(userId);

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), false);

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

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefresh)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .build();
    }
}
