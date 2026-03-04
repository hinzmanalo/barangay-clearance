package com.barangay.clearance.shared.audit;

import com.barangay.clearance.identity.repository.UserRepository;
import com.barangay.clearance.shared.util.PageResponse;
import com.barangay.clearance.shared.util.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only query service for audit log records.
 *
 * <p>
 * Handles dynamic filter logic and enriches returned DTOs with the actor's
 * email address by performing a batch lookup against the users table.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

        private final AuditLogRepository auditLogRepository;
        private final UserRepository userRepository;

        /**
         * Returns a paginated, filtered list of audit log entries.
         *
         * @param action     optional exact-match filter on the {@code action} column
         * @param entityType optional exact-match filter on the {@code entity_type}
         *                   column
         * @param userId     optional exact-match filter on the {@code user_id} column
         * @param from       optional lower bound (inclusive) for {@code created_at}
         * @param to         optional upper bound (inclusive) for {@code created_at}
         * @param pageable   pagination and sort (default: {@code created_at DESC})
         * @return paginated audit log DTOs with enriched actor emails
         */
        @Transactional(readOnly = true)
        public PageResponse<AuditLogDTO> list(
                        String action,
                        String entityType,
                        UUID userId,
                        Instant from,
                        Instant to,
                        Pageable pageable) {

                var spec = SpecificationBuilder.<AuditLog>of()
                                .equal("action", action)
                                .equal("entityType", entityType)
                                .equal("userId", userId)
                                .greaterThanOrEqualTo("createdAt", from)
                                .lessThanOrEqualTo("createdAt", to)
                                .build();

                Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
                Page<AuditLogDTO> dtoPage = page.map(entry -> toDTO(entry, null));

                // Batch-enrich actor emails to avoid N+1 queries
                Set<UUID> userIds = page.getContent().stream()
                                .map(AuditLog::getUserId)
                                .filter(id -> id != null)
                                .collect(Collectors.toSet());

                Map<UUID, String> emailMap = userIds.isEmpty()
                                ? Map.of()
                                : userRepository.findAllById(userIds).stream()
                                                .collect(Collectors.toMap(u -> u.getId(), u -> u.getEmail()));

                Page<AuditLogDTO> enriched = page.map(entry -> toDTO(entry, emailMap.get(entry.getUserId())));
                return PageResponse.of(enriched);
        }

        /**
         * Returns paginated audit log entries for a specific entity.
         *
         * @param entityType the entity class name (e.g. {@code "ClearanceRequest"})
         * @param entityId   the entity's primary key
         * @param pageable   pagination and sort
         * @return paginated audit log DTOs with enriched actor emails
         */
        @Transactional(readOnly = true)
        public PageResponse<AuditLogDTO> listByEntity(String entityType, UUID entityId, Pageable pageable) {
                Page<AuditLog> page = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);

                Set<UUID> userIds = page.getContent().stream()
                                .map(AuditLog::getUserId)
                                .filter(id -> id != null)
                                .collect(Collectors.toSet());

                Map<UUID, String> emailMap = userIds.isEmpty()
                                ? Map.of()
                                : userRepository.findAllById(userIds).stream()
                                                .collect(Collectors.toMap(u -> u.getId(), u -> u.getEmail()));

                Page<AuditLogDTO> dtoPage = page.map(entry -> toDTO(entry, emailMap.get(entry.getUserId())));
                return PageResponse.of(dtoPage);
        }

        // ── private helpers ──────────────────────────────────────────────────────

        private AuditLogDTO toDTO(AuditLog entry, String actorEmail) {
                return AuditLogDTO.builder()
                                .id(entry.getId())
                                .userId(entry.getUserId())
                                .actorEmail(actorEmail)
                                .action(entry.getAction())
                                .entityType(entry.getEntityType())
                                .entityId(entry.getEntityId())
                                .details(entry.getDetails())
                                .ipAddress(entry.getIpAddress())
                                .createdAt(entry.getCreatedAt())
                                .build();
        }
}
