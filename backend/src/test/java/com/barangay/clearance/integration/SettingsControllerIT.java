package com.barangay.clearance.integration;

import com.barangay.clearance.settings.dto.BarangaySettingsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@code /api/v1/settings/**} endpoints.
 *
 * <p>
 * The {@code barangay_settings} singleton row is seeded by Flyway V2 and is
 * preserved between tests (only application data tables are truncated). Each
 * test restores a known baseline value to avoid ordering dependencies.
 * </p>
 */
class SettingsControllerIT extends BaseIntegrationTest {

    private static final String BASE = "/api/v1/settings";

    @BeforeEach
    void setUp() {
        // Only application data tables — settings/fee_config rows are preserved.
        truncateAllTables();
        // Reset settings to the Flyway-seeded baseline so tests are independent.
        resetSettingsToDefaults();
    }

    // ── Test 1: GET settings as ADMIN ─────────────────────────────────────────

    /**
     * ADMIN can retrieve the barangay settings including the required fields.
     */
    @Test
    void getSettings_asAdmin_returns200WithRequiredFields() throws Exception {
        performGet(BASE, asAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barangayName").isNotEmpty())
                .andExpect(jsonPath("$.captainName").isNotEmpty())
                .andExpect(jsonPath("$.municipality").isNotEmpty())
                .andExpect(jsonPath("$.province").isNotEmpty());
    }

    // ── Test 2: PUT settings as ADMIN then GET returns updated values ─────────

    /**
     * Updating barangay settings as ADMIN persists the changes, which are
     * visible on a subsequent GET.
     */
    @Test
    void updateSettings_asAdmin_subsequentGetReturnsUpdatedValues() throws Exception {
        BarangaySettingsDTO update = BarangaySettingsDTO.builder()
                .barangayName("Barangay Mabuhay")
                .municipality("City of Test")
                .province("Province of Testing")
                .captainName("Captain Updated")
                .build();

        performPut(BASE, update, asAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barangayName", is("Barangay Mabuhay")));

        performGet(BASE, asAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barangayName", is("Barangay Mabuhay")))
                .andExpect(jsonPath("$.captainName", is("Captain Updated")));
    }

    // ── Test 3: Upload valid PNG logo as ADMIN returns 204 ────────────────────

    /**
     * Uploading a valid PNG image as ADMIN returns 204 No Content.
     */
    @Test
    void uploadLogo_validPng_asAdmin_returns204() throws Exception {
        // Minimal PNG: header magic + minimal IDAT — service only checks MIME type
        MockMultipartFile logo = new MockMultipartFile(
                "file", "logo.png", "image/png", createMinimalPngBytes());

        performMultipart(BASE + "/logo", logo, asAdmin())
                .andExpect(status().isNoContent());
    }

    // ── Test 4: Upload oversized file returns 400 ────────────────────────────

    /**
     * Uploading a file larger than 2 MB returns 400 (service-level size check).
     */
    @Test
    void uploadLogo_oversizedFile_returns400() throws Exception {
        // 2 MB + 1 byte exceeds the SettingsService limit
        byte[] oversized = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile bigFile = new MockMultipartFile(
                "file", "big-logo.png", "image/png", oversized);

        performMultipart(BASE + "/logo", bigFile, asAdmin())
                .andExpect(status().isBadRequest());
    }

    // ── Test 5: Upload non-image MIME type returns 400 ───────────────────────

    /**
     * Uploading a file with a non-image MIME type returns 400 (service-level
     * MIME type check).
     */
    @Test
    void uploadLogo_nonImageMimeType_returns400() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "document.pdf", "application/pdf",
                "%PDF-1.4 test content".getBytes());

        performMultipart(BASE + "/logo", pdf, asAdmin())
                .andExpect(status().isBadRequest());
    }

    // ── Test 6: PUT settings as CLERK returns 403 ────────────────────────────

    /**
     * A CLERK role cannot update barangay settings — must receive 403.
     */
    @Test
    void updateSettings_asClerk_returns403() throws Exception {
        BarangaySettingsDTO update = BarangaySettingsDTO.builder()
                .barangayName("Unauthorized Update")
                .municipality("City of Test")
                .province("Province of Testing")
                .captainName("Some Captain")
                .build();

        performPut(BASE, update, asClerk())
                .andExpect(status().isForbidden());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Resets the singleton {@code barangay_settings} row to the Flyway V2
     * baseline values so tests always start from a known state.
     */
    private void resetSettingsToDefaults() {
        jdbcTemplate.update(
                "UPDATE barangay_settings SET barangay_name = ?, municipality = ?, " +
                        "province = ?, captain_name = ?, logo = NULL, logo_mime_type = NULL " +
                        "WHERE id = 1",
                "Barangay San Jose",
                "Municipality of Sample",
                "Province of Sample",
                "Juan dela Cruz");
    }

    /**
     * Returns minimal bytes that satisfy the {@code image/png} MIME type check.
     * The service only validates MIME type, not actual PNG structure.
     */
    private byte[] createMinimalPngBytes() {
        // PNG magic header signature (8 bytes)
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
    }
}
