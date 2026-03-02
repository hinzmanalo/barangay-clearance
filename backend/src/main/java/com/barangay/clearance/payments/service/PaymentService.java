package com.barangay.clearance.payments.service;

import com.barangay.clearance.clearance.entity.ClearanceRequest;
import com.barangay.clearance.clearance.repository.ClearanceRequestRepository;
import com.barangay.clearance.clearance.service.ClearanceService;
import com.barangay.clearance.payments.dto.PaymentDTO;
import com.barangay.clearance.payments.entity.Payment;
import com.barangay.clearance.payments.entity.Payment.PaymentMethod;
import com.barangay.clearance.payments.entity.Payment.PaymentStatus;
import com.barangay.clearance.payments.gateway.PaymentGateway;
import com.barangay.clearance.payments.gateway.PaymentRequest;
import com.barangay.clearance.payments.gateway.PaymentResult;
import com.barangay.clearance.payments.repository.PaymentRepository;
import com.barangay.clearance.payments.service.mapper.PaymentMapper;
import com.barangay.clearance.shared.exception.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Core business logic for payment processing.
 *
 * <h3>Idempotency contract</h3>
 * <ol>
 * <li>Client supplies an {@code Idempotency-Key} header that must be a valid
 * UUID v4.</li>
 * <li>Within 24 hours the key is scoped to the initiating user:
 * {@code (idempotencyKey, initiatedByUserId)}.</li>
 * <li>If a matching record is {@code PENDING} → 409 Conflict.</li>
 * <li>If a matching record is {@code SUCCESS} or {@code FAILED} → return cached
 * response with {@code idempotent = true}.</li>
 * <li>If no matching record → persist a {@code PENDING} record via
 * {@code saveAndFlush} (fires the unique constraint immediately), call the
 * gateway, then update to {@code SUCCESS} or {@code FAILED}.</li>
 * <li>Concurrent race conditions are caught by
 * {@link DataIntegrityViolationException}
 * on the unique constraint → 409.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    /** 24-hour idempotency window. */
    private static final long IDEMPOTENCY_TTL_HOURS = 24;

    /**
     * Simple UUID v4 format validation pattern.
     * Does not enforce variant/version bits strictly — just structural format.
     */
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE);

    private final PaymentRepository paymentRepo;
    private final ClearanceRequestRepository clearanceRepo;
    private final ClearanceService clearanceService;
    private final PaymentGateway paymentGateway;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────
    // Resident / online payment
    // ─────────────────────────────────────────────────────────────────

    /**
     * Initiates or replays a payment for the given clearance request.
     *
     * <p>
     * The caller must supply a valid UUID v4 idempotency key via the
     * {@code Idempotency-Key} HTTP header. A fresh key per click ensures
     * each new attempt is treated as a new payment.
     * </p>
     *
     * @param clearanceId    UUID of the clearance to pay for
     * @param userId         UUID of the authenticated user (from JWT)
     * @param idempotencyKey UUID v4 from the {@code Idempotency-Key} header
     * @return the payment DTO; {@code idempotent = true} if served from cache
     * @throws AppException 400 if key is missing/invalid or clearance is not
     *                      APPROVED
     * @throws AppException 404 if clearance not found
     * @throws AppException 409 if an identical request is currently PENDING
     */
    @Transactional
    public PaymentDTO initiate(UUID clearanceId, UUID userId, String idempotencyKey) {
        // Step 1 — validate idempotency key format
        validateIdempotencyKey(idempotencyKey);

        // Step 2 — look up existing record within 24-hour TTL
        var existing = paymentRepo.findByIdempotencyKeyAndInitiatedByUserIdAndIdempotencyExpiresAtAfter(
                idempotencyKey, userId, Instant.now());

        if (existing.isPresent()) {
            Payment p = existing.get();

            // Step 3 — PENDING → 409
            if (p.getStatus() == PaymentStatus.PENDING) {
                throw AppException.conflict(
                        "A payment with this idempotency key is already in progress (PENDING)");
            }

            // Step 4 — SUCCESS/FAILED → return cached response
            log.info("PaymentService.initiate: idempotent replay key={} paymentId={} status={}",
                    idempotencyKey, p.getId(), p.getStatus());
            return deserializeOrMap(p, true);
        }

        // Step 5 — resolve clearance and validate state
        ClearanceRequest clearance = clearanceRepo.findById(clearanceId)
                .orElseThrow(() -> AppException.notFound("Clearance request not found"));

        if (clearance.getStatus() != ClearanceRequest.ClearanceStatus.APPROVED) {
            throw AppException.badRequest(
                    "Payment can only be initiated for APPROVED clearances (current: " + clearance.getStatus() + ")");
        }

        // Step 5b — build and persist the PENDING record (fires unique constraint)
        Payment pendingPayment = Payment.builder()
                .clearanceRequestId(clearanceId)
                .amount(clearance.getFeeAmount())
                .idempotencyKey(idempotencyKey)
                .initiatedByUserId(userId)
                .paymentMethod(PaymentMethod.STUB)
                .provider(paymentGateway.getProviderCode())
                .status(PaymentStatus.PENDING)
                .idempotencyExpiresAt(Instant.now().plus(IDEMPOTENCY_TTL_HOURS, ChronoUnit.HOURS))
                .build();

        try {
            paymentRepo.saveAndFlush(pendingPayment);
        } catch (DataIntegrityViolationException e) {
            // Step 6 — concurrent duplicate caught by DB unique constraint → 409
            log.warn("PaymentService: concurrent duplicate detected for key={} userId={}",
                    idempotencyKey, userId);
            throw AppException.conflict(
                    "Duplicate payment request — another request with this idempotency key is already in progress");
        }

        // Step 7 — call payment gateway
        PaymentResult result = paymentGateway.initiate(new PaymentRequest(
                clearanceId, userId, clearance.getFeeAmount(), idempotencyKey));

        // Step 8 — update payment with terminal status and serialise for caching
        pendingPayment.setStatus(result.success() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        PaymentDTO dto = paymentMapper.toDTO(pendingPayment);
        pendingPayment.setResponseBody(serializeDTO(dto));

        Payment saved = paymentRepo.save(pendingPayment);
        log.info("PaymentService.initiate: clearanceId={} paymentId={} status={} provider={}",
                clearanceId, saved.getId(), saved.getStatus(), saved.getProvider());

        // Step 9 — mark clearance as PAID on success
        if (result.success()) {
            clearanceService.markPaid(clearanceId);
            log.info("PAYMENT_SUCCESS clearanceId={} paymentId={}", clearanceId, saved.getId());
        } else {
            log.warn("PAYMENT_FAILED clearanceId={} paymentId={} message={}",
                    clearanceId, saved.getId(), result.message());
        }

        return paymentMapper.toDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    // Clerk — cash mark-as-paid
    // ─────────────────────────────────────────────────────────────────

    /**
     * Records a cash payment for a clearance request.
     *
     * <p>
     * Idempotent: calling this method on an already-paid clearance returns the
     * existing payment record instead of creating a duplicate.
     * </p>
     *
     * @param clearanceId the clearance to mark as paid
     * @param staffUserId the clerk's UUID from the JWT
     * @return the payment DTO
     * @throws AppException 404 if clearance not found
     * @throws AppException 400 if clearance is not APPROVED
     */
    @Transactional
    public PaymentDTO markPaid(UUID clearanceId, UUID staffUserId) {
        ClearanceRequest clearance = clearanceRepo.findById(clearanceId)
                .orElseThrow(() -> AppException.notFound("Clearance request not found"));

        if (clearance.getStatus() != ClearanceRequest.ClearanceStatus.APPROVED) {
            throw AppException.badRequest(
                    "Cash payment can only be recorded for APPROVED clearances (current: " + clearance.getStatus()
                            + ")");
        }

        // Idempotent: if already PAID, return existing CASH payment record
        if (clearance.getPaymentStatus() == ClearanceRequest.ClearancePaymentStatus.PAID) {
            List<Payment> existing = paymentRepo.findByClearanceRequestIdOrderByCreatedAtDesc(clearanceId);
            if (!existing.isEmpty()) {
                log.info("PaymentService.markPaid: idempotent replay — clearanceId={} already PAID", clearanceId);
                return paymentMapper.toDTO(existing.get(0));
            }
        }

        // Create a synthetic SUCCESS CASH payment record
        String cashKey = "CASH-" + clearanceId.toString().replace("-", "").substring(0, 12);
        Payment cashPayment = Payment.builder()
                .clearanceRequestId(clearanceId)
                .amount(clearance.getFeeAmount())
                // Use a deterministic key so duplicate calls produce the same record
                .idempotencyKey(cashKey)
                .initiatedByUserId(staffUserId)
                .paymentMethod(PaymentMethod.CASH)
                .provider("CASH")
                .status(PaymentStatus.SUCCESS)
                .idempotencyExpiresAt(Instant.now().plus(IDEMPOTENCY_TTL_HOURS, ChronoUnit.HOURS))
                .build();

        Payment saved = paymentRepo.save(cashPayment);

        // Mark clearance payment status as PAID
        clearanceService.markPaid(clearanceId);

        log.info("PAYMENT_CASH_RECORDED clearanceId={} paymentId={} by={}", clearanceId, saved.getId(), staffUserId);
        return paymentMapper.toDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns all payments for a clearance request (most recent first).
     *
     * @param clearanceId the clearance request UUID
     * @return list of payments (may be empty)
     * @throws AppException 404 if clearance not found
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> getForClearance(UUID clearanceId) {
        if (!clearanceRepo.existsById(clearanceId)) {
            throw AppException.notFound("Clearance request not found");
        }
        return paymentRepo.findByClearanceRequestIdOrderByCreatedAtDesc(clearanceId)
                .stream()
                .map(paymentMapper::toDTO)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Validates that {@code key} is present and matches the UUID v4 pattern.
     *
     * @throws AppException 400 if the key is null, blank, or not a valid UUID v4
     */
    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw AppException.badRequest("Idempotency-Key header is required");
        }
        if (!UUID_PATTERN.matcher(key.trim()).matches()) {
            throw AppException.badRequest("Idempotency-Key must be a valid UUID v4");
        }
    }

    /**
     * Attempts to deserialise the cached {@code responseBody} JSON from the
     * payment entity. Falls back to a live {@link #paymentMapper} mapping if
     * deserialisation fails.
     *
     * @param payment    the payment entity with a cached response body
     * @param idempotent whether to set the {@code idempotent} flag
     * @return the deserialised or freshly-mapped DTO
     */
    private PaymentDTO deserializeOrMap(Payment payment, boolean idempotent) {
        if (payment.getResponseBody() != null) {
            try {
                PaymentDTO cached = objectMapper.readValue(payment.getResponseBody(), PaymentDTO.class);
                cached.setIdempotent(idempotent);
                return cached;
            } catch (JsonProcessingException e) {
                log.warn(
                        "PaymentService: failed to deserialize cached responseBody for paymentId={}; falling back to live map",
                        payment.getId(), e);
            }
        }
        PaymentDTO dto = paymentMapper.toDTO(payment);
        dto.setIdempotent(idempotent);
        return dto;
    }

    /**
     * Serialises a {@link PaymentDTO} to a JSON string for caching in
     * {@code response_body}.
     *
     * @param dto the DTO to serialise
     * @return JSON string, or {@code null} on serialisation failure
     */
    private String serializeDTO(PaymentDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.warn("PaymentService: could not serialize PaymentDTO to responseBody", e);
            return null;
        }
    }
}
