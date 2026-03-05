package com.barangay.clearance.shared.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Cross-cutting service for writing immutable audit log records.
 *
 * <h3>Design decisions</h3>
 * <ul>
 * <li><b>REQUIRES_NEW propagation</b> — audit writes commit independently
 * of the caller's transaction. If the caller's transaction rolls back
 * (e.g. a validation failure <em>after</em> partially succeeding), the
 * audit entry still persists.</li>
 * <li><b>@Async execution</b> — audit writes are dispatched to the
 * {@code audit-pool} thread pool so they do not add latency to
 * user-facing requests. The pool is configured in
 * {@link AuditAsyncConfig}.</li>
 * <li><b>IP resolution</b> — the client IP is read from
 * {@code RequestContextHolder} so it is available without passing
 * {@code HttpServletRequest} into every service method. When called from
 * a background thread (e.g. an event listener) the request context is
 * propagated by Spring's {@code TaskDecorator}; if unavailable, IP is
 * {@code null}.</li>
 * </ul>
 *
 * <p>
 * This service has <em>no</em> dependency on any module-specific service or
 * repository — all modules may safely depend on it without creating cycles.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Records a state-changing operation asynchronously in a new transaction.
     *
     * @param userId     the actor's UUID — {@code null} for unauthenticated events
     * @param action     one of the constants in {@link AuditAction}
     * @param entityType the affected entity class name (max 50 chars)
     * @param entityId   the affected entity's primary key — may be {@code null}
     * @param details    human-readable or JSON description of what changed
     */
    @Async("auditTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID userId, String action, String entityType, UUID entityId, String details) {
        try {
            String ipAddress = resolveIpAddress();

            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(entry);

            log.debug("Audit: action={} entityType={} entityId={} userId={} ip={}",
                    action, entityType, entityId, userId, ipAddress);

        } catch (Exception e) {
            // Audit failure must NEVER propagate to the caller.
            // Log the error and continue — a missing audit entry is preferable
            // to a failed user request.
            log.error("Failed to write audit log: action={} entityType={} entityId={} userId={}",
                    action, entityType, entityId, userId, e);
        }
    }

    /**
     * Extracts the real client IP address from the current HTTP request context.
     *
     * <p>
     * Checks {@code X-Forwarded-For} first to handle requests behind the
     * Nginx reverse proxy configured in this project. Falls back to
     * {@code request.getRemoteAddr()} for direct connections.
     * </p>
     *
     * @return the client IP, or {@code null} if no HTTP context is available
     *         (e.g. called from a background job or unit test)
     */
    private String resolveIpAddress() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank())
                ? xff.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
