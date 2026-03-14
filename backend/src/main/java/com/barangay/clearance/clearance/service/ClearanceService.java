package com.barangay.clearance.clearance.service;

import com.barangay.clearance.clearance.dto.ClearanceRequestDTO;
import com.barangay.clearance.clearance.dto.ClearanceSummaryDTO;
import com.barangay.clearance.clearance.dto.CreateClearanceRequest;
import com.barangay.clearance.clearance.dto.RejectRequest;
import com.barangay.clearance.clearance.entity.ClearanceRequest;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearancePaymentStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.barangay.clearance.clearance.repository.ClearanceRequestRepository;
import com.barangay.clearance.clearance.service.mapper.ClearanceMapper;
import com.barangay.clearance.residents.entity.Resident;
import com.barangay.clearance.residents.entity.Resident.ResidentStatus;
import com.barangay.clearance.residents.repository.ResidentRepository;
import com.barangay.clearance.settings.entity.FeeConfig;
import com.barangay.clearance.settings.repository.FeeConfigRepository;
import com.barangay.clearance.shared.exception.AppException;
import com.barangay.clearance.shared.util.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.clearance.shared.util.SpecificationBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Core business logic for clearance requests.
 *
 * <p>
 * Enforces the state machine:
 * 
 * <pre>
 *   submit / walk-in create → FOR_APPROVAL
 *   FOR_APPROVAL → approve → APPROVED
 *   FOR_APPROVAL → reject  → REJECTED
 *   REJECTED     → resubmit → FOR_APPROVAL (same resident only)
 *   APPROVED + PAID → release → RELEASED (clearance number assigned here)
 * </pre>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClearanceService {

    /**
     * Fallback fees used only if the fee_config row is missing (should never happen
     * after Flyway V2 runs, but guards against misconfigured environments).
     */
    private static final BigDecimal FALLBACK_STANDARD_FEE = new BigDecimal("50.00");
    private static final BigDecimal FALLBACK_RUSH_FEE = new BigDecimal("100.00");

    private final ClearanceRequestRepository clearanceRepo;
    private final ResidentRepository residentRepository;
    private final ClearanceMapper mapper;
    private final ClearanceNumberService numberService;
    private final ApplicationEventPublisher eventPublisher;
    private final FeeConfigRepository feeConfigRepository;

    // ─────────────────────────────────────────────────────────────────
    // Portal — RESIDENT actions
    // ─────────────────────────────────────────────────────────────────

    /**
     * Resident submits a new clearance request via the portal.
     * The resident profile is resolved from the JWT principal's userId.
     * Request starts at {@code FOR_APPROVAL} (skips DRAFT).
     *
     * @param principalUserId the portal user's UUID from the JWT
     * @param request         the submitted form data
     * @return the created clearance DTO
     * @throws AppException 404 if no resident profile is linked to this user
     * @throws AppException 400 if the resident account is not ACTIVE
     */
    @Transactional
    public ClearanceRequestDTO submitPortal(UUID principalUserId, CreateClearanceRequest request) {
        // Resolve resident from JWT principal
        var resident = residentRepository.findByUserId(principalUserId)
                .orElseThrow(() -> AppException.notFound("No resident profile linked to this account"));

        if (resident.getStatus() != ResidentStatus.ACTIVE) {
            throw AppException.badRequest("Resident account is not active");
        }

        if (request.getPurpose() == ClearanceRequest.Purpose.OTHER
                && (request.getPurposeOther() == null || request.getPurposeOther().isBlank())) {
            throw AppException.badRequest("Purpose description is required when purpose is OTHER");
        }

        BigDecimal fee = resolveFee(request.getUrgency());

        ClearanceRequest entity = ClearanceRequest.builder()
                .residentId(resident.getId())
                .requestedBy(principalUserId)
                .purpose(request.getPurpose())
                .purposeOther(request.getPurposeOther())
                .urgency(request.getUrgency())
                .feeAmount(fee)
                .copies(request.getCopies())
                .status(ClearanceStatus.FOR_APPROVAL)
                .paymentStatus(ClearancePaymentStatus.UNPAID)
                .notes(request.getNotes())
                .build();

        ClearanceRequest saved = clearanceRepo.save(entity);
        log.info("CLEARANCE_SUBMITTED (portal) id={} residentId={}", saved.getId(), saved.getResidentId());

        publishStatusChange(saved, null, ClearanceStatus.FOR_APPROVAL, principalUserId);
        return enrich(saved);
    }

    /**
     * Returns paginated clearance requests for the authenticated resident.
     * Scoped by residentId resolved from the JWT — never from request params.
     *
     * @throws AppException 404 if no resident profile is linked to this user
     */
    @Transactional(readOnly = true)
    public PageResponse<ClearanceRequestDTO> listForResident(UUID principalUserId, Pageable pageable) {
        var resident = residentRepository.findByUserId(principalUserId)
                .orElseThrow(() -> AppException.notFound("No resident profile linked to this account"));

        Page<ClearanceRequestDTO> page = clearanceRepo
                .findByResidentId(resident.getId(), pageable)
                .map(this::enrich);
        return PageResponse.of(page);
    }

    /**
     * Get a single clearance owned by the authenticated resident.
     *
     * @throws AppException 403 if the clearance doesn't belong to this resident
     * @throws AppException 404 if not found
     */
    @Transactional(readOnly = true)
    public ClearanceRequestDTO getForResident(UUID id, UUID principalUserId) {
        var resident = residentRepository.findByUserId(principalUserId)
                .orElseThrow(() -> AppException.notFound("No resident profile linked to this account"));

        ClearanceRequest cr = clearanceRepo.findByIdAndResidentId(id, resident.getId())
                .orElseThrow(() -> AppException.notFound("Clearance request not found"));
        return enrich(cr);
    }

    /**
     * Resident resubmits a REJECTED request (edits notes/purpose and puts it back
     * into FOR_APPROVAL).
     *
     * @throws AppException 403 if the request doesn't belong to this resident
     * @throws AppException 400 if the current status is not REJECTED
     */
    @Transactional
    public ClearanceRequestDTO resubmit(UUID id, UUID principalUserId, CreateClearanceRequest request) {
        var resident = residentRepository.findByUserId(principalUserId)
                .orElseThrow(() -> AppException.notFound("No resident profile linked to this account"));

        ClearanceRequest cr = clearanceRepo.findByIdAndResidentId(id, resident.getId())
                .orElseThrow(() -> AppException.notFound("Clearance request not found"));

        guard(cr, ClearanceStatus.REJECTED, "Only REJECTED requests can be resubmitted");

        if (request.getPurpose() == ClearanceRequest.Purpose.OTHER
                && (request.getPurposeOther() == null || request.getPurposeOther().isBlank())) {
            throw AppException.badRequest("Purpose description is required when purpose is OTHER");
        }

        ClearanceStatus previous = cr.getStatus();
        cr.setPurpose(request.getPurpose());
        cr.setPurposeOther(request.getPurposeOther());
        cr.setUrgency(request.getUrgency());
        cr.setFeeAmount(resolveFee(request.getUrgency()));
        cr.setCopies(request.getCopies());
        cr.setNotes(request.getNotes());
        cr.setStatus(ClearanceStatus.FOR_APPROVAL);
        cr.setReviewedBy(null);
        cr.setReviewedAt(null);

        ClearanceRequest saved = clearanceRepo.save(cr);
        log.info("CLEARANCE_RESUBMITTED id={}", saved.getId());

        publishStatusChange(saved, previous, ClearanceStatus.FOR_APPROVAL, principalUserId);
        return enrich(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    // Backoffice — staff actions
    // ─────────────────────────────────────────────────────────────────

    /**
     * Walk-in request creation by a clerk.
     * Resident is selected from the registry. Request starts at
     * {@code FOR_APPROVAL}.
     *
     * @param staffUserId the clerk's userId from the JWT
     * @throws AppException 400 if {@code residentId} is missing
     * @throws AppException 404 if the resident doesn't exist
     */
    @Transactional
    public ClearanceRequestDTO createWalkIn(UUID staffUserId, CreateClearanceRequest request) {
        if (request.getResidentId() == null) {
            throw AppException.badRequest("residentId is required for walk-in requests");
        }

        var resident = residentRepository.findById(request.getResidentId())
                .orElseThrow(() -> AppException.notFound("Resident not found"));

        if (request.getPurpose() == ClearanceRequest.Purpose.OTHER
                && (request.getPurposeOther() == null || request.getPurposeOther().isBlank())) {
            throw AppException.badRequest("Purpose description is required when purpose is OTHER");
        }

        BigDecimal fee = resolveFee(request.getUrgency());

        ClearanceRequest entity = ClearanceRequest.builder()
                .residentId(resident.getId())
                .requestedBy(staffUserId)
                .purpose(request.getPurpose())
                .purposeOther(request.getPurposeOther())
                .urgency(request.getUrgency())
                .feeAmount(fee)
                .copies(request.getCopies())
                .status(ClearanceStatus.FOR_APPROVAL)
                .paymentStatus(ClearancePaymentStatus.UNPAID)
                .notes(request.getNotes())
                .build();

        ClearanceRequest saved = clearanceRepo.save(entity);
        log.info("CLEARANCE_CREATED (walk-in) id={} residentId={} by={}", saved.getId(), saved.getResidentId(),
                staffUserId);

        publishStatusChange(saved, null, ClearanceStatus.FOR_APPROVAL, staffUserId);
        return enrich(saved);
    }

    /**
     * Paginated backoffice list with optional filters.
     *
     * @param status        optional status filter
     * @param paymentStatus optional payment status filter
     * @param from          optional inclusive start of {@code created_at} range
     * @param to            optional inclusive end of {@code created_at} range
     */
    @Transactional(readOnly = true)
    public PageResponse<ClearanceRequestDTO> list(
            ClearanceStatus status,
            ClearancePaymentStatus paymentStatus,
            Instant from,
            Instant to,
            Pageable pageable) {

        var spec = SpecificationBuilder.<ClearanceRequest>of()
                .equal("status", status)
                .equal("paymentStatus", paymentStatus)
                .greaterThanOrEqualTo("createdAt", from)
                .lessThanOrEqualTo("createdAt", to)
                .build();
        Page<ClearanceRequestDTO> page = clearanceRepo.findAll(spec, pageable).map(this::enrich);
        return PageResponse.of(page);
    }

    /**
     * Get a single clearance request by ID (backoffice — no ownership check).
     *
     * @throws AppException 404 if not found
     */
    @Transactional(readOnly = true)
    public ClearanceRequestDTO getById(UUID id) {
        return enrich(findOrThrow(id));
    }

    /**
     * Approve a clearance request.
     * Transition: {@code FOR_APPROVAL → APPROVED}.
     *
     * @param staffUserId the approver's userId from the JWT
     * @throws AppException 400 if current status is not FOR_APPROVAL
     */
    @Transactional
    public ClearanceRequestDTO approve(UUID id, UUID staffUserId) {
        ClearanceRequest cr = findOrThrow(id);
        guard(cr, ClearanceStatus.FOR_APPROVAL, "Only FOR_APPROVAL requests can be approved");

        ClearanceStatus previous = cr.getStatus();
        cr.setStatus(ClearanceStatus.APPROVED);
        cr.setReviewedBy(staffUserId);
        cr.setReviewedAt(Instant.now());

        ClearanceRequest saved = clearanceRepo.save(cr);
        log.info("CLEARANCE_APPROVED id={} by={}", saved.getId(), staffUserId);

        publishStatusChange(saved, previous, ClearanceStatus.APPROVED, staffUserId);
        return enrich(saved);
    }

    /**
     * Reject a clearance request.
     * Transition: {@code FOR_APPROVAL → REJECTED}.
     * The rejection reason is stored in {@code notes}.
     *
     * @throws AppException 400 if current status is not FOR_APPROVAL
     * @throws AppException 400 if reason is blank (enforced by DTO validation, but
     *                      also here as a guard)
     */
    @Transactional
    public ClearanceRequestDTO reject(UUID id, UUID staffUserId, RejectRequest rejectRequest) {
        ClearanceRequest cr = findOrThrow(id);
        guard(cr, ClearanceStatus.FOR_APPROVAL, "Only FOR_APPROVAL requests can be rejected");

        if (rejectRequest.getReason() == null || rejectRequest.getReason().isBlank()) {
            throw AppException.badRequest("Rejection reason is required");
        }

        ClearanceStatus previous = cr.getStatus();
        cr.setStatus(ClearanceStatus.REJECTED);
        cr.setReviewedBy(staffUserId);
        cr.setReviewedAt(Instant.now());
        // Append rejection reason to notes for audit visibility
        cr.setNotes("[REJECTED] " + rejectRequest.getReason()
                + (cr.getNotes() != null ? "\n" + cr.getNotes() : ""));

        ClearanceRequest saved = clearanceRepo.save(cr);
        log.info("CLEARANCE_REJECTED id={} by={} reason={}", saved.getId(), staffUserId, rejectRequest.getReason());

        publishStatusChange(saved, previous, ClearanceStatus.REJECTED, staffUserId);
        return enrich(saved);
    }

    /**
     * Release a clearance request.
     * Transition: {@code APPROVED + PAID → RELEASED}.
     * The clearance number is assigned atomically at this point.
     *
     * @throws AppException 400 if status is not APPROVED
     * @throws AppException 400 if paymentStatus is not PAID
     */
    @Transactional
    public ClearanceRequestDTO release(UUID id, UUID staffUserId) {
        ClearanceRequest cr = findOrThrow(id);

        if (cr.getStatus() != ClearanceStatus.APPROVED) {
            throw AppException.badRequest(
                    "Cannot release: request must be APPROVED (current: " + cr.getStatus() + ")");
        }
        if (cr.getPaymentStatus() != ClearancePaymentStatus.PAID) {
            throw AppException.badRequest(
                    "Cannot release: payment must be PAID (current: " + cr.getPaymentStatus() + ")");
        }

        String clearanceNumber = numberService.next();

        ClearanceStatus previous = cr.getStatus();
        cr.setStatus(ClearanceStatus.RELEASED);
        cr.setClearanceNumber(clearanceNumber);
        cr.setIssuedAt(Instant.now());
        cr.setReviewedBy(staffUserId);
        cr.setReviewedAt(Instant.now());

        ClearanceRequest saved = clearanceRepo.save(cr);
        log.info("CLEARANCE_RELEASED id={} number={} by={}", saved.getId(), clearanceNumber, staffUserId);

        publishStatusChange(saved, previous, ClearanceStatus.RELEASED, staffUserId);
        return enrich(saved);
    }

    /**
     * Dashboard summary counts for the backoffice.
     */
    @Transactional(readOnly = true)
    public ClearanceSummaryDTO summary() {
        long pendingApproval = clearanceRepo.countByStatus(ClearanceStatus.FOR_APPROVAL);
        long approved = clearanceRepo.countByStatus(ClearanceStatus.APPROVED);
        long awaitingPayment = clearanceRepo.countByPaymentStatus(ClearancePaymentStatus.UNPAID);

        // "Released today" — requests issued on the current UTC day
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = startOfDay.plusSeconds(86_400);
        long releasedToday = clearanceRepo.countReleasedToday(startOfDay, endOfDay);

        return ClearanceSummaryDTO.builder()
                .pendingApproval(pendingApproval)
                .approved(approved)
                .awaitingPayment(awaitingPayment)
                .releasedToday(releasedToday)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // Package-visible helper for Payments module (Phase 4)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Marks a clearance request's payment status as PAID.
     * Called by the Payments module after a successful payment.
     *
     * @throws AppException 404 if not found
     * @throws AppException 400 if request is not in APPROVED state
     */
    @Transactional
    public ClearanceRequestDTO markPaid(UUID id) {
        ClearanceRequest cr = findOrThrow(id);
        if (cr.getStatus() != ClearanceStatus.APPROVED) {
            throw AppException.badRequest("Payment can only be recorded for APPROVED requests");
        }
        cr.setPaymentStatus(ClearancePaymentStatus.PAID);
        ClearanceRequest saved = clearanceRepo.save(cr);
        log.info("CLEARANCE_PAYMENT_MARKED_PAID id={}", saved.getId());
        return enrich(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    // PDF support — raw entity access for PDF generation
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns the raw clearance entity for PDF generation.
     *
     * @throws AppException 404 if not found
     * @throws AppException 400 if status is not RELEASED
     */
    @Transactional(readOnly = true)
    public ClearanceRequest getReleasedEntity(UUID id) {
        ClearanceRequest cr = findOrThrow(id);
        if (cr.getStatus() != ClearanceStatus.RELEASED) {
            throw AppException
                    .badRequest("PDF can only be generated for RELEASED clearances (current: " + cr.getStatus() + ")");
        }
        return cr;
    }

    /**
     * Returns the raw clearance entity for PDF generation, scoped to the resident's
     * ownership.
     *
     * @throws AppException 404 if not found or not owned by this resident
     * @throws AppException 403 if status is not RELEASED
     */
    @Transactional(readOnly = true)
    public ClearanceRequest getReleasedEntityForResident(UUID id, UUID principalUserId) {
        Resident resident = residentRepository.findByUserId(principalUserId)
                .orElseThrow(() -> AppException.notFound("No resident profile linked to this account"));

        ClearanceRequest cr = clearanceRepo.findByIdAndResidentId(id, resident.getId())
                .orElseThrow(() -> AppException.notFound("Clearance request not found"));

        if (cr.getStatus() != ClearanceStatus.RELEASED) {
            throw AppException.forbidden("PDF can only be downloaded for RELEASED clearances");
        }
        return cr;
    }

    /**
     * Returns the resident associated with a clearance request.
     *
     * @throws AppException 404 if resident not found
     */
    @Transactional(readOnly = true)
    public Resident getResidentForClearance(UUID residentId) {
        return residentRepository.findById(residentId)
                .orElseThrow(() -> AppException.notFound("Resident not found for clearance"));
    }

    // ─────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────

    private ClearanceRequest findOrThrow(UUID id) {
        return clearanceRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Clearance request not found"));
    }

    /** Validates that the request is in the expected status before a transition. */
    private void guard(ClearanceRequest cr, ClearanceStatus expected, String message) {
        if (cr.getStatus() != expected) {
            throw AppException.badRequest(message + " (current: " + cr.getStatus() + ")");
        }
    }

    /**
     * Enriches the mapped DTO with the denormalised resident name.
     * Falls back to "Unknown Resident" if the resident is not found.
     */
    private ClearanceRequestDTO enrich(ClearanceRequest entity) {
        ClearanceRequestDTO dto = mapper.toDTO(entity);

        // Cross-module call to residents — uses service layer (no cross-module JPA)
        String residentName = residentRepository.findById(entity.getResidentId())
                .map(r -> r.getLastName() + ", " + r.getFirstName())
                .orElse("Unknown Resident");

        // Builder pattern not used here — DTOs are already constructed by MapStruct.
        // Use reflection-free approach: return a new DTO with residentName populated.
        return ClearanceRequestDTO.builder()
                .id(dto.getId())
                .clearanceNumber(dto.getClearanceNumber())
                .residentId(dto.getResidentId())
                .residentName(residentName)
                .requestedBy(dto.getRequestedBy())
                .purpose(dto.getPurpose())
                .purposeOther(dto.getPurposeOther())
                .urgency(dto.getUrgency())
                .feeAmount(dto.getFeeAmount())
                .copies(dto.getCopies())
                .status(dto.getStatus())
                .paymentStatus(dto.getPaymentStatus())
                .notes(dto.getNotes())
                .reviewedBy(dto.getReviewedBy())
                .reviewedAt(dto.getReviewedAt())
                .issuedAt(dto.getIssuedAt())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    /**
     * Resolves the fee for the given urgency level by reading the live fee_config
     * singleton. Falls back to hardcoded defaults if the row is unavailable.
     *
     * @param urgency STANDARD or RUSH
     * @return fee amount from the database (or fallback default)
     */
    private BigDecimal resolveFee(ClearanceRequest.Urgency urgency) {
        FeeConfig config = feeConfigRepository.findById(1).orElse(null);
        if (config == null) {
            log.warn("fee_config row not found — using fallback defaults");
            return urgency == ClearanceRequest.Urgency.RUSH ? FALLBACK_RUSH_FEE : FALLBACK_STANDARD_FEE;
        }
        return urgency == ClearanceRequest.Urgency.RUSH ? config.getRushFee() : config.getStandardFee();
    }

    private void publishStatusChange(ClearanceRequest cr, ClearanceStatus from, ClearanceStatus to, UUID actorId) {
        eventPublisher.publishEvent(new ClearanceStatusChangedEvent(this, cr.getId(), from, to, actorId));
    }
}
