package com.barangay.clearance.shared.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repository for {@link AuditLog} records.
 *
 * <p>
 * Extends {@link JpaSpecificationExecutor} to support the dynamic filter
 * queries required by the audit log viewer endpoint.
 * </p>
 *
 * <p>
 * Mutation is intentionally limited to the {@code save()} inherited from
 * {@link JpaRepository} — records should only ever be inserted, never updated.
 * </p>
 */
public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Returns all audit records for a specific entity, most recent first.
     *
     * @param entityType the entity class name (e.g. {@code "ClearanceRequest"})
     * @param entityId   the entity's primary key
     * @param pageable   pagination / sort
     * @return paginated audit log entries
     */
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    /**
     * Returns all audit records attributed to a specific user, most recent first.
     *
     * @param userId   the user's UUID
     * @param pageable pagination / sort
     * @return paginated audit log entries
     */
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);
}
