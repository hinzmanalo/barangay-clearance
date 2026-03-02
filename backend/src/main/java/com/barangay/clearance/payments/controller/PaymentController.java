package com.barangay.clearance.payments.controller;

import com.barangay.clearance.clearance.service.ClearanceService;
import com.barangay.clearance.payments.dto.PaymentDTO;
import com.barangay.clearance.payments.service.PaymentService;
import com.barangay.clearance.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Payment endpoints — both backoffice (CLERK/ADMIN) and portal (RESIDENT).
 *
 * <h3>Idempotency</h3>
 * Online payment requests ({@code POST /clearances/{id}/payments} and
 * {@code POST /me/clearances/{id}/pay}) require an {@code Idempotency-Key}
 * header containing a UUID v4. Duplicate requests within 24 hours with the
 * same key and user are replayed from cache.
 */
@Tag(name = "Payments", description = "Payment processing and cash collection endpoints")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class PaymentController {

    private final PaymentService paymentService;
    private final ClearanceService clearanceService;

    // ─────────────────────────────────────────────────────────────────
    // Portal — RESIDENT endpoints
    // ─────────────────────────────────────────────────────────────────

    /**
     * Resident pays for their own clearance via the portal.
     *
     * <p>
     * Ownership is enforced by resolving the resident from the JWT, ensuring
     * the authenticated user can only pay for their own requests.
     * Returns 201 for new payments, 200 for idempotent replays.
     * </p>
     */
    @Operation(summary = "Pay for my clearance (portal)", description = "Initiates an online payment for the authenticated resident's clearance. "
            +
            "Requires an Idempotency-Key header (UUID v4). Returns 201 on first successful call; " +
            "200 with idempotent=true on replay within the 24-hour window.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment initiated successfully"),
            @ApiResponse(responseCode = "200", description = "Idempotent replay — payment already processed"),
            @ApiResponse(responseCode = "400", description = "Invalid idempotency key, or clearance not APPROVED"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — RESIDENT role required"),
            @ApiResponse(responseCode = "404", description = "Clearance request not found"),
            @ApiResponse(responseCode = "409", description = "Payment with this idempotency key is PENDING")
    })
    @PostMapping("/api/v1/me/clearances/{id}/pay")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<PaymentDTO> payPortal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        // Validate ownership — resolves resident from JWT and checks the request
        // belongs to them
        clearanceService.getForResident(id, principal.getUserId());

        PaymentDTO result = paymentService.initiate(id, principal.getUserId(), idempotencyKey);
        HttpStatus httpStatus = result.isIdempotent() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(httpStatus).body(result);
    }

    // ─────────────────────────────────────────────────────────────────
    // Backoffice — CLERK / ADMIN / RESIDENT endpoints
    // ─────────────────────────────────────────────────────────────────

    /**
     * Initiates an online payment for a clearance request (backoffice or
     * integrated).
     *
     * <p>
     * Accessible by RESIDENT, CLERK, and ADMIN. RESIDENT callers should prefer
     * {@code POST /me/clearances/{id}/pay} for ownership-scoped access.
     * </p>
     */
    @Operation(summary = "Initiate payment for a clearance", description = "Initiates an online payment. Requires an Idempotency-Key header (UUID v4). "
            +
            "Returns 201 for new payments, 200 with idempotent=true on replay.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment initiated"),
            @ApiResponse(responseCode = "200", description = "Idempotent replay"),
            @ApiResponse(responseCode = "400", description = "Invalid idempotency key or clearance not APPROVED"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Clearance request not found"),
            @ApiResponse(responseCode = "409", description = "PENDING payment already exists for this key")
    })
    @PostMapping("/api/v1/clearances/{id}/payments")
    @PreAuthorize("hasAnyRole('RESIDENT', 'CLERK', 'ADMIN')")
    public ResponseEntity<PaymentDTO> initiate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        PaymentDTO result = paymentService.initiate(id, principal.getUserId(), idempotencyKey);
        HttpStatus httpStatus = result.isIdempotent() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(httpStatus).body(result);
    }

    /**
     * Records a cash payment collected in-person by a clerk.
     *
     * <p>
     * Idempotent: calling this on an already-paid clearance returns the existing
     * payment record with HTTP 200 (no error).
     * </p>
     */
    @Operation(summary = "Mark clearance as paid (cash)", description = "Records an in-person cash collection. Idempotent — calling on an already-paid "
            +
            "clearance returns the existing payment with HTTP 200.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cash payment recorded or clearance already paid"),
            @ApiResponse(responseCode = "400", description = "Clearance is not APPROVED"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — CLERK or ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Clearance request not found")
    })
    @PostMapping("/api/v1/clearances/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")
    public ResponseEntity<PaymentDTO> markPaid(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        return ResponseEntity.ok(paymentService.markPaid(id, principal.getUserId()));
    }

    /**
     * Returns all payments for a clearance request (most recent first).
     */
    @Operation(summary = "Get payments for a clearance", description = "Returns all payment records associated with the given clearance, "
            +
            "ordered by creation date descending.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of payments"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — CLERK or ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Clearance request not found")
    })
    @GetMapping("/api/v1/clearances/{id}/payments")
    @PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")
    public ResponseEntity<List<PaymentDTO>> getPayments(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getForClearance(id));
    }
}
