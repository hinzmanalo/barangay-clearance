package com.barangay.clearance.clearance.repository;

import com.barangay.clearance.clearance.entity.ClearanceRequest;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClearanceRequestRepository extends JpaRepository<ClearanceRequest, UUID>,
        JpaSpecificationExecutor<ClearanceRequest> {

    /**
     * Portal scoping — returns only requests belonging to the given resident's
     * portal user.
     */
    Page<ClearanceRequest> findByResidentId(UUID residentId, Pageable pageable);

    /**
     * Dashboard summary — count by status.
     */
    long countByStatus(ClearanceStatus status);

    /**
     * Count requests released today (issued_at on current day).
     */
    @Query("""
            SELECT COUNT(cr) FROM ClearanceRequest cr
            WHERE cr.status = 'RELEASED'
              AND cr.issuedAt >= :startOfDay
              AND cr.issuedAt < :endOfDay
            """)
    long countReleasedToday(@Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);

    /**
     * Find a single clearance owned by the given resident (for portal ownership
     * check).
     */
    Optional<ClearanceRequest> findByIdAndResidentId(UUID id, UUID residentId);
}
