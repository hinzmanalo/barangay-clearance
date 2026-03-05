package com.barangay.clearance.clearance.controller;

import com.barangay.clearance.clearance.dto.ClearanceRequestDTO;
import com.barangay.clearance.clearance.dto.CreateClearanceRequest;
import com.barangay.clearance.clearance.service.ClearanceService;
import com.barangay.clearance.pdf.service.ClearancePdfService;
import com.barangay.clearance.settings.entity.BarangaySettings;
import com.barangay.clearance.settings.repository.BarangaySettingsRepository;
import com.barangay.clearance.shared.audit.AuditAction;
import com.barangay.clearance.shared.audit.AuditService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Resident portal endpoints for clearance requests.
 *
 * <p>
 * <strong>Security note:</strong> The resident identity is always resolved
 * from the JWT principal — never from request parameters — to prevent
 * horizontal
 * privilege escalation.
 * </p>
 */
@Tag(name = "Clearances (Portal)", description = "Resident portal clearance request endpoints")
@RestController
@RequestMapping("/api/v1/me/clearances")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
@SecurityRequirement(name = "bearer-jwt")
public class PortalClearanceController {

        private final ClearanceService clearanceService;
        private final ClearancePdfService clearancePdfService;
        private final BarangaySettingsRepository settingsRepository;
        private final AuditService auditService;

        @Operation(summary = "List my clearance requests", description = "Returns the authenticated resident's clearance history. Scoped to the resident from the JWT.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Paginated list of the resident's requests"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden — RESIDENT role required"),
                        @ApiResponse(responseCode = "404", description = "No resident profile linked to this account")
        })
        @GetMapping
        public ResponseEntity<PageResponse<ClearanceRequestDTO>> list(
                        @AuthenticationPrincipal UserPrincipal principal,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {

                PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                return ResponseEntity.ok(clearanceService.listForResident(principal.getUserId(), pageable));
        }

        @Operation(summary = "Submit a clearance request", description = "Creates a new clearance request for the authenticated resident. Starts at FOR_APPROVAL.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Request submitted successfully"),
                        @ApiResponse(responseCode = "400", description = "Validation error or account not active"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "No resident profile linked to this account")
        })
        @PostMapping
        public ResponseEntity<ClearanceRequestDTO> submit(
                        @AuthenticationPrincipal UserPrincipal principal,
                        @Valid @RequestBody CreateClearanceRequest request) {

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(clearanceService.submitPortal(principal.getUserId(), request));
        }

        @Operation(summary = "Get my clearance request by ID", description = "Returns the detail of a single request. Validates ownership — returns 404 if the request doesn't belong to the resident.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Request found"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Not found or not owned by this resident")
        })
        @GetMapping("/{id}")
        public ResponseEntity<ClearanceRequestDTO> getById(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal principal) {

                return ResponseEntity.ok(clearanceService.getForResident(id, principal.getUserId()));
        }

        @Operation(summary = "Resubmit a rejected request", description = "Resident may resubmit a REJECTED request with updated information. Transitions back to FOR_APPROVAL.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Request resubmitted"),
                        @ApiResponse(responseCode = "400", description = "Request is not in REJECTED state"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Not found or not owned by this resident")
        })
        @PutMapping("/{id}")
        public ResponseEntity<ClearanceRequestDTO> resubmit(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal principal,
                        @Valid @RequestBody CreateClearanceRequest request) {

                return ResponseEntity.ok(clearanceService.resubmit(id, principal.getUserId(), request));
        }

        @Operation(summary = "Download my clearance PDF", description = "Downloads the clearance certificate PDF. Only available for RELEASED clearances owned by the authenticated resident.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "PDF document"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Clearance is not in RELEASED status"),
                        @ApiResponse(responseCode = "404", description = "Not found or not owned by this resident")
        })
        @GetMapping("/{id}/pdf")
        public ResponseEntity<byte[]> downloadPdf(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal UserPrincipal principal) {

                var clearance = clearanceService.getReleasedEntityForResident(id, principal.getUserId());
                var resident = clearanceService.getResidentForClearance(clearance.getResidentId());
                var settings = settingsRepository.findById(1)
                                .orElseGet(() -> BarangaySettings.builder()
                                                .id(1).barangayName("Barangay").municipality("Municipality")
                                                .province("Province").captainName("Barangay Captain").build());

                byte[] pdfBytes = clearancePdfService.generate(clearance, resident, settings);
                String filename = "clearance-" + clearance.getClearanceNumber() + ".pdf";

                auditService.log(principal.getUserId(), AuditAction.CLEARANCE_PDF_DOWNLOADED,
                                "ClearanceRequest", id,
                                "PDF downloaded by resident: clearanceNumber=" + clearance.getClearanceNumber());

                return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                                .body(pdfBytes);
        }
}
