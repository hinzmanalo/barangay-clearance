package com.barangay.clearance.settings.controller;

import com.barangay.clearance.settings.dto.BarangaySettingsDTO;
import com.barangay.clearance.settings.dto.FeeConfigDTO;
import com.barangay.clearance.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST controller for barangay settings and fee configuration.
 *
 * <p>
 * All endpoints are restricted to authenticated users; most require the
 * {@code ADMIN} role. Logo retrieval also permits {@code CLERK} access
 * so that staff can view the logo when generating documents.
 * </p>
 *
 * <p>
 * The underlying tables ({@code barangay_settings}, {@code fee_config}) are
 * singletons. There is no POST for initial creation — rows are seeded by
 * Flyway V2 and updated via PUT only.
 * </p>
 */
@Slf4j
@Tag(name = "Settings", description = "Barangay profile and fee configuration (Admin only)")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class SettingsController {

    private final SettingsService settingsService;

    // ── Barangay settings ────────────────────────────────────────────────────

    /**
     * Retrieves the current barangay settings (logo bytes excluded).
     *
     * @return barangay settings DTO with {@code hasLogo} flag
     */
    @Operation(summary = "Get barangay settings", description = "Returns the current barangay profile (name, municipality, province, captain). Logo bytes are excluded; use GET /settings/logo to retrieve the logo image.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role — ADMIN required")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BarangaySettingsDTO> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    /**
     * Updates the barangay profile fields.
     * The logo is never modified by this endpoint — use POST /settings/logo.
     *
     * @param dto updated settings values
     * @return the saved settings DTO
     */
    @Operation(summary = "Update barangay settings", description = "Updates barangay name, municipality, province, and captain name. Logo is unaffected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settings updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role — ADMIN required")
    })
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BarangaySettingsDTO> updateSettings(@Valid @RequestBody BarangaySettingsDTO dto) {
        return ResponseEntity.ok(settingsService.updateSettings(dto));
    }

    // ── Logo ─────────────────────────────────────────────────────────────────

    /**
     * Uploads or replaces the barangay logo.
     *
     * <ul>
     * <li>Accepted types: {@code image/png}, {@code image/jpeg},
     * {@code image/gif}</li>
     * <li>Maximum size: 2 MB</li>
     * </ul>
     *
     * @param file the logo image as a multipart file
     * @return 204 No Content on success
     */
    @Operation(summary = "Upload or replace barangay logo", description = "Stores a PNG/JPEG/GIF logo (max 2 MB). Replaces any existing logo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logo uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file type or file too large"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role — ADMIN required")
    })
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> uploadLogo(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.barangay.clearance.shared.exception.AppException(
                    HttpStatus.BAD_REQUEST, "Logo file must not be empty");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read logo file bytes", e);
            throw new com.barangay.clearance.shared.exception.AppException(
                    HttpStatus.BAD_REQUEST, "Failed to read uploaded file");
        }

        settingsService.uploadLogo(bytes, file.getContentType());
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the barangay logo as a raw image stream.
     * Returns 404 if no logo has been uploaded yet.
     *
     * @return raw logo bytes with appropriate {@code Content-Type} header
     */
    @Operation(summary = "Get barangay logo", description = "Streams the logo image bytes. Returns 404 if no logo has been uploaded. Accessible by ADMIN and CLERK.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logo image returned"),
            @ApiResponse(responseCode = "404", description = "No logo uploaded yet"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @GetMapping("/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
    public ResponseEntity<byte[]> getLogo() {
        SettingsService.LogoData logo = settingsService.getLogo();

        if (!logo.hasLogo()) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = logo.contentType() != null ? logo.contentType() : "image/png";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(logo.bytes());
    }

    // ── Fee config ───────────────────────────────────────────────────────────

    /**
     * Returns the current fee configuration (standard and rush fees).
     *
     * @return fee config DTO
     */
    @Operation(summary = "Get fee configuration", description = "Returns the current STANDARD and RUSH clearance fees.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fee config retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role — ADMIN required")
    })
    @GetMapping("/fees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeeConfigDTO> getFees() {
        return ResponseEntity.ok(settingsService.getFees());
    }

    /**
     * Updates the clearance fee amounts.
     * The new fees apply to all clearance requests created after this call.
     *
     * @param dto updated fee values
     * @return the saved fee config DTO
     */
    @Operation(summary = "Update fee configuration", description = "Sets new STANDARD and RUSH clearance fees. Affects all subsequent clearance requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fee config updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role — ADMIN required")
    })
    @PutMapping("/fees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeeConfigDTO> updateFees(@Valid @RequestBody FeeConfigDTO dto) {
        return ResponseEntity.ok(settingsService.updateFees(dto));
    }
}
