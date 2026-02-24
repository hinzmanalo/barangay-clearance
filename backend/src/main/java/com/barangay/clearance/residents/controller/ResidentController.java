package com.barangay.clearance.residents.controller;

import com.barangay.clearance.residents.dto.CreateResidentRequest;
import com.barangay.clearance.residents.dto.ResidentDTO;
import com.barangay.clearance.residents.dto.UpdateResidentRequest;
import com.barangay.clearance.residents.service.ResidentService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Residents", description = "Resident registry management")
@RestController
@RequestMapping("/api/v1/residents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")
@SecurityRequirement(name = "bearer-jwt")
public class ResidentController {

    private final ResidentService residentService;

    @Operation(summary = "Search residents", description = "Paginated search by name and/or purok/zone")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of matching residents"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — CLERK or ADMIN role required")
    })
    @GetMapping
    public ResponseEntity<PageResponse<ResidentDTO>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String purok,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("lastName", "firstName").ascending());
        return ResponseEntity.ok(residentService.search(q, purok, pageable));
    }

    @Operation(summary = "Create a walk-in resident", description = "Creates a resident profile without a linked portal account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Resident created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    public ResponseEntity<ResidentDTO> create(@Valid @RequestBody CreateResidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(residentService.create(request));
    }

    @Operation(summary = "Get a resident by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resident found"),
            @ApiResponse(responseCode = "404", description = "Resident not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResidentDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(residentService.getById(id));
    }

    @Operation(summary = "Update a resident", description = "Partial update — only provided fields are changed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resident updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Resident not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ResidentDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResidentRequest request) {
        return ResponseEntity.ok(residentService.update(id, request));
    }

    @Operation(summary = "List pending portal registrations", description = "Returns residents whose linked portal account is awaiting verification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of pending residents"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/pending-users")
    public ResponseEntity<List<ResidentDTO>> pendingUsers() {
        return ResponseEntity.ok(residentService.findPendingUsers());
    }

    @Operation(summary = "Activate a pending portal account", description = "Sets user status to ACTIVE")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account activated"),
            @ApiResponse(responseCode = "400", description = "User is not pending verification"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable UUID userId) {
        residentService.activateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reject a pending portal account", description = "Sets user status to REJECTED")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account rejected"),
            @ApiResponse(responseCode = "400", description = "User is not pending verification"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/users/{userId}/reject")
    public ResponseEntity<Void> rejectUser(@PathVariable UUID userId) {
        residentService.rejectUser(userId);
        return ResponseEntity.noContent().build();
    }
}
