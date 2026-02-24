package com.barangay.clearance.clearance.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tracks the per-month sequence counter used to generate clearance numbers.
 * <p>
 * Primary key is {@code year_month} (e.g. {@code "2025-02"}).
 * The {@code lastSeq} is incremented atomically via a native
 * {@code ON CONFLICT DO UPDATE RETURNING} query to prevent duplicates under
 * concurrent load.
 * </p>
 */
@Entity
@Table(name = "clearance_number_sequence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearanceNumberSequence {

    /**
     * Year-month key in the format {@code YYYY-MM} (e.g. {@code "2025-02"}).
     */
    @Id
    @Column(name = "year_month", length = 7, nullable = false, updatable = false)
    private String yearMonth;

    /** Last used sequence number for this month. */
    @Column(name = "last_seq", nullable = false)
    private Integer lastSeq;
}
