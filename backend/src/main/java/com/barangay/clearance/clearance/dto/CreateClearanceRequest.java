package com.barangay.clearance.clearance.dto;

import com.barangay.clearance.clearance.entity.ClearanceRequest.Purpose;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Urgency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for submitting a new clearance request (portal or walk-in).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateClearanceRequest {

    @NotNull(message = "Purpose is required")
    private Purpose purpose;

    /** Required when {@code purpose == OTHER}. */
    private String purposeOther;

    @NotNull(message = "Urgency is required")
    private Urgency urgency;

    @Min(value = 1, message = "At least 1 copy is required")
    private int copies = 1;

    private String notes;

    /**
     * Walk-in only — staff selects the resident from the registry.
     * Portal submissions resolve the resident from the JWT principal.
     */
    private java.util.UUID residentId;
}
