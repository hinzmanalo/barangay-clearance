package com.barangay.clearance.identity.controller;

import com.barangay.clearance.identity.dto.UpdateProfileRequest;
import com.barangay.clearance.identity.dto.UserDTO;
import com.barangay.clearance.identity.service.UserService;
import com.barangay.clearance.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for the authenticated user to view and update their own profile.
 * Available to any authenticated role (ADMIN, CLERK, APPROVER, RESIDENT).
 */
@Tag(name = "Me", description = "Authenticated user profile endpoints")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class MeController {

    private final UserService userService;

    /**
     * Retrieve the authenticated user's own profile.
     *
     * @param principal the authenticated user extracted from the JWT
     * @return the user's profile data
     */
    @Operation(summary = "Get own profile", description = "Returns the profile of the currently authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User record not found")
    })
    @GetMapping
    public ResponseEntity<UserDTO> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getUserId()));
    }

    /**
     * Update the authenticated user's own profile (first name and/or last name
     * only).
     * Email and role changes must go through admin endpoints.
     *
     * @param principal the authenticated user extracted from the JWT
     * @param request   fields to update; null fields are ignored
     * @return the updated profile
     */
    @Operation(summary = "Update own profile", description = "Allows the authenticated user to update their first and last name. Email and role cannot be changed here.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User record not found")
    })
    @PutMapping
    public ResponseEntity<UserDTO> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(principal.getUserId(), request));
    }
}
