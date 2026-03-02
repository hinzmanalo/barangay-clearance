package com.barangay.clearance.payments.gateway;

/**
 * Immutable result returned by a {@link PaymentGateway} after attempting
 * to process a payment.
 *
 * @param success           {@code true} if the payment was authorised
 * @param providerReference provider-assigned reference/transaction ID
 * @param message           human-readable description from the provider
 */
public record PaymentResult(
        boolean success,
        String providerReference,
        String message) {
}
