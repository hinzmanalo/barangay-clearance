package com.barangay.clearance.payments.dto;

import com.barangay.clearance.payments.entity.Payment.PaymentMethod;
import com.barangay.clearance.payments.entity.Payment.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a {@link com.barangay.clearance.payments.entity.Payment}.
 *
 * <p>
 * The {@code idempotent} flag is {@code true} when the response was served
 * from a cached replay (i.e. the request was a duplicate within the 24-hour TTL
 * window and the original result was already {@code SUCCESS} or
 * {@code FAILED}).
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDTO {

    private UUID id;
    private UUID clearanceRequestId;
    private BigDecimal amount;
    private String idempotencyKey;
    private UUID initiatedByUserId;
    private PaymentMethod paymentMethod;
    private String provider;
    private PaymentStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * {@code true} when this response is a cached replay of a previous successful
     * or failed payment with the same idempotency key.
     */
    @Builder.Default
    private boolean idempotent = false;
}
