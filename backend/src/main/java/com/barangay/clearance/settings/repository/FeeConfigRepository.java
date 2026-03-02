package com.barangay.clearance.settings.repository;

import com.barangay.clearance.settings.entity.FeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the singleton {@link FeeConfig} row.
 *
 * <p>
 * Usage: {@code findById(1)} — always returns the single fee configuration row
 * seeded by Flyway V2.
 * </p>
 */
@Repository
public interface FeeConfigRepository extends JpaRepository<FeeConfig, Integer> {
}
