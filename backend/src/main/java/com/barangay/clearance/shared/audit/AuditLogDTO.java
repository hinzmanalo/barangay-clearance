package com.barangay.clearance.shared.audit;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only DTO returned by the audit log query endpoints.
 *
 * <p>
 * Includes {@code actorEmail} enriched from the {@code users} table.
 * This field is {@code null} when the actor's user account has been deleted
 * or when {@code userId} is {@code null} (unauthenticated events such as
 * failed login attempts).
 * </p>
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogDTO {

    /** Surrogate primary key of the audit record. */
    private UUID id;

    /** UUID of the actor; {@code null} for unauthenticated events. */
    private UUID userId;

    /**
     * Email of the actor resolved from the users table.
     * {@code null} if {@code userId} is null or the user has been deleted.
     */
    private String actorEmail;

    /** The action that was performed (see {@link AuditAction}). */
    private String action;

    /** The entity type that was affected (e.g. {@code "ClearanceRequest"}). */
    private String entityType;

    /** The primary key of the affected entity. */
    private UUID entityId;

    /**
     * Human-readable or JSON-structured description of what changed.
     */
    private String details;

    /** Client IP address; {@code null} for background jobs or non-HTTP contexts. */
    private String ipAddress;

    /** When the audit record was created. */
    private Instant createdAt;
}
