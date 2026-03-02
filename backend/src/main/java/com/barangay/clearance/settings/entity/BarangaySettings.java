package com.barangay.clearance.settings.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Singleton entity for barangay-wide configuration.
 *
 * <p>
 * The database enforces {@code CHECK (id = 1)}, guaranteeing exactly one row.
 * Always fetch by {@code id = 1}. For writes, use
 * {@code ON CONFLICT (id) DO UPDATE} to maintain the singleton invariant.
 * </p>
 */
@Entity
@Table(name = "barangay_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarangaySettings {

    @Id
    private Integer id;

    @Column(name = "barangay_name", nullable = false)
    private String barangayName;

    @Column(nullable = false)
    private String municipality;

    @Column(nullable = false)
    private String province;

    @Column(name = "captain_name", nullable = false)
    private String captainName;

    /** Barangay logo image bytes (PNG/JPEG). Null if no logo uploaded. */
    @Column(columnDefinition = "BYTEA")
    private byte[] logo;

    @Column(name = "logo_mime_type", length = 50)
    private String logoMimeType;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
