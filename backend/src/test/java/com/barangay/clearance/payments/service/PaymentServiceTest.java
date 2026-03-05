package com.barangay.clearance.payments.service;

import com.barangay.clearance.clearance.entity.ClearanceRequest;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearancePaymentStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
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
import com.barangay.clearance.shared.audit.AuditService;
import com.barangay.clearance.shared.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private ClearanceRequestRepository clearanceRepo;
    @Mock
    private ClearanceService clearanceService;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PaymentService paymentService;

    private UUID testClearanceId;
    private UUID testUserId;
    private UUID testPaymentId;
    private String testIdempotencyKey;

    @BeforeEach
    void setUp() {
        testClearanceId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testPaymentId = UUID.randomUUID();
        testIdempotencyKey = "550e8400-e29b-41d4-a716-446655440000";
    }

    @Test
    void initiate_freshKey_callsGatewayAndReturnsPending() {
        ClearanceRequest clearance = ClearanceRequest.builder()
                .id(testClearanceId)
                .status(ClearanceStatus.APPROVED)
                .feeAmount(new BigDecimal("50.00"))
                .build();

        PaymentDTO savedPaymentDTO = PaymentDTO.builder()
                .id(testPaymentId)
                .status(PaymentStatus.SUCCESS)
                .idempotent(false)
                .build();

        when(paymentRepo.findByIdempotencyKeyAndInitiatedByUserIdAndIdempotencyExpiresAtAfter(
                eq(testIdempotencyKey), eq(testUserId), any(Instant.class))).thenReturn(Optional.empty());
        when(clearanceRepo.findById(testClearanceId)).thenReturn(Optional.of(clearance));
        when(paymentGateway.initiate(any(PaymentRequest.class)))
                .thenReturn(new PaymentResult(true, "REF123", null));
        when(paymentRepo.saveAndFlush(any(Payment.class)))
                .thenAnswer(inv -> {
                    Payment p = inv.getArgument(0);
                    p.setId(testPaymentId);
                    return p;
                });
        when(paymentRepo.save(any(Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toDTO(any(Payment.class))).thenReturn(savedPaymentDTO);

        PaymentDTO result = paymentService.initiate(testClearanceId, testUserId, testIdempotencyKey);

        assertNotNull(result);
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertFalse(result.isIdempotent());
        verify(paymentGateway).initiate(any(PaymentRequest.class));
        verify(clearanceService).markPaid(testClearanceId);
    }

    @Test
    void initiate_replaySuccess_returnsCachedIdempotent() {
        Payment existingPayment = Payment.builder()
                .id(testPaymentId)
                .status(PaymentStatus.SUCCESS)
                .build();

        PaymentDTO cachedDTO = PaymentDTO.builder()
                .id(testPaymentId)
                .status(PaymentStatus.SUCCESS)
                .idempotent(true)
                .build();

        when(paymentRepo.findByIdempotencyKeyAndInitiatedByUserIdAndIdempotencyExpiresAtAfter(
                eq(testIdempotencyKey), eq(testUserId), any(Instant.class)))
                .thenReturn(Optional.of(existingPayment));
        when(paymentMapper.toDTO(existingPayment)).thenReturn(cachedDTO);

        PaymentDTO result = paymentService.initiate(testClearanceId, testUserId, testIdempotencyKey);

        assertNotNull(result);
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertTrue(result.isIdempotent());
        verify(paymentGateway, never()).initiate(any());
    }

    @Test
    void initiate_replayPending_throws409() {
        Payment pendingPayment = Payment.builder()
                .id(testPaymentId)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepo.findByIdempotencyKeyAndInitiatedByUserIdAndIdempotencyExpiresAtAfter(
                eq(testIdempotencyKey), eq(testUserId), any(Instant.class)))
                .thenReturn(Optional.of(pendingPayment));

        AppException exception = assertThrows(AppException.class,
                () -> paymentService.initiate(testClearanceId, testUserId, testIdempotencyKey));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().contains("PENDING") || exception.getMessage().contains("in progress"));
    }

    @Test
    void initiate_clearanceNotApproved_throws400() {
        ClearanceRequest unapprovedClearance = ClearanceRequest.builder()
                .id(testClearanceId)
                .status(ClearanceStatus.FOR_APPROVAL)
                .feeAmount(new BigDecimal("50.00"))
                .build();

        when(paymentRepo.findByIdempotencyKeyAndInitiatedByUserIdAndIdempotencyExpiresAtAfter(
                eq(testIdempotencyKey), eq(testUserId), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(clearanceRepo.findById(testClearanceId)).thenReturn(Optional.of(unapprovedClearance));

        AppException exception = assertThrows(AppException.class,
                () -> paymentService.initiate(testClearanceId, testUserId, testIdempotencyKey));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Payment") || exception.getMessage().contains("APPROVED"));
    }

    @Test
    void initiate_missingIdempotencyKey_throws400() {
        AppException exception = assertThrows(AppException.class,
                () -> paymentService.initiate(testClearanceId, testUserId, null));
        assertTrue(exception.getMessage().contains("400") || exception.getMessage().contains("Idempotency"));
    }

    @Test
    void initiate_blankIdempotencyKey_throws400() {
        AppException exception = assertThrows(AppException.class,
                () -> paymentService.initiate(testClearanceId, testUserId, "   "));
        assertTrue(exception.getMessage().contains("400") || exception.getMessage().contains("Idempotency"));
    }

    @Test
    void initiate_invalidIdempotencyKey_throws400() {
        AppException exception = assertThrows(AppException.class,
                () -> paymentService.initiate(testClearanceId, testUserId, "not-a-uuid"));
        assertTrue(exception.getMessage().contains("400") || exception.getMessage().contains("UUID"));
    }

    @Test
    void initiate_concurrentDuplicate_throws409() {
        ClearanceRequest clearance = ClearanceRequest.builder()
                .id(testClearanceId)
                .status(ClearanceStatus.APPROVED)
                .feeAmount(new BigDecimal("50.00"))
                .build();

        when(paymentRepo.findByIdempotencyKeyAndInitiatedByUserIdAndIdempotencyExpiresAtAfter(
                eq(testIdempotencyKey), eq(testUserId), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(clearanceRepo.findById(testClearanceId)).thenReturn(Optional.of(clearance));
        when(paymentRepo.saveAndFlush(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        AppException exception = assertThrows(AppException.class,
                () -> paymentService.initiate(testClearanceId, testUserId, testIdempotencyKey));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().contains("Duplicate") || exception.getMessage().contains("in progress"));
    }

    @Test
    void markPaid_approvedClearance_createsSuccessPayment() {
        ClearanceRequest clearance = ClearanceRequest.builder()
                .id(testClearanceId)
                .status(ClearanceStatus.APPROVED)
                .paymentStatus(ClearancePaymentStatus.UNPAID)
                .feeAmount(new BigDecimal("50.00"))
                .build();

        PaymentDTO paymentDTO = PaymentDTO.builder()
                .id(testPaymentId)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(clearanceRepo.findById(testClearanceId)).thenReturn(Optional.of(clearance));
        when(paymentRepo.save(any(Payment.class)))
                .thenAnswer(inv -> {
                    Payment p = inv.getArgument(0);
                    p.setId(testPaymentId);
                    return p;
                });
        when(paymentMapper.toDTO(any(Payment.class))).thenReturn(paymentDTO);

        PaymentDTO result = paymentService.markPaid(testClearanceId, testUserId);

        assertNotNull(result);
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        verify(clearanceService).markPaid(testClearanceId);
    }

    @Test
    void markPaid_alreadyPaid_returnsExistingIdempotent() {
        ClearanceRequest clearance = ClearanceRequest.builder()
                .id(testClearanceId)
                .status(ClearanceStatus.APPROVED)
                .paymentStatus(ClearancePaymentStatus.PAID)
                .feeAmount(new BigDecimal("50.00"))
                .build();

        Payment existingPayment = Payment.builder()
                .id(testPaymentId)
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(PaymentMethod.CASH)
                .build();

        PaymentDTO existingDTO = PaymentDTO.builder()
                .id(testPaymentId)
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(PaymentMethod.CASH)
                .build();

        when(clearanceRepo.findById(testClearanceId)).thenReturn(Optional.of(clearance));
        when(paymentRepo.findByClearanceRequestIdOrderByCreatedAtDesc(testClearanceId))
                .thenReturn(Collections.singletonList(existingPayment));
        when(paymentMapper.toDTO(existingPayment)).thenReturn(existingDTO);

        PaymentDTO result = paymentService.markPaid(testClearanceId, testUserId);

        assertNotNull(result);
        assertEquals(testPaymentId, result.getId());
        verify(clearanceService, never()).markPaid(testClearanceId);
    }

    @Test
    void markPaid_notApproved_throws400() {
        ClearanceRequest unapprovedClearance = ClearanceRequest.builder()
                .id(testClearanceId)
                .status(ClearanceStatus.FOR_APPROVAL)
                .build();

        when(clearanceRepo.findById(testClearanceId)).thenReturn(Optional.of(unapprovedClearance));

        AppException exception = assertThrows(AppException.class,
                () -> paymentService.markPaid(testClearanceId, testUserId));
        assertTrue(exception.getMessage().contains("400") || exception.getMessage().contains("payment"));
    }

    @Test
    void markPaid_clearanceNotFound_throws404() {
        when(clearanceRepo.findById(testClearanceId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> paymentService.markPaid(testClearanceId, testUserId));
        assertTrue(exception.getMessage().contains("404") || exception.getMessage().contains("not found"));
    }
}
