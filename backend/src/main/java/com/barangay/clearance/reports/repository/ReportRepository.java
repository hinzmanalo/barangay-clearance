package com.barangay.clearance.reports.repository;

import com.barangay.clearance.clearance.entity.ClearanceRequest;
import com.barangay.clearance.reports.dto.ReportRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only reporting repository for the clearance issuance log.
 * <p>
 * Uses a native SQL query to join {@code clearance_requests} with
 * {@code residents} without introducing a cross-module JPA relationship. All
 * filter parameters are nullable — passing {@code null} disables the
 * corresponding filter clause.
 * </p>
 */
@Repository
public interface ReportRepository extends JpaRepository<ClearanceRequest, UUID> {

    /**
     * Returns a paginated, filtered report of clearance requests joined with
     * resident names.
     *
     * @param status        clearance status filter (nullable)
     * @param paymentStatus payment status filter (nullable)
     * @param purpose       purpose filter (nullable)
     * @param purok         partial address/purok match (nullable)
     * @param fromDate      include only rows where issued_at &ge; fromDate
     *                      (nullable)
     * @param toDate        include only rows where issued_at &lt; toDate (nullable)
     * @param pageable      pagination and sort
     * @return page of {@link ReportRowProjection}
     */
    @Query(value = """
            SELECT
                cr.clearance_number                           AS clearanceNumber,
                r.first_name || ' ' || r.last_name           AS residentFullName,
                cr.purpose                                    AS purpose,
                cr.urgency                                    AS urgency,
                cr.status                                     AS status,
                cr.payment_status                             AS paymentStatus,
                cr.issued_at                                  AS issuedAt,
                cr.created_at                                 AS createdAt
            FROM clearance_requests cr
            JOIN residents r ON cr.resident_id = r.id
            WHERE (CAST(:status AS TEXT) IS NULL        OR cr.status         = :status)
              AND (CAST(:paymentStatus AS TEXT) IS NULL OR cr.payment_status = :paymentStatus)
              AND (CAST(:purpose AS TEXT) IS NULL       OR cr.purpose        = :purpose)
              AND (CAST(:purok AS TEXT) IS NULL         OR LOWER(r.address)  LIKE LOWER(CONCAT('%', :purok, '%')))
              AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR cr.issued_at   >= :fromDate)
              AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL   OR cr.issued_at   <  :toDate)
            ORDER BY cr.issued_at DESC NULLS LAST, cr.created_at DESC
            """, countQuery = """
            SELECT COUNT(*)
            FROM clearance_requests cr
            JOIN residents r ON cr.resident_id = r.id
            WHERE (CAST(:status AS TEXT) IS NULL        OR cr.status         = :status)
              AND (CAST(:paymentStatus AS TEXT) IS NULL OR cr.payment_status = :paymentStatus)
              AND (CAST(:purpose AS TEXT) IS NULL       OR cr.purpose        = :purpose)
              AND (CAST(:purok AS TEXT) IS NULL         OR LOWER(r.address)  LIKE LOWER(CONCAT('%', :purok, '%')))
              AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR cr.issued_at   >= :fromDate)
              AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL   OR cr.issued_at   <  :toDate)
            """, nativeQuery = true)
    Page<ReportRowProjection> findReportRows(
            @Param("status") String status,
            @Param("paymentStatus") String paymentStatus,
            @Param("purpose") String purpose,
            @Param("purok") String purok,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
