package com.barangay.clearance.clearance.dto;

import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearancePaymentStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Purpose;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Urgency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a clearance request.
 * Includes denormalised resident name fields for convenient display.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClearanceRequestDTO {

    private UUID id;
    private String clearanceNumber;

    private UUID residentId;
    /** Denormalised from the Resident entity — last name, first name. */
    private String residentName;

    private UUID requestedBy;

    private Purpose purpose;
    private String purposeOther;
    private Urgency urgency;
    private BigDecimal feeAmount;
    private Integer copies;
    private ClearanceStatus status;
    private ClearancePaymentStatus paymentStatus;
    private String notes;

    private UUID reviewedBy;
    private Instant reviewedAt;
    private Instant issuedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
