package com.barangay.clearance.clearance.service;

import com.barangay.clearance.clearance.dto.ClearanceRequestDTO;
import com.barangay.clearance.clearance.dto.CreateClearanceRequest;
import com.barangay.clearance.clearance.dto.RejectRequest;
import com.barangay.clearance.clearance.entity.ClearanceRequest;
import com.barangay.clearance.clearance.entity.ClearanceRequest.*;
import com.barangay.clearance.clearance.repository.ClearanceRequestRepository;
import com.barangay.clearance.clearance.service.mapper.ClearanceMapper;
import com.barangay.clearance.residents.entity.Resident;
import com.barangay.clearance.residents.entity.Resident.ResidentStatus;
import com.barangay.clearance.residents.repository.ResidentRepository;
import com.barangay.clearance.settings.entity.FeeConfig;
import com.barangay.clearance.settings.repository.FeeConfigRepository;
import com.barangay.clearance.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearanceServiceTest {

    @Mock
    private ClearanceRequestRepository clearanceRepo;
    @Mock
    private ResidentRepository residentRepository;
    @Mock
    private ClearanceMapper mapper;
    @Mock
    private ClearanceNumberService numberService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private FeeConfigRepository feeConfigRepository;

    @InjectMocks
    private ClearanceService clearanceService;

    private UUID testResidentId;
    private UUID testUserId;
    private UUID testClearanceId;
    private Resident testResident;
    private ClearanceRequest testClearance;

    @BeforeEach
    void setUp() {
        testResidentId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testClearanceId = UUID.randomUUID();

        testResident = Resident.builder()
                .id(testResidentId)
                .userId(testUserId)
                .firstName("John")
                .lastName("Doe")
                .status(ResidentStatus.ACTIVE)
                .build();

        testClearance = ClearanceRequest.builder()
                .id(testClearanceId)
                .residentId(testResidentId)
                .requestedBy(testUserId)
                .purpose(Purpose.EMPLOYMENT)
                .urgency(Urgency.STANDARD)
                .feeAmount(new BigDecimal("50.00"))
                .copies(1)
                .status(ClearanceStatus.FOR_APPROVAL)
                .paymentStatus(ClearancePaymentStatus.UNPAID)
                .build();
    }

    @Test
    void submitPortal_createsRequestInForApprovalStatus() {
        CreateClearanceRequest request = new CreateClearanceRequest(
                Purpose.EMPLOYMENT, null, Urgency.STANDARD, 1, "Test notes", null);

        when(residentRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(testResident));
        when(feeConfigRepository.findById(1))
                .thenReturn(Optional.of(FeeConfig.builder().standardFee(new BigDecimal("50.00")).build()));
        when(clearanceRepo.save(any(ClearanceRequest.class)))
                .thenAnswer(inv -> {
                    ClearanceRequest cr = inv.getArgument(0);
                    cr.setId(testClearanceId);
                    cr.setCreatedAt(Instant.now());
                    return cr;
                });
        when(mapper.toDTO(any(ClearanceRequest.class)))
                .thenReturn(buildClearanceDTO(ClearanceStatus.FOR_APPROVAL));

        ClearanceRequestDTO result = clearanceService.submitPortal(testUserId, request);

        assertNotNull(result);
        assertEquals(ClearanceStatus.FOR_APPROVAL, result.getStatus());
        verify(clearanceRepo).save(argThat(cr -> cr.getStatus() == ClearanceStatus.FOR_APPROVAL &&
                cr.getPaymentStatus() == ClearancePaymentStatus.UNPAID));
    }

    @Test
    void submitPortal_noResidentProfile_throws404() {
        CreateClearanceRequest request = new CreateClearanceRequest(
                Purpose.EMPLOYMENT, null, Urgency.STANDARD, 1, null, null);

        when(residentRepository.findByUserId(testUserId))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> clearanceService.submitPortal(testUserId, request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void approve_fromForApproval_setsApprovedStatus() {
        when(clearanceRepo.findById(testClearanceId))
                .thenReturn(Optional.of(testClearance));
        when(clearanceRepo.save(any(ClearanceRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any(ClearanceRequest.class)))
                .thenReturn(buildClearanceDTO(ClearanceStatus.APPROVED));

        ClearanceRequestDTO result = clearanceService.approve(testClearanceId, UUID.randomUUID());

        assertEquals(ClearanceStatus.APPROVED, result.getStatus());
        verify(clearanceRepo).save(argThat(cr -> cr.getStatus() == ClearanceStatus.APPROVED));
    }

    @Test
    void approve_onApproved_throws400() {
        testClearance.setStatus(ClearanceStatus.APPROVED);
        when(clearanceRepo.findById(testClearanceId))
                .thenReturn(Optional.of(testClearance));

        AppException exception = assertThrows(AppException.class,
                () -> clearanceService.approve(testClearanceId, UUID.randomUUID()));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void reject_fromForApproval_setsRejectedStatus() {
        RejectRequest rejectRequest = new RejectRequest("Does not meet criteria");

        when(clearanceRepo.findById(testClearanceId))
                .thenReturn(Optional.of(testClearance));
        when(clearanceRepo.save(any(ClearanceRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any(ClearanceRequest.class)))
                .thenReturn(buildClearanceDTO(ClearanceStatus.REJECTED));

        ClearanceRequestDTO result = clearanceService.reject(testClearanceId, UUID.randomUUID(), rejectRequest);

        assertEquals(ClearanceStatus.REJECTED, result.getStatus());
        verify(clearanceRepo).save(argThat(cr -> cr.getStatus() == ClearanceStatus.REJECTED &&
                cr.getNotes().contains("Does not meet criteria")));
    }

    @Test
    void reject_withBlankReason_throws400() {
        RejectRequest rejectRequest = new RejectRequest("");

        when(clearanceRepo.findById(testClearanceId))
                .thenReturn(Optional.of(testClearance));

        AppException exception = assertThrows(AppException.class,
                () -> clearanceService.reject(testClearanceId, UUID.randomUUID(), rejectRequest));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void release_fromApprovedAndPaid_setsReleasedStatus() {
        testClearance.setStatus(ClearanceStatus.APPROVED);
        testClearance.setPaymentStatus(ClearancePaymentStatus.PAID);

        when(clearanceRepo.findById(testClearanceId))
                .thenReturn(Optional.of(testClearance));
        when(numberService.next()).thenReturn("2025-030001");
        when(clearanceRepo.save(any(ClearanceRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any(ClearanceRequest.class)))
                .thenReturn(buildClearanceDTO(ClearanceStatus.RELEASED));

        ClearanceRequestDTO result = clearanceService.release(testClearanceId, UUID.randomUUID());

        assertEquals(ClearanceStatus.RELEASED, result.getStatus());
        verify(numberService).next();
    }

    @Test
    void release_whenUnpaid_throws400() {
        testClearance.setStatus(ClearanceStatus.APPROVED);
        testClearance.setPaymentStatus(ClearancePaymentStatus.UNPAID);
        when(clearanceRepo.findById(testClearanceId))
                .thenReturn(Optional.of(testClearance));

        AppException exception = assertThrows(AppException.class,
                () -> clearanceService.release(testClearanceId, UUID.randomUUID()));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void resubmit_fromRejected_setsForApprovalStatus() {
        testClearance.setStatus(ClearanceStatus.REJECTED);

        CreateClearanceRequest request = new CreateClearanceRequest(
                Purpose.EMPLOYMENT, null, Urgency.STANDARD, 1, "Updated notes", null);

        when(residentRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(testResident));
        when(clearanceRepo.findByIdAndResidentId(testClearanceId, testResidentId))
                .thenReturn(Optional.of(testClearance));
        when(feeConfigRepository.findById(1))
                .thenReturn(Optional.of(FeeConfig.builder().standardFee(new BigDecimal("50.00")).build()));
        when(clearanceRepo.save(any(ClearanceRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any(ClearanceRequest.class)))
                .thenReturn(buildClearanceDTO(ClearanceStatus.FOR_APPROVAL));

        ClearanceRequestDTO result = clearanceService.resubmit(testClearanceId, testUserId, request);

        assertEquals(ClearanceStatus.FOR_APPROVAL, result.getStatus());
    }

    @Test
    void resubmit_onForApproval_throws400() {
        testClearance.setStatus(ClearanceStatus.FOR_APPROVAL);

        CreateClearanceRequest request = new CreateClearanceRequest(
                Purpose.EMPLOYMENT, null, Urgency.STANDARD, 1, null, null);

        when(residentRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(testResident));
        when(clearanceRepo.findByIdAndResidentId(testClearanceId, testResidentId))
                .thenReturn(Optional.of(testClearance));

        AppException exception = assertThrows(AppException.class,
                () -> clearanceService.resubmit(testClearanceId, testUserId, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    private ClearanceRequestDTO buildClearanceDTO(ClearanceStatus status) {
        return ClearanceRequestDTO.builder()
                .id(testClearanceId)
                .status(status)
                .paymentStatus(ClearancePaymentStatus.UNPAID)
                .residentId(testResidentId)
                .residentName("Doe, John")
                .clearanceNumber("2025-030001")
                .build();
    }
}
