package com.barangay.clearance.reports.service;

import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearancePaymentStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Purpose;
import com.barangay.clearance.reports.dto.ReportRowDTO;
import com.barangay.clearance.reports.dto.ReportRowProjection;
import com.barangay.clearance.reports.repository.ReportRepository;
import com.barangay.clearance.shared.util.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Provides filtered, paginated clearance issuance reports.
 * <p>
 * All filter parameters are optional (nullable). Passing {@code null} for a
 * parameter disables that filter clause so the query returns all records for
 * that dimension.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportsService {

    private final ReportRepository reportRepository;

    /**
     * Retrieves a paginated report of clearance requests with optional filtering.
     *
     * @param status        optional clearance status filter
     * @param paymentStatus optional payment status filter
     * @param purpose       optional purpose filter
     * @param purok         optional partial address/purok match (case-insensitive)
     * @param from          optional start date (inclusive, local date)
     * @param to            optional end date (inclusive, local date — converted to
     *                      exclusive end-of-day Instant internally)
     * @param pageable      pagination and sort
     * @return paginated report rows
     */
    @Transactional(readOnly = true)
    public PageResponse<ReportRowDTO> getReport(
            ClearanceStatus status,
            ClearancePaymentStatus paymentStatus,
            Purpose purpose,
            String purok,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        // Convert nullable LocalDate → Instant at system timezone boundaries.
        // 'to' is shifted +1 day so it acts as an exclusive upper bound,
        // capturing all records up to 23:59:59.999 of the requested end date.
        Instant fromInstant = from != null
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;
        Instant toInstant = to != null
                ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        String statusStr = status != null ? status.name() : null;
        String paymentStatusStr = paymentStatus != null ? paymentStatus.name() : null;
        String purposeStr = purpose != null ? purpose.name() : null;

        log.info("REPORT_QUERY status={} paymentStatus={} purpose={} purok={} from={} to={}",
                statusStr, paymentStatusStr, purposeStr, purok, from, to);

        Page<ReportRowProjection> page = reportRepository.findReportRows(
                statusStr, paymentStatusStr, purposeStr,
                purok, fromInstant, toInstant,
                pageable);

        Page<ReportRowDTO> mapped = page.map(this::toDTO);
        return PageResponse.of(mapped);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private ReportRowDTO toDTO(ReportRowProjection p) {
        return ReportRowDTO.builder()
                .clearanceNumber(p.getClearanceNumber())
                .residentFullName(p.getResidentFullName())
                .purpose(p.getPurpose() != null
                        ? Purpose.valueOf(p.getPurpose())
                        : null)
                .urgency(p.getUrgency() != null
                        ? com.barangay.clearance.clearance.entity.ClearanceRequest.Urgency.valueOf(p.getUrgency())
                        : null)
                .status(p.getStatus() != null
                        ? ClearanceStatus.valueOf(p.getStatus())
                        : null)
                .paymentStatus(p.getPaymentStatus() != null
                        ? ClearancePaymentStatus.valueOf(p.getPaymentStatus())
                        : null)
                .issuedAt(p.getIssuedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
