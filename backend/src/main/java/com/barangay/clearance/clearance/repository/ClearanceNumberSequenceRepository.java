package com.barangay.clearance.clearance.repository;

import com.barangay.clearance.clearance.entity.ClearanceNumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClearanceNumberSequenceRepository extends JpaRepository<ClearanceNumberSequence, String> {
}
