package com.barangay.clearance.clearance.controller;

import com.barangay.clearance.clearance.dto.ClearanceRequestDTO;
import com.barangay.clearance.clearance.dto.ClearanceSummaryDTO;
import com.barangay.clearance.clearance.dto.CreateClearanceRequest;
import com.barangay.clearance.clearance.dto.RejectRequest;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearancePaymentStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.barangay.clearance.clearance.service.ClearanceService;
import com.barangay.clearance.pdf.service.ClearancePdfService;
import com.barangay.clearance.settings.entity.BarangaySettings;
import com.barangay.clearance.settings.repository.BarangaySettingsRepository;
import com.barangay.clearance.shared.security.UserPrincipal;
import com.barangay.clearance.shared.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Backoffice endpoints for managing clearance requests.
 * Accessible by CLERK, APPROVER, and ADMIN roles.
 */
@Tag(name = "Clearances (Backoffice)", description = "Staff clearance management endpoints")
@RestController
@RequestMapping("/api/v1/clearances")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class ClearanceController {

        private final ClearanceService clearanceService;
        private final ClearancePdfService clearancePdfService;
        private final BarangaySettingsRepository settingsRepository;

        @Operation(summary = "List clearance requests", description = "Paginated list with optional filters for status, payment status, and date range")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Page of clearance requests"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden")
        })
        @GetMapping
        @PreAuthorize("hasAnyRole('CLERK', 'APPROVER', 'ADMIN')")
        public ResponseEntity<PageResponse<ClearanceRequestDTO>> list(
                        @RequestParam(required = false) ClearanceStatus status,
                        @RequestParam(required = false) ClearancePaymentStatus paymentStatus,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {

                PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                return ResponseEntity.ok(clearanceService.list(status, paymentStatus, from, to, pageable));
        }

        @Operation(summary = "Create a walk-in clearance request", description = "Clerk creates a request on behalf of a resident. Starts at FOR_APPROVAL.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Request created"),
                        @ApiResponse(responseCode = "400", description = "Validation error or resident not found"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden")
        })
        @PostMapping
        @PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")
        public ResponseEntity<ClearanceRequestDTO> createWalkIn(
                        @AuthenticationPrincipal UserPrincipal principal,
                        @Valid @RequestBody CreateClearanceRequest request) {

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(clearanceService.createWalkIn(principal.getUserId(), request));
        }

        @Operation(summary = "Get a clearance request by ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Request found"),
                        @ApiResponse(responseCode = "404", description = "Not found"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('CLERK', 'APPROVER', 'ADMIN')")
        public ResponseEntity<ClearanceRequestDTO> getById(@PathVariable UUID id) {
                return ResponseEntity.ok(clearanceService.getById(id));
        }

        @Operation(summary = "Approve a clearance request", description = "Transitions FOR_APPROVAL → APPROVED. Requires APPROVER or ADMIN role.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Request approved"),
                        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — APPROVER or ADMIN role required"),
                        @ApiResponse(responseCode = "404", description = "Not found")
        })
        @PostMapping("/{id}/approve")
        @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
        public ResponseEntity<ClearanceRequestDTO> approve(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal principal) {

                return ResponseEntity.ok(clearanceService.approve(id, principal.getUserId()));
        }

        @Operation(summary = "Reject a clearance request", description = "Transitions FOR_APPROVAL → REJECTED. Reason is required.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Request rejected"),
                        @ApiResponse(responseCode = "400", description = "Missing reason or invalid status transition"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — APPROVER or ADMIN role required"),
                        @ApiResponse(responseCode = "404", description = "Not found")
        })
        @PostMapping("/{id}/reject")
        @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
        public ResponseEntity<ClearanceRequestDTO> reject(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal principal,
                        @Valid @RequestBody RejectRequest request) {

                return ResponseEntity.ok(clearanceService.reject(id, principal.getUserId(), request));
        }

        @Operation(summary = "Release a clearance request", description = "Transitions APPROVED + PAID → RELEASED. Assigns clearance number.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Request released with clearance number"),
                        @ApiResponse(responseCode = "400", description = "Not APPROVED or payment not PAID"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — CLERK or ADMIN role required"),
                        @ApiResponse(responseCode = "404", description = "Not found")
        })
        @PostMapping("/{id}/release")
        @PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")
        public ResponseEntity<ClearanceRequestDTO> release(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal principal) {

                return ResponseEntity.ok(clearanceService.release(id, principal.getUserId()));
        }

        @Operation(summary = "Download clearance PDF", description = "Generates and downloads the clearance certificate PDF. Only available for RELEASED clearances.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "PDF document"),
                        @ApiResponse(responseCode = "400", description = "Clearance is not in RELEASED status"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Not found")
        })
        @GetMapping("/{id}/pdf")
        @PreAuthorize("hasAnyRole('CLERK', 'ADMIN')")
        public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
                var clearance = clearanceService.getReleasedEntity(id);
                var resident = clearanceService.getResidentForClearance(clearance.getResidentId());
                var settings = settingsRepository.findById(1)
                                .orElseGet(() -> BarangaySettings.builder()
                                                .id(1).barangayName("Barangay").municipality("Municipality")
                                                .province("Province").captainName("Barangay Captain").build());

                byte[] pdfBytes = clearancePdfService.generate(clearance, resident, settings);
                String filename = "clearance-" + clearance.getClearanceNumber() + ".pdf";

                return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                                .body(pdfBytes);
        }

        @Operation(summary = "Dashboard summary counts", description = "Returns pending approval count, approved awaiting payment count, and released today count.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Summary counts"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @GetMapping("/summary")
        @PreAuthorize("hasAnyRole('CLERK', 'APPROVER', 'ADMIN')")
        public ResponseEntity<ClearanceSummaryDTO> summary() {
                return ResponseEntity.ok(clearanceService.summary());
        }
}
