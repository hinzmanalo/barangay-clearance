package com.barangay.clearance.reports.dto;

import java.time.Instant;

/**
 * Spring Data native-query projection for the report query result.
 * <p>
 * Each getter maps to a column alias in the native SQL query in
 * {@code ReportRepository}.
 * </p>
 */
public interface ReportRowProjection {

    String getClearanceNumber();

    String getResidentFullName();

    String getPurpose();

    String getUrgency();

    String getStatus();

    String getPaymentStatus();

    Instant getIssuedAt();

    Instant getCreatedAt();
}
