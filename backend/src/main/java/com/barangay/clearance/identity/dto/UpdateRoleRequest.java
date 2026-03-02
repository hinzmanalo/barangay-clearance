package com.barangay.clearance.identity.dto;

import com.barangay.clearance.identity.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for changing a staff user's role.
 * The role must not be RESIDENT — resident accounts are managed separately.
 */
@Data
public class UpdateRoleRequest {

    @NotNull(message = "Role is required")
    private User.Role role;
}
