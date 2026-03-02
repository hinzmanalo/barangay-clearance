package com.barangay.clearance.payments.gateway;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable value object carrying all data required to initiate a payment.
 *
 * @param clearanceRequestId the clearance being paid for
 * @param userId             the user initiating the payment
 * @param amount             the exact charge amount
 * @param idempotencyKey     client-supplied UUID v4 for deduplication
 */
public record PaymentRequest(
        UUID clearanceRequestId,
        UUID userId,
        BigDecimal amount,
        String idempotencyKey) {
}
