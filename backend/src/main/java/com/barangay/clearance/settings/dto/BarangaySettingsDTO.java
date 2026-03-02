package com.barangay.clearance.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

/**
 * DTO for {@code barangay_settings}.
 *
 * <p>
 * Binary logo data is intentionally excluded. Use {@code hasLogo} to determine
 * whether a logo has been uploaded; retrieve the logo bytes via
 * {@code GET /api/v1/settings/logo}.
 * </p>
 *
 * <p>
 * On {@code PUT /settings} the logo field in the database is never cleared —
 * upload/replace logo via {@code POST /settings/logo} only.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarangaySettingsDTO {

    private Integer id;

    @NotBlank(message = "Barangay name is required")
    @Size(max = 255, message = "Barangay name must not exceed 255 characters")
    private String barangayName;

    @NotBlank(message = "Municipality is required")
    @Size(max = 255, message = "Municipality must not exceed 255 characters")
    private String municipality;

    @NotBlank(message = "Province is required")
    @Size(max = 255, message = "Province must not exceed 255 characters")
    private String province;

    @NotBlank(message = "Captain name is required")
    @Size(max = 255, message = "Captain name must not exceed 255 characters")
    private String captainName;

    /** {@code true} if a logo image has been uploaded; {@code false} otherwise. */
    private boolean hasLogo;

    /**
     * MIME type of the uploaded logo, e.g. {@code image/png}. {@code null} if no
     * logo.
     */
    private String logoMimeType;

    private Instant updatedAt;
}
