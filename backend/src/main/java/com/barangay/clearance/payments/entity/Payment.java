package com.barangay.clearance.payments.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single payment attempt for a clearance request.
 *
 * <p>
 * Idempotency is enforced via a composite unique index on
 * {@code (idempotency_key, initiated_by_user_id)}. Duplicate submissions
 * within the 24-hour TTL window are detected by querying with
 * {@code idempotency_expires_at > now()}.
 * </p>
 *
 * <p>
 * Status lifecycle: {@code PENDING → SUCCESS | FAILED}
 * </p>
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The clearance request this payment belongs to. */
    @Column(name = "clearance_request_id", nullable = false)
    private UUID clearanceRequestId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Client-supplied UUID v4; forms the idempotency window together with
     * {@code initiatedByUserId}.
     */
    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    /** User (resident or clerk) who initiated this payment. */
    @Column(name = "initiated_by_user_id", nullable = false)
    private UUID initiatedByUserId;

    @Column(name = "payment_method", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.STUB;

    /**
     * Short code matching the
     * {@link com.barangay.clearance.payments.gateway.PaymentGateway} provider.
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "STUB";

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * JSON-serialized {@link com.barangay.clearance.payments.dto.PaymentDTO} of the
     * final result.
     * Populated on terminal status (SUCCESS or FAILED) for idempotent replay.
     */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    /**
     * The instant after which this idempotency key is considered expired (default:
     * 24 h).
     */
    @Column(name = "idempotency_expires_at", nullable = false)
    private Instant idempotencyExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ─────────────────────────────────────────────────────────────────
    // Nested enums
    // ─────────────────────────────────────────────────────────────────

    /** Payment processing result. */
    public enum PaymentStatus {
        PENDING,
        SUCCESS,
        FAILED
    }

    /** How the payment was collected. */
    public enum PaymentMethod {
        /** Via a payment gateway (stub or real). */
        STUB,
        /** Cash collected in-person by a clerk. */
        CASH
    }

    /** Identifies the payment provider. */
    public enum PaymentProvider {
        STUB
    }
}
