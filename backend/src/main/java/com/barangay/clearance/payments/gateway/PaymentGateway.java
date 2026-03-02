package com.barangay.clearance.payments.gateway;

/**
 * Strategy interface for payment providers.
 *
 * <p>
 * Implementations are selected at startup via
 * {@code @ConditionalOnProperty(name = "payment.provider", havingValue = "...")}
 * and must be thread-safe.
 * </p>
 */
public interface PaymentGateway {

    /**
     * Initiates a payment with the given provider.
     *
     * @param request the payment parameters
     * @return the provider's result; never {@code null}
     */
    PaymentResult initiate(PaymentRequest request);

    /**
     * Returns the short provider code stored in the {@code provider} column
     * (e.g. {@code "STUB"}).
     *
     * @return provider code string
     */
    String getProviderCode();
}
