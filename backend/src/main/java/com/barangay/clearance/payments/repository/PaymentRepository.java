package com.barangay.clearance.payments.repository;

import com.barangay.clearance.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Payment} entities.
 *
 * <p>
 * The idempotency lookup uses {@code idempotencyExpiresAt} to enforce a sliding
 * 24-hour window per {@code (idempotencyKey, initiatedByUserId)} combination.
 * </p>
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Finds an active (non-expired) payment record matching the idempotency key
     * and the user who initiated it.
     *
     * @param idempotencyKey    the client-supplied idempotency key
     * @param initiatedByUserId the user who submitted the payment
     * @param now               current instant — only records with
     *                          {@code idempotencyExpiresAt > now} are returned
     * @return the matching payment if found within the TTL window
     */
    Optional<Payment> findByIdempotencyKeyAndInitiatedByUserIdAndIdempotencyExpiresAtAfter(
            String idempotencyKey, UUID initiatedByUserId, Instant now);

    /**
     * Returns all payments associated with a clearance request, ordered by
     * creation time descending.
     *
     * @param clearanceRequestId the clearance request UUID
     * @return list of payments (may be empty)
     */
    List<Payment> findByClearanceRequestIdOrderByCreatedAtDesc(UUID clearanceRequestId);
}
