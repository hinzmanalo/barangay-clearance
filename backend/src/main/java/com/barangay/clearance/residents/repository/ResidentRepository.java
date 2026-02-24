package com.barangay.clearance.residents.repository;

import com.barangay.clearance.residents.entity.Resident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResidentRepository extends JpaRepository<Resident, UUID> {

        /**
         * Full-text search matching on lower(last_name || ' ' || first_name || ' ' ||
         * first_name || ' ' || last_name)
         * to exploit the idx_residents_name functional index.
         * Also filters by purok_zone (address partial match) when provided.
         */
        @Query("""
                        SELECT r FROM Resident r
                        WHERE (:q = '' OR
                               LOWER(CONCAT(r.lastName, ' ', r.firstName)) LIKE LOWER(CONCAT('%', :q, '%')) OR
                               LOWER(CONCAT(r.firstName, ' ', r.lastName)) LIKE LOWER(CONCAT('%', :q, '%')))
                          AND (:purok = '' OR LOWER(r.address) LIKE LOWER(CONCAT('%', :purok, '%')))
                        """)
        Page<Resident> search(
                        @Param("q") String q,
                        @Param("purok") String purok,
                        Pageable pageable);

        Optional<Resident> findByUserId(UUID userId);

        boolean existsByUserId(UUID userId);
}
