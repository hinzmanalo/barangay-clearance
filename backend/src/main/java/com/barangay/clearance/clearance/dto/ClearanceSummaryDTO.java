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

    /**
     * Requests currently in FOR_APPROVAL state waiting for a clerk/approver
     * decision.
     */
    private long pendingApproval;

    /** Requests that have been approved (APPROVED status, any payment state). */
    private long approved;

    /** Approved requests still awaiting payment (APPROVED + UNPAID). */
    private long awaitingPayment;

    /** Clearances issued (RELEASED) on the current UTC day. */
    private long releasedToday;
}
