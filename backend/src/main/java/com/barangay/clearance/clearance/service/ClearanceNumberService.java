package com.barangay.clearance.clearance.service;

import com.barangay.clearance.clearance.repository.ClearanceNumberSequenceRepository;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ClearanceNumberService {

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ClearanceNumberSequenceRepository sequenceRepository;

    /**
     * Returns the next clearance number for the current calendar month.
     * Must be called inside a transaction (uses {@code REQUIRES_NEW} to ensure
     * the sequence update is committed even if the caller rolls back — preventing
     * gaps from being reused).
     *
     * @return formatted clearance number, e.g. {@code "2025-02-0001"}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next() {
        String yearMonth = LocalDate.now().format(YEAR_MONTH_FMT);
        Integer seq = sequenceRepository.nextSequence(yearMonth);
        String number = String.format("%s-%04d", yearMonth, seq);
        log.debug("Generated clearance number: {}", number);
        return number;
    }
}
