package com.barangay.clearance.reports.dto;

import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearancePaymentStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Purpose;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Urgency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A single row in the clearance issuance report.
 * <p>
 * Denormalises the resident's full name so the API consumer does not need a
 * separate resident lookup.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRowDTO {

    private String clearanceNumber;
    private String residentFullName;
    private Purpose purpose;
    private Urgency urgency;
    private ClearanceStatus status;
    private ClearancePaymentStatus paymentStatus;
    private Instant issuedAt;
    private Instant createdAt;
}
