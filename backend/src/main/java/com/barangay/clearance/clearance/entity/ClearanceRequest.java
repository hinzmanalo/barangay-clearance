package com.barangay.clearance.clearance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Core entity representing a barangay clearance request.
 * <p>
 * Follows a strict state machine:
 * 
 * <pre>
 *   DRAFT → FOR_APPROVAL → APPROVED → (PAID) → RELEASED
 *                        ↘ REJECTED → DRAFT (resident resubmits)
 * </pre>
 * 
 * Payment status is tracked separately (UNPAID → PAID → WAIVED).
 * The clearance number is assigned only at release.
 * </p>
 */
@Entity
@Table(name = "clearance_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Assigned only at release — format: YYYY-MM-NNNN (e.g. 2025-02-0001). */
    @Column(name = "clearance_number", length = 12)
    private String clearanceNumber;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    /**
     * The portal user (RESIDENT) or clerk (CLERK/ADMIN) who created this request.
     */
    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Purpose purpose;

    /** Free-text description when {@code purpose == OTHER}. */
    @Column(name = "purpose_other", length = 255)
    private String purposeOther;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Urgency urgency = Urgency.STANDARD;

    @Column(name = "fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeAmount;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    @Builder.Default
    private Integer copies = 1;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ClearanceStatus status = ClearanceStatus.DRAFT;

    @Column(name = "payment_status", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ClearancePaymentStatus paymentStatus = ClearancePaymentStatus.UNPAID;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Staff user who approved/rejected this request. */
    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Timestamp when the clearance was issued/released. */
    @Column(name = "issued_at")
    private Instant issuedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum ClearanceStatus {
        DRAFT,
        FOR_APPROVAL,
        APPROVED,
        REJECTED,
        RELEASED
    }

    public enum ClearancePaymentStatus {
        UNPAID,
        PAID,
        WAIVED
    }

    public enum Purpose {
        EMPLOYMENT,
        TRAVEL_ABROAD,
        SCHOLARSHIP,
        LOAN,
        BUSINESS_PERMIT,
        LEGAL,
        CEDULA,
        OTHER
    }

    public enum Urgency {
        STANDARD,
        RUSH
    }
}
