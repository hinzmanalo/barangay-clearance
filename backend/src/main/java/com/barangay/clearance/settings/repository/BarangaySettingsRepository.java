package com.barangay.clearance.settings.repository;

import com.barangay.clearance.settings.entity.BarangaySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the singleton {@link BarangaySettings} row.
 *
 * <p>
 * Usage: {@code findById(1)} — always returns the single configuration row
 * seeded by Flyway V2.
 * </p>
 */
@Repository
public interface BarangaySettingsRepository extends JpaRepository<BarangaySettings, Integer> {
}
