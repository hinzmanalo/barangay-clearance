package com.barangay.clearance.shared.audit;

import com.barangay.clearance.shared.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST endpoints for querying the system audit trail.
 *
 * <p>
 * All endpoints are restricted to {@code ADMIN} role only.
 * </p>
 *
 * <p>
 * Base path: {@code /api/v1/audit-logs}
 * </p>
 */
@Tag(name = "Audit Logs", description = "System audit trail (Admin only)")
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class AuditLogController {

        private final AuditLogQueryService auditLogQueryService;

        /**
         * Returns a paginated, filtered list of audit log entries.
         *
         * <p>
         * All filter parameters are optional. Unspecified filters are ignored.
         * Defaults to {@code sort=createdAt,desc}.
         * </p>
         *
         * @param action     optional exact-match filter on the action column
         * @param entityType optional exact-match filter on the entity type column
         * @param userId     optional filter by actor user ID
         * @param from       optional ISO-8601 lower bound (inclusive) for createdAt
         * @param to         optional ISO-8601 upper bound (inclusive) for createdAt
         * @param pageable   pagination and sort
         * @return paginated audit log DTOs
         */
        @Operation(summary = "List audit logs", description = "Returns a paginated, filtered list of audit log entries. "
                        +
                        "All filter parameters are optional.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Audit logs retrieved"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
        })
        @GetMapping
        public ResponseEntity<PageResponse<AuditLogDTO>> list(
                        @RequestParam(required = false) String action,
                        @RequestParam(required = false) String entityType,
                        @RequestParam(required = false) UUID userId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

                return ResponseEntity.ok(
                                auditLogQueryService.list(action, entityType, userId, from, to, pageable));
        }

        /**
         * Returns the audit history for a specific entity.
         *
         * @param type     the entity type name (e.g. {@code ClearanceRequest})
         * @param id       the entity's UUID primary key
         * @param pageable pagination and sort (default: {@code createdAt DESC})
         * @return paginated audit log entries for the specified entity
         */
        @Operation(summary = "Get entity audit history", description = "Returns all audit log entries for a specific entity instance, "
                        +
                        "ordered by most recent first.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Entity audit history retrieved"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
        })
        @GetMapping("/entity/{type}/{id}")
        public ResponseEntity<PageResponse<AuditLogDTO>> entityHistory(
                        @PathVariable String type,
                        @PathVariable UUID id,
                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

                return ResponseEntity.ok(
                                auditLogQueryService.listByEntity(type, id, pageable));
        }
}
