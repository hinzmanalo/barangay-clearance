package com.barangay.clearance.clearance.service;

import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Published on every clearance status transition.
 * No listeners are registered in the MVP — this is a zero-cost preparation hook
 * for future integrations (notifications, audit, etc.).
 */
@Getter
public class ClearanceStatusChangedEvent extends ApplicationEvent {

    private final UUID clearanceRequestId;
    private final ClearanceStatus from;
    private final ClearanceStatus to;
    private final UUID actorId;

    public ClearanceStatusChangedEvent(Object source,
            UUID clearanceRequestId,
            ClearanceStatus from,
            ClearanceStatus to,
            UUID actorId) {
        super(source);
        this.clearanceRequestId = clearanceRequestId;
        this.from = from;
        this.to = to;
        this.actorId = actorId;
    }
}
