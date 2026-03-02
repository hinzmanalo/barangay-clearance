package com.barangay.clearance.identity.controller;

import com.barangay.clearance.identity.dto.*;
import com.barangay.clearance.identity.entity.User;
import com.barangay.clearance.identity.service.UserService;
import com.barangay.clearance.shared.security.UserPrincipal;
import com.barangay.clearance.shared.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Users (Admin)", description = "Admin-only user management")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List staff accounts (paginated, filterable)",
               description = "Returns all non-RESIDENT accounts. Supports optional filtering by role, status, and keyword search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated staff list returned"),
            @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping
    public ResponseEntity<PageResponse<UserDTO>> listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) User.UserStatus status,
            @RequestParam(required = false) String search) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(userService.listStaff(role, status, search, pageable));
    }

    @Operation(summary = "Get a user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @Operation(summary = "Create a staff account (CLERK, APPROVER, or ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Staff account created"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping
    public ResponseEntity<UserDTO> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createStaff(request));
    }

    @Operation(summary = "Deactivate a user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deactivated"),
            @ApiResponse(responseCode = "400", description = "User is already deactivated"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<UserDTO> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.deactivate(id));
    }

    @Operation(summary = "Reactivate a deactivated user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User reactivated"),
            @ApiResponse(responseCode = "400", description = "User is not deactivated"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}/activate")
    public ResponseEntity<UserDTO> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.activate(id));
    }

    @Operation(summary = "Change a staff user's role",
               description = "Admins cannot change their own role. Role cannot be set to RESIDENT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "400", description = "Invalid role or self-demotion attempt"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}/role")
    public ResponseEntity<UserDTO> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateRole(id, request.getRole(), principal.getUserId()));
    }

    @Operation(summary = "Update a staff user's profile (name and/or email)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest request) {
        return ResponseEntity.ok(userService.updateStaff(id, request));
    }

    @Operation(summary = "Force-reset a user's password",
               description = "Hashes and saves the new password, sets mustChangePassword=true, and invalidates all refresh tokens for the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> adminResetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.adminResetPassword(id, request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
