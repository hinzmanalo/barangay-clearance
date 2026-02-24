package com.barangay.clearance.identity.service;

import com.barangay.clearance.identity.dto.CreateStaffRequest;
import com.barangay.clearance.identity.dto.UserDTO;
import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.identity.repository.UserRepository;
import com.barangay.clearance.shared.exception.AppException;
import com.barangay.clearance.shared.util.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * List all non-resident users (staff accounts) — paginated.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserDTO> listStaff(Pageable pageable) {
        Page<UserDTO> page = userRepository
                .findByRoleNot(User.Role.RESIDENT, pageable)
                .map(this::toDTO);
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
