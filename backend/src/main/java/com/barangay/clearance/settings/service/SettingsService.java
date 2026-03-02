package com.barangay.clearance.settings.service;

import com.barangay.clearance.settings.dto.BarangaySettingsDTO;
import com.barangay.clearance.settings.dto.FeeConfigDTO;
import com.barangay.clearance.settings.entity.BarangaySettings;
import com.barangay.clearance.settings.entity.FeeConfig;
import com.barangay.clearance.settings.repository.BarangaySettingsRepository;
import com.barangay.clearance.settings.repository.FeeConfigRepository;
import com.barangay.clearance.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for barangay settings and fee configuration.
 *
 * <p>
 * Both {@code barangay_settings} and {@code fee_config} are singleton tables
 * (enforced by {@code CHECK (id = 1)} at the DB level and seeded by Flyway V2).
 * Reads always use {@code findById(1)}. Writes never insert — only update the
 * existing row.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    /** Accepted MIME types for logo images. */
    private static final List<String> ACCEPTED_LOGO_TYPES = List.of(
            "image/png", "image/jpeg", "image/gif");

    /** Maximum logo file size: 2 MB. */
    private static final long MAX_LOGO_SIZE_BYTES = 2L * 1024 * 1024;

    private final BarangaySettingsRepository settingsRepo;
    private final FeeConfigRepository feeConfigRepo;

    // ── Barangay settings ────────────────────────────────────────────────────

    /**
     * Returns the singleton {@link BarangaySettings} row (id = 1).
     *
     * @return current barangay settings DTO (logo bytes excluded)
     * @throws IllegalStateException if the settings row was not seeded by Flyway V2
     */
    @Transactional(readOnly = true)
    public BarangaySettingsDTO getSettings() {
        BarangaySettings settings = loadSettings();
        return toDTO(settings);
    }

    /**
     * Updates editable fields on the singleton settings row.
     * The logo is never cleared or overwritten by this method.
     *
     * @param dto updated settings values
     * @return the updated settings DTO
     */
    @Transactional
    public BarangaySettingsDTO updateSettings(BarangaySettingsDTO dto) {
        BarangaySettings settings = loadSettings();

        settings.setBarangayName(dto.getBarangayName());
        settings.setMunicipality(dto.getMunicipality());
        settings.setProvince(dto.getProvince());
        settings.setCaptainName(dto.getCaptainName());
        // Do NOT overwrite logo fields — use uploadLogo() for that.

        BarangaySettings saved = settingsRepo.save(settings);
        log.info("Barangay settings updated: barangayName={}", saved.getBarangayName());
        return toDTO(saved);
    }

    /**
     * Stores a logo image on the singleton settings row.
     *
     * @param bytes       raw image bytes
     * @param contentType MIME type (must be image/png, image/jpeg, or image/gif)
     * @throws AppException 400 if the content type is not an accepted image type
     * @throws AppException 400 if the file exceeds 2 MB
     */
    @Transactional
    public void uploadLogo(byte[] bytes, String contentType) {
        validateLogo(bytes, contentType);

        BarangaySettings settings = loadSettings();
        settings.setLogo(bytes);
        settings.setLogoMimeType(contentType);
        settingsRepo.save(settings);

        log.info("Logo uploaded: contentType={}, size={} bytes", contentType, bytes.length);
    }

    /**
     * Retrieves the raw logo bytes and MIME type for streaming to the client.
     *
     * @return logo data container, or {@code null} values if no logo has been
     *         uploaded
     */
    @Transactional(readOnly = true)
    public LogoData getLogo() {
        BarangaySettings settings = loadSettings();
        return new LogoData(settings.getLogo(), settings.getLogoMimeType());
    }

    /**
     * Returns the raw {@link BarangaySettings} entity (used by PDF generation).
     *
     * @return the singleton settings entity
     */
    @Transactional(readOnly = true)
    public BarangaySettings getSettingsEntity() {
        return loadSettings();
    }

    // ── Fee config ───────────────────────────────────────────────────────────

    /**
     * Returns the singleton {@link FeeConfig} row (id = 1).
     *
     * @return current fee configuration DTO
     * @throws IllegalStateException if the fee_config row was not seeded by Flyway
     *                               V2
     */
    @Transactional(readOnly = true)
    public FeeConfigDTO getFees() {
        FeeConfig config = loadFees();
        return toDTO(config);
    }

    /**
     * Updates the standard and rush fees on the singleton fee_config row.
     *
     * @param dto new fee values
     * @return updated fee configuration DTO
     */
    @Transactional
    public FeeConfigDTO updateFees(FeeConfigDTO dto) {
        FeeConfig config = loadFees();

        config.setStandardFee(dto.getStandardFee());
        config.setRushFee(dto.getRushFee());

        FeeConfig saved = feeConfigRepo.save(config);
        log.info("Fee config updated: standardFee={}, rushFee={}", saved.getStandardFee(), saved.getRushFee());
        return toDTO(saved);
    }

    /**
     * Returns the raw {@link FeeConfig} entity (used by clearance service).
     *
     * @return the singleton fee config entity
     */
    @Transactional(readOnly = true)
    public FeeConfig getFeeConfigEntity() {
        return loadFees();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private BarangaySettings loadSettings() {
        return settingsRepo.findById(1)
                .orElseThrow(() -> new IllegalStateException(
                        "barangay_settings row (id=1) not found — ensure Flyway V2 migration ran"));
    }

    private FeeConfig loadFees() {
        return feeConfigRepo.findById(1)
                .orElseThrow(() -> new IllegalStateException(
                        "fee_config row (id=1) not found — ensure Flyway V2 migration ran"));
    }

    private void validateLogo(byte[] bytes, String contentType) {
        if (contentType == null || !ACCEPTED_LOGO_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Invalid logo file type '" + contentType + "'. Accepted types: image/png, image/jpeg, image/gif");
        }
        if (bytes == null || bytes.length == 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Logo file must not be empty");
        }
        if (bytes.length > MAX_LOGO_SIZE_BYTES) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Logo file too large (" + bytes.length + " bytes). Maximum size is 2 MB");
        }
    }

    private BarangaySettingsDTO toDTO(BarangaySettings s) {
        return BarangaySettingsDTO.builder()
                .id(s.getId())
                .barangayName(s.getBarangayName())
                .municipality(s.getMunicipality())
                .province(s.getProvince())
                .captainName(s.getCaptainName())
                .hasLogo(s.getLogo() != null && s.getLogo().length > 0)
                .logoMimeType(s.getLogoMimeType())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private FeeConfigDTO toDTO(FeeConfig c) {
        return FeeConfigDTO.builder()
                .id(c.getId())
                .standardFee(c.getStandardFee())
                .rushFee(c.getRushFee())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    /**
     * Simple value container for logo bytes and MIME type.
     *
     * @param bytes       raw image bytes; {@code null} if no logo uploaded
     * @param contentType MIME type; {@code null} if no logo uploaded
     */
    public record LogoData(byte[] bytes, String contentType) {
        public boolean hasLogo() {
            return bytes != null && bytes.length > 0;
        }
    }
}
