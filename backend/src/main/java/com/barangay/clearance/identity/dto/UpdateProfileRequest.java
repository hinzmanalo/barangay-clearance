package com.barangay.clearance.identity.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for a user updating their own profile via
 * {@code PUT /api/v1/users/me}.
 * Only name fields may be changed by the user themselves.
 * Email and role changes are admin-only operations.
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;
}
