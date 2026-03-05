package com.barangay.clearance.identity.service;

import com.barangay.clearance.identity.dto.*;
import com.barangay.clearance.identity.entity.RefreshToken;
import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.identity.repository.RefreshTokenRepository;
import com.barangay.clearance.identity.repository.UserRepository;
import com.barangay.clearance.residents.entity.Resident;
import com.barangay.clearance.residents.service.ResidentService;
import com.barangay.clearance.shared.audit.AuditService;
import com.barangay.clearance.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ResidentService residentService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private UUID testUserId;
    private String testEmail;
    private String testPassword;
    private String encryptedPassword;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testPassword = "password123";
        encryptedPassword = "$2a$10$encryptedHashValue";
    }

    @Test
    void register_success_createsUserAndReturnsSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBirthDate(LocalDate.of(1990, 5, 15));
        request.setGender(Resident.Gender.MALE);
        request.setAddress("123 Main St");

        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn(encryptedPassword);

        User savedUser = User.builder()
                .id(testUserId)
                .email(testEmail)
                .passwordHash(encryptedPassword)
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.RESIDENT)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .mustChangePassword(false)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.register(request);

        verify(userRepository).save(any(User.class));
        verify(residentService).createFromRegistration(request, savedUser);
    }

    @Test
    void register_duplicateEmail_throws409() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void login_success_returnsTokenResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        User activeUser = User.builder()
                .id(testUserId)
                .email(testEmail)
                .passwordHash(encryptedPassword)
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.RESIDENT)
                .status(User.UserStatus.ACTIVE)
                .mustChangePassword(false)
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(testPassword, encryptedPassword)).thenReturn(true);
        when(jwtService.generateAccessToken(testUserId, testEmail, User.Role.RESIDENT, false))
                .thenReturn("access-token");
        when(jwtService.generateRawRefreshToken()).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3600L);

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void login_wrongPassword_throws401() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        User user = User.builder()
                .id(testUserId)
                .email(testEmail)
                .passwordHash(encryptedPassword)
                .status(User.UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(testPassword, encryptedPassword)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void login_userNotFound_throws401() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword(testPassword);

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void login_pendingVerificationStatus_throws403() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        User pendingUser = User.builder()
                .id(testUserId)
                .email(testEmail)
                .passwordHash(encryptedPassword)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(pendingUser));
        when(passwordEncoder.matches(testPassword, encryptedPassword)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void refresh_validToken_returnsNewTokens() {
        String rawRefreshToken = "550e8400-e29b-41d4-a716-446655440000";
        String hashedRefreshToken = "hashed-token-value";

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(testUserId)
                .tokenHash(hashedRefreshToken)
                .expiresAt(Instant.now().plusSeconds(86400))
                .revoked(false)
                .build();

        User user = User.builder()
                .id(testUserId)
                .email(testEmail)
                .role(User.Role.RESIDENT)
                .status(User.UserStatus.ACTIVE)
                .mustChangePassword(false)
                .build();

        when(jwtService.hashRefreshToken(rawRefreshToken)).thenReturn(hashedRefreshToken);
        when(refreshTokenRepository.findByTokenHash(hashedRefreshToken))
                .thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(testUserId, testEmail, User.Role.RESIDENT, false))
                .thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3600L);

        TokenResponse response = authService.refresh(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
    }

    @Test
    void refresh_invalidToken_throws401() {
        String rawRefreshToken = "invalid-token";
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(rawRefreshToken);

        when(jwtService.hashRefreshToken(rawRefreshToken))
                .thenReturn("hashed-invalid-token");
        when(refreshTokenRepository.findByTokenHash("hashed-invalid-token"))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.refresh(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void logout_revokesRefreshToken() {
        String rawRefreshToken = "token-to-revoke";
        String hashedToken = "hashed-token";

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(rawRefreshToken);

        RefreshToken tokenToRevoke = RefreshToken.builder()
                .userId(testUserId)
                .tokenHash(hashedToken)
                .expiresAt(Instant.now().plusSeconds(86400))
                .revoked(false)
                .build();

        when(jwtService.hashRefreshToken(rawRefreshToken)).thenReturn(hashedToken);
        when(refreshTokenRepository.findByTokenHash(hashedToken))
                .thenReturn(Optional.of(tokenToRevoke));

        authService.logout(request);

        assertTrue(tokenToRevoke.isRevoked());
        verify(refreshTokenRepository).save(tokenToRevoke);
    }

    @Test
    void changePassword_success_changesPasswordAndRotatesTokens() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(testPassword);
        request.setNewPassword("newPassword123");

        User user = User.builder()
                .id(testUserId)
                .email(testEmail)
                .passwordHash(encryptedPassword)
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.RESIDENT)
                .status(User.UserStatus.ACTIVE)
                .mustChangePassword(true)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(testPassword, encryptedPassword)).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encrypted-password");
        when(jwtService.generateAccessToken(testUserId, testEmail, User.Role.RESIDENT, false))
                .thenReturn("new-access-token");
        when(jwtService.generateRawRefreshToken()).thenReturn("new-refresh-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3600L);

        TokenResponse response = authService.changePassword(testUserId, request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertFalse(user.isMustChangePassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongCurrentPassword_throws400() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");

        User user = User.builder()
                .id(testUserId)
                .email(testEmail)
                .passwordHash(encryptedPassword)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", encryptedPassword)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> authService.changePassword(testUserId, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
