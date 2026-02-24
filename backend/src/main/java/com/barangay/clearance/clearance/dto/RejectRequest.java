package com.barangay.clearance.clearance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for rejecting a clearance request.
 * A non-blank reason is required.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RejectRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
