package com.barangay.clearance.residents.service;

import com.barangay.clearance.identity.dto.RegisterRequest;
import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.identity.repository.UserRepository;
import com.barangay.clearance.residents.dto.CreateResidentRequest;
import com.barangay.clearance.residents.dto.ResidentDTO;
import com.barangay.clearance.residents.dto.UpdateResidentRequest;
import com.barangay.clearance.residents.entity.Resident;
import com.barangay.clearance.residents.repository.ResidentRepository;
import com.barangay.clearance.residents.service.mapper.ResidentMapper;
import com.barangay.clearance.shared.audit.AuditAction;
import com.barangay.clearance.shared.audit.AuditService;
import com.barangay.clearance.shared.exception.AppException;
import com.barangay.clearance.shared.util.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepository;
    private final UserRepository userRepository;
    private final ResidentMapper residentMapper;
    private final AuditService auditService;

    // ─────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────

    /**
     * Enrich a ResidentDTO with portal status from the linked user account.
     * Sets portalStatus if hasPortalAccount is true.
     */
    private ResidentDTO enrichWithPortalStatus(ResidentDTO dto) {
        if (dto.isHasPortalAccount() && dto.getUserId() != null) {
            userRepository.findById(dto.getUserId())
                    .ifPresent(user -> dto.setPortalStatus(user.getStatus()));
        }
        return dto;
    }

    /**
     * Search residents by name (q) and/or purok/zone (purok).
     * Results are paginated. Null/blank parameters are ignored.
     */
    @Transactional(readOnly = true)
    public PageResponse<ResidentDTO> search(String q, String purok, Pageable pageable) {
        String normalizedQ = (q != null && !q.isBlank()) ? q.trim() : "";
        String normalizedPurok = (purok != null && !purok.isBlank()) ? purok.trim() : "";

        Page<ResidentDTO> page = residentRepository
                .search(normalizedQ, normalizedPurok, pageable)
                .map(residentMapper::toDTO)
                .map(this::enrichWithPortalStatus);
        return PageResponse.of(page);
    }

    /**
     * Get a single resident by ID.
     *
     * @throws AppException 404 if not found
     */
    @Transactional(readOnly = true)
    public ResidentDTO getById(UUID id) {
        return residentRepository.findById(id)
                .map(residentMapper::toDTO)
                .map(this::enrichWithPortalStatus)
                .orElseThrow(() -> AppException.notFound("Resident not found"));
    }

    /**
     * Create a walk-in resident (no portal account).
     */
    @Transactional
    public ResidentDTO create(CreateResidentRequest request) {
        Resident resident = residentMapper.toEntity(request);
        Resident saved = residentRepository.save(resident);
        log.info("RESIDENT_CREATED id={} name={} {}", saved.getId(), saved.getFirstName(), saved.getLastName());
        auditService.log(null, AuditAction.RESIDENT_CREATED, "Resident", saved.getId(),
                "Resident created (walk-in): " + saved.getLastName() + ", " + saved.getFirstName());
        return enrichWithPortalStatus(residentMapper.toDTO(saved));
    }

    /**
     * Update an existing resident's profile fields (partial update — null fields
     * are skipped).
     *
     * @throws AppException 404 if not found
     */
    @Transactional
    public ResidentDTO update(UUID id, UpdateResidentRequest request) {
        Resident resident = residentRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Resident not found"));

        if (request.getFirstName() != null)
            resident.setFirstName(request.getFirstName());
        if (request.getMiddleName() != null)
            resident.setMiddleName(request.getMiddleName());
        if (request.getLastName() != null)
            resident.setLastName(request.getLastName());
        if (request.getBirthDate() != null)
            resident.setBirthDate(request.getBirthDate());
        if (request.getGender() != null)
            resident.setGender(request.getGender());
        if (request.getAddress() != null)
            resident.setAddress(request.getAddress());
        if (request.getContactNumber() != null)
            resident.setContactNumber(request.getContactNumber());
        if (request.getEmail() != null)
            resident.setEmail(request.getEmail());
        if (request.getStatus() != null)
            resident.setStatus(request.getStatus());
        if (request.getUserId() != null)
            resident.setUserId(request.getUserId());

        Resident saved = residentRepository.save(resident);
        log.info("RESIDENT_UPDATED id={}", saved.getId());
        auditService.log(null, AuditAction.RESIDENT_UPDATED, "Resident", saved.getId(),
                "Resident profile updated: " + saved.getLastName() + ", " + saved.getFirstName());
        return enrichWithPortalStatus(residentMapper.toDTO(saved));
    }

    /**
     * Create a resident profile atomically during portal self-registration.
     * Called from {@code AuthService.register()} inside the same transaction.
     *
     * @param request the registration payload submitted by the new resident
     * @param user    the already-persisted {@link User} created for this
     *                registration
     */
    @Transactional
    public Resident createFromRegistration(RegisterRequest request, User user) {
        Resident resident = Resident.builder()
                .userId(user.getId())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .address(request.getAddress())
                .contactNumber(request.getContactNumber())
                .email(request.getEmail())
                .status(Resident.ResidentStatus.ACTIVE)
                .build();

        Resident saved = residentRepository.save(resident);
        log.info("RESIDENT_CREATED (via registration) id={} userId={}", saved.getId(), user.getId());
        auditService.log(user.getId(), AuditAction.RESIDENT_CREATED, "Resident", saved.getId(),
                "Resident profile created via self-registration for userId=" + user.getId());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────
    // Pending-user activation workflow
    // ─────────────────────────────────────────────────────────────────

    /**
     * Return all resident profiles whose linked portal account has
     * {@code status = PENDING_VERIFICATION}.
     */
    @Transactional(readOnly = true)
    public List<ResidentDTO> findPendingUsers() {
        List<User> pendingUsers = userRepository.findByStatus(
                User.UserStatus.PENDING_VERIFICATION,
                Pageable.unpaged()).getContent();

        return pendingUsers.stream()
                .filter(u -> u.getRole() == User.Role.RESIDENT)
                .map(u -> residentRepository.findByUserId(u.getId())
                        .map(residentMapper::toDTO)
                        .map(this::enrichWithPortalStatus)
                        .orElse(null))
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * Activate a pending resident portal account.
     * Sets {@code user.status = ACTIVE}.
     *
     * @throws AppException 404 if user not found
     * @throws AppException 400 if user is not in PENDING_VERIFICATION state
     */
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (user.getStatus() != User.UserStatus.PENDING_VERIFICATION) {
            throw AppException.badRequest("User is not pending verification");
        }

        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("USER_ACCOUNT_ACTIVATED userId={}", userId);
        auditService.log(userId, AuditAction.RESIDENT_ACTIVATED, "User", userId,
                "Resident portal account activated");
    }

    /**
     * Reject a resident portal account.
     * Sets {@code user.status = REJECTED}.
     * Allowed from {@code PENDING_VERIFICATION} or {@code ACTIVE} states.
     *
     * @throws AppException 404 if user not found
     * @throws AppException 400 if user is not in a rejectable state
     */
    @Transactional
    public void rejectUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (user.getStatus() != User.UserStatus.PENDING_VERIFICATION
                && user.getStatus() != User.UserStatus.ACTIVE) {
            throw AppException.badRequest("User cannot be rejected from status: " + user.getStatus());
        }

        user.setStatus(User.UserStatus.REJECTED);
        userRepository.save(user);
        log.info("USER_ACCOUNT_REJECTED userId={}", userId);
    }
}
