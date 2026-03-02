package com.barangay.clearance.reports.controller;

import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearancePaymentStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Purpose;
import com.barangay.clearance.reports.dto.ReportRowDTO;
import com.barangay.clearance.reports.service.ReportsService;
import com.barangay.clearance.shared.exception.AppException;
import com.barangay.clearance.shared.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Reporting endpoints — filterable clearance issuance log.
 * <p>
 * Accessible by {@code CLERK} and {@code ADMIN} roles only.
 * A {@code RESIDENT} token will receive a 403.
 * </p>
 */
@Slf4j
@Tag(name = "Reports", description = "Clearance issuance reports and logs")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class ReportsController {

    private final ReportsService reportsService;

    /**
     * Returns a paginated, filterable list of clearance issuances for use as a
     * digital log-book replacement.
     *
     * @param status        optional {@link ClearanceStatus} filter
     * @param paymentStatus optional {@link ClearancePaymentStatus} filter
     * @param purpose       optional {@link Purpose} filter
     * @param purok         optional partial address/purok string (case-insensitive)
     * @param from          optional inclusive start date ({@code yyyy-MM-dd})
     * @param to            optional inclusive end date ({@code yyyy-MM-dd})
     * @param pageable      pagination — default page size 20, sorted by
     *                      {@code issuedAt} DESC
     * @return paginated report rows
     */
    @Operation(summary = "Clearance issuance report", description = "Filterable, paginated clearance log. All query parameters are optional. "
            + "Date range filters on issued_at. Accessible by CLERK and ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated report rows"),
            @ApiResponse(responseCode = "400", description = "Invalid enum or date parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — missing or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden — RESIDENT role not allowed")
    })
    @GetMapping("/clearances")
    @PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")
    public ResponseEntity<PageResponse<ReportRowDTO>> getClearanceReport(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String purok,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        ClearanceStatus statusEnum = parseEnum(ClearanceStatus.class, "status", status);
        ClearancePaymentStatus pmtEnum = parseEnum(ClearancePaymentStatus.class, "paymentStatus", paymentStatus);
        Purpose purposeEnum = parseEnum(Purpose.class, "purpose", purpose);

        PageResponse<ReportRowDTO> report = reportsService.getReport(
                statusEnum, pmtEnum, purposeEnum, purok, from, to, pageable);

        return ResponseEntity.ok(report);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Parses a nullable string into the requested enum type.
     * Returns {@code null} when {@code value} is null or blank.
     * Throws {@link AppException} (400) for unrecognised values.
     */
    private <E extends Enum<E>> E parseEnum(Class<E> type, String paramName, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid enum value for param '{}': {}", paramName, value);
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Invalid value for '" + paramName + "': " + value);
        }
    }
}
