package com.barangay.clearance.identity.service;

import com.barangay.clearance.identity.dto.*;
import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.identity.repository.RefreshTokenRepository;
import com.barangay.clearance.identity.repository.UserRepository;
import com.barangay.clearance.shared.exception.AppException;
import com.barangay.clearance.shared.util.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * List all non-resident staff accounts — paginated with optional filters.
     *
     * @param role   optional role filter
     * @param status optional status filter
     * @param search optional keyword matched against firstName, lastName, or email (case-insensitive)
     * @param pageable pagination / sort
     * @return paginated staff list
     */
    @Transactional(readOnly = true)
    public PageResponse<UserDTO> listStaff(
            User.Role role,
            User.UserStatus status,
            String search,
            Pageable pageable) {

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude residents
            predicates.add(cb.notEqual(root.get("role"), User.Role.RESIDENT));

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate firstNameLike = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameLike  = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate emailLike     = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(firstNameLike, lastNameLike, emailLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserDTO> page = userRepository.findAll(spec, pageable).map(this::toDTO);
        return PageResponse.of(page);
    }

    /**
     * Get a single user by ID.
     */
    @Transactional(readOnly = true)
    public UserDTO getUser(UUID userId) {
        return userRepository.findById(userId)
                .map(this::toDTO)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    /**
     * Create a staff account (ADMIN, CLERK, or APPROVER).
     */
    @Transactional
    public UserDTO createStaff(CreateStaffRequest request) {
        if (request.getRole() == User.Role.RESIDENT) {
            throw AppException.badRequest("Use /auth/register to create resident accounts");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.conflict("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .status(User.UserStatus.ACTIVE)
                .mustChangePassword(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Staff account created: {} ({})", saved.getEmail(), saved.getRole());
        return toDTO(saved);
    }

    /**
     * Deactivate a user account.
     */
    @Transactional
    public UserDTO deactivate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (user.getStatus() == User.UserStatus.DEACTIVATED || user.getStatus() == User.UserStatus.INACTIVE) {
            throw AppException.badRequest("User is already deactivated");
        }

        user.setStatus(User.UserStatus.DEACTIVATED);
        userRepository.save(user);
        log.info("User deactivated: {}", user.getEmail());
        return toDTO(user);
    }

    /**
     * Reactivate a deactivated user account.
     *
     * @param userId the target user's ID
     * @return updated user DTO
     * @throws AppException 404 if user not found
     * @throws AppException 400 if user is not currently DEACTIVATED
     */
    @Transactional
    public UserDTO activate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (user.getStatus() != User.UserStatus.DEACTIVATED) {
            throw AppException.badRequest("User is not deactivated");
        }

        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User activated: {}", user.getEmail());
        return toDTO(user);
    }

    /**
     * Change a staff user's role.
     *
     * <p>Guards:</p>
     * <ul>
     *   <li>Role cannot be set to RESIDENT.</li>
     *   <li>Admin cannot demote their own account.</li>
     * </ul>
     *
     * @param userId       target user
     * @param newRole      the desired new role
     * @param callerUserId the authenticated admin's own ID (to prevent self-demotion)
     * @return updated user DTO
     */
    @Transactional
    public UserDTO updateRole(UUID userId, User.Role newRole, UUID callerUserId) {
        if (newRole == User.Role.RESIDENT) {
            throw AppException.badRequest("Cannot assign RESIDENT role to a staff account");
        }
        if (userId.equals(callerUserId)) {
            throw AppException.badRequest("Cannot change your own role");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        User.Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        log.info("User role changed: {} ({} → {})", user.getEmail(), oldRole, newRole);
        return toDTO(user);
    }

    /**
     * Update a staff user's profile fields (name and/or email).
     * If the email is being changed, uniqueness is enforced.
     *
     * @param userId  target user
     * @param request fields to update (nulls are ignored)
     * @return updated user DTO
     */
    @Transactional
    public UserDTO updateStaff(UUID userId, UpdateStaffRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw AppException.conflict("Email already registered");
            }
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }

        userRepository.save(user);
        log.info("Staff profile updated: {}", user.getEmail());
        return toDTO(user);
    }

    /**
     * Force-reset a user's password and invalidate all their refresh tokens.
     * Sets {@code mustChangePassword = true} on the account.
     *
     * @param userId      target user
     * @param newPassword plain-text password (will be BCrypt-encoded)
     */
    @Transactional
    public void adminResetPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);

        // Invalidate all active sessions for this user
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Admin reset password for user: {}", user.getEmail());
    }

    /**
     * Fetch the authenticated user's own profile.
     *
     * @param userId the caller's user ID
     * @return user DTO
     */
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .map(this::toDTO)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    /**
     * Update the authenticated user's own profile.
     * Only first name and last name may be changed — email and role changes are admin-only.
     *
     * @param userId  the caller's user ID
     * @param request fields to update (nulls ignored)
     * @return updated user DTO
     */
    @Transactional
    public UserDTO updateCurrentUser(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }

        userRepository.save(user);
        log.info("User self-updated profile: {}", user.getEmail());
        return toDTO(user);
    }

    // ---- private helpers ----

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .status(user.getStatus())
                .mustChangePassword(user.isMustChangePassword())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
