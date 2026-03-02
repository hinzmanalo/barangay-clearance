package com.barangay.clearance.settings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for {@code fee_config}.
 *
 * <p>
 * Contains fee amounts for STANDARD and RUSH urgency clearance requests.
 * These values are read at clearance-request creation time to set the
 * {@code fee_amount} on the clearance record.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeConfigDTO {

    private Integer id;

    @NotNull(message = "Standard fee is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Standard fee must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Standard fee must have at most 8 integer digits and 2 decimal places")
    private BigDecimal standardFee;

    @NotNull(message = "Rush fee is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Rush fee must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Rush fee must have at most 8 integer digits and 2 decimal places")
    private BigDecimal rushFee;

    private Instant updatedAt;
}
