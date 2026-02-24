package com.barangay.clearance.clearance.repository;

import com.barangay.clearance.clearance.entity.ClearanceNumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClearanceNumberSequenceRepository extends JpaRepository<ClearanceNumberSequence, String> {

    /**
     * Atomically increments (or initialises) the sequence for the given month and
     * returns the new {@code last_seq} value.
     *
     * <p>
     * Uses a PostgreSQL {@code INSERT … ON CONFLICT DO UPDATE RETURNING} so that
     * concurrent releases in the same month never produce duplicate numbers.
     * </p>
     *
     * @param yearMonth the key in the format {@code YYYY-MM} (e.g.
     *                  {@code "2025-02"})
     * @return the next sequence number (1-based)
     */
    @Modifying
    @Query(value = """
            INSERT INTO clearance_number_sequence (year_month, last_seq)
            VALUES (:yearMonth, 1)
            ON CONFLICT (year_month)
            DO UPDATE SET last_seq = clearance_number_sequence.last_seq + 1
            RETURNING last_seq
            """, nativeQuery = true)
    Integer nextSequence(@Param("yearMonth") String yearMonth);
}
