package com.barangay.clearance.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for an admin-triggered password reset.
 * The new password must meet minimum strength requirements.
 * After reset, {@code mustChangePassword} is set to {@code true} on the target
 * user.
 */
@Data
public class AdminResetPasswordRequest {

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
    @Pattern(regexp = ".*[0-9].*", message = "Password must contain at least one digit")
    private String newPassword;
}
