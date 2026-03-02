package com.barangay.clearance.clearance.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates atomic, sequential clearance numbers per calendar month.
 *
 * <p>
 * Format: {@code YYYY-MM-NNNN} (e.g. {@code 2025-02-0001}).
 * The sequence counter is incremented via a PostgreSQL
 * {@code INSERT … ON CONFLICT DO UPDATE RETURNING} query so concurrent
 * releases never produce duplicate numbers (ADR-008).
 * </p>
 */
@Slf4j
@Service
public class ClearanceNumberService {

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final String NEXT_SEQ_SQL = """
            INSERT INTO clearance_number_sequence (year_month, last_seq)
            VALUES (:yearMonth, 1)
            ON CONFLICT (year_month)
            DO UPDATE SET last_seq = clearance_number_sequence.last_seq + 1
            RETURNING last_seq
            """;

    @PersistenceContext
    private EntityManager em;

    /**
     * Returns the next clearance number for the current calendar month.
     * Uses {@code REQUIRES_NEW} propagation so the sequence update is committed
     * independently — preventing gaps from being reused on caller rollback.
     *
     * <p>
     * Uses {@link EntityManager#createNativeQuery} directly because Spring Data
     * JPA's {@code @Modifying} annotation does not support {@code RETURNING}
     * clauses (it expects an update count, not a result set).
     * </p>
     *
     * @return formatted clearance number, e.g. {@code "2025-02-0001"}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next() {
        String yearMonth = LocalDate.now().format(YEAR_MONTH_FMT);

        Integer seq = (Integer) em.createNativeQuery(NEXT_SEQ_SQL)
                .setParameter("yearMonth", yearMonth)
                .getSingleResult();

        String number = String.format("%s-%04d", yearMonth, seq);
        log.debug("Generated clearance number: {}", number);
        return number;
    }
}
