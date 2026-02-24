package com.barangay.clearance.identity.controller;

import com.barangay.clearance.identity.dto.CreateStaffRequest;
import com.barangay.clearance.identity.dto.UserDTO;
import com.barangay.clearance.identity.service.UserService;
import com.barangay.clearance.shared.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Users (Admin)", description = "Admin-only user management")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List all staff accounts (paginated)")
    @GetMapping
    public ResponseEntity<PageResponse<UserDTO>> listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(userService.listStaff(pageable));
    }

    @Operation(summary = "Get a user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @Operation(summary = "Create a staff account (CLERK, APPROVER, or ADMIN)")
    @PostMapping
    public ResponseEntity<UserDTO> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createStaff(request));
    }

    @Operation(summary = "Deactivate a user account")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<UserDTO> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.deactivate(id));
    }
}
