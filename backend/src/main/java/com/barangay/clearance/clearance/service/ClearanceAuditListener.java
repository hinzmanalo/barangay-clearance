package com.barangay.clearance.clearance.service;

import com.barangay.clearance.shared.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to {@link ClearanceStatusChangedEvent} and writes an audit record
 * for every clearance state transition.
 *
 * <p>
 * This is the single source of truth for clearance transition auditing
 * (Option A from the phase spec). Services must NOT call
 * {@code auditService.log()} directly for status changes — they publish the
 * event, and this listener handles the audit write.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClearanceAuditListener {

    private final AuditService auditService;

    /**
     * Translates a clearance status-changed event into an audit log entry.
     *
     * <p>
     * The action string is derived from the destination status
     * (e.g. {@code APPROVED → "CLEARANCE_APPROVED"}). The special case
     * {@code FOR_APPROVAL} maps to either {@code CLEARANCE_SUBMITTED} (initial
     * submission, {@code from} is {@code null}) or {@code CLEARANCE_RESUBMITTED}
     * (resident resubmitting after rejection).
     * </p>
     *
     * @param event the published status-change event
     */
    @EventListener
    public void onStatusChanged(ClearanceStatusChangedEvent event) {
        String action = resolveAction(event);

        String details = event.getFrom() == null
                ? "Status set to " + event.getTo().name()
                : "{\"from\":\"" + event.getFrom().name() + "\",\"to\":\"" + event.getTo().name() + "\"}";

        auditService.log(
                event.getActorId(),
                action,
                "ClearanceRequest",
                event.getClearanceRequestId(),
                details);
    }

    /**
     * Maps a status-change event to the appropriate audit action constant.
     *
     * @param event the event to resolve
     * @return a string matching a constant in
     *         {@link com.barangay.clearance.shared.audit.AuditAction}
     */
    private String resolveAction(ClearanceStatusChangedEvent event) {
        return switch (event.getTo()) {
            case FOR_APPROVAL -> event.getFrom() == null
                    ? "CLEARANCE_SUBMITTED" // initial submission / walk-in
                    : "CLEARANCE_RESUBMITTED"; // resident resubmit after rejection
            case APPROVED -> "CLEARANCE_APPROVED";
            case REJECTED -> "CLEARANCE_REJECTED";
            case RELEASED -> "CLEARANCE_RELEASED";
            default -> "CLEARANCE_" + event.getTo().name();
        };
    }
}
