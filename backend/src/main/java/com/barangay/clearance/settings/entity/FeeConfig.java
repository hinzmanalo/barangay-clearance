package com.barangay.clearance.settings.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Singleton entity for clearance fee configuration.
 *
 * <p>
 * The database enforces {@code CHECK (id = 1)}, guaranteeing exactly one row.
 * Always fetch by {@code id = 1}. Seeded by Flyway V2.
 * </p>
 *
 * <ul>
 * <li>{@code standardFee} — fee for STANDARD urgency clearance requests</li>
 * <li>{@code rushFee} — fee for RUSH urgency clearance requests</li>
 * </ul>
 */
@Entity
@Table(name = "fee_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeConfig {

    @Id
    private Integer id;

    @Column(name = "standard_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal standardFee;

    @Column(name = "rush_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal rushFee;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
