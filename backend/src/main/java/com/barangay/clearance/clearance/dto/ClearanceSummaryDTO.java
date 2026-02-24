package com.barangay.clearance.clearance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Backoffice dashboard summary counts.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClearanceSummaryDTO {

    private long pendingApproval;
    private long approvedAwaitingPayment;
    private long releasedToday;
}
