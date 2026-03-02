package com.barangay.clearance.payments.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stub payment gateway for development and testing.
 *
 * <p>
 * Active when {@code payment.provider=stub} (default). Uses
 * {@link ThreadLocalRandom} for thread safety. Configurable via
 * {@code payment.stub.always-success} (defaults to {@code true} so that
 * the happy path works out-of-the-box in local dev).
 * </p>
 *
 * @see ConditionalOnProperty
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "stub", matchIfMissing = true)
public class StubPaymentGateway implements PaymentGateway {

    @Value("${payment.stub.always-success:true}")
    private boolean alwaysSuccess;

    /**
     * Simulates a payment by optionally randomising success/failure.
     * When {@code payment.stub.always-success=true} (the default) this always
     * returns a successful result.
     *
     * @param request the payment request parameters
     * @return a stub result indicating success or failure
     */
    @Override
    public PaymentResult initiate(PaymentRequest request) {
        boolean success = alwaysSuccess || ThreadLocalRandom.current().nextBoolean();
        // Build a recognisable reference from the clearance request ID
        String ref = "STUB-" + request.clearanceRequestId().toString().replace("-", "").substring(0, 12).toUpperCase();
        log.info("StubPaymentGateway.initiate: clearanceId={} idempotencyKey={} success={}",
                request.clearanceRequestId(), request.idempotencyKey(), success);
        return new PaymentResult(
                success,
                ref,
                success ? "Stub payment authorised" : "Stub payment declined");
    }

    @Override
    public String getProviderCode() {
        return "STUB";
    }
}
