package com.barangay.clearance.pdf.service;

import com.barangay.clearance.clearance.entity.ClearanceRequest;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearancePaymentStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Purpose;
import com.barangay.clearance.clearance.entity.ClearanceRequest.Urgency;
import com.barangay.clearance.residents.entity.Resident;
import com.barangay.clearance.residents.entity.Resident.Gender;
import com.barangay.clearance.settings.entity.BarangaySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClearancePdfServiceTest {

    private ClearancePdfServiceImpl pdfService;
    private ClearanceRequest testClearance;
    private Resident testResident;
    private BarangaySettings testSettings;

    @BeforeEach
    void setUp() {
        pdfService = new ClearancePdfServiceImpl();

        // Setup test clearance
        testClearance = ClearanceRequest.builder()
                .id(UUID.randomUUID())
                .residentId(UUID.randomUUID())
                .requestedBy(UUID.randomUUID())
                .purpose(Purpose.EMPLOYMENT)
                .urgency(Urgency.STANDARD)
                .feeAmount(new BigDecimal("50.00"))
                .copies(1)
                .status(ClearanceStatus.RELEASED)
                .paymentStatus(ClearancePaymentStatus.PAID)
                .clearanceNumber("2025-03-0001")
                .issuedAt(Instant.now())
                .notes("Test notes")
                .build();

        // Setup test resident
        testResident = Resident.builder()
                .id(testClearance.getResidentId())
                .firstName("Juan")
                .lastName("Dela Cruz")
                .middleName("Garcia")
                .birthDate(LocalDate.of(1990, 5, 15))
                .gender(Gender.MALE)
                .address("123 Main Street")
                .build();

        // Setup test settings
        testSettings = BarangaySettings.builder()
                .id(1)
                .barangayName("Barangay Sample")
                .municipality("San Juan")
                .province("Metro Manila")
                .captainName("Hon. Pedro Santos")
                .logo(null) // No logo initially
                .build();
    }

    @Test
    void generate_returnsNonEmptyByteArray() {
        byte[] pdfBytes = pdfService.generate(testClearance, testResident, testSettings);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF byte array should not be empty");
    }

    @Test
    void generate_startsWithPdfMagicBytes() {
        byte[] pdfBytes = pdfService.generate(testClearance, testResident, testSettings);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length >= 4, "PDF should be at least 4 bytes");

        // Check PDF magic bytes: %PDF
        assertEquals((byte) 0x25, pdfBytes[0], "First byte should be '%' (0x25)");
        assertEquals((byte) 0x50, pdfBytes[1], "Second byte should be 'P' (0x50)");
        assertEquals((byte) 0x44, pdfBytes[2], "Third byte should be 'D' (0x44)");
        assertEquals((byte) 0x46, pdfBytes[3], "Fourth byte should be 'F' (0x46)");
    }

    @Test
    void generate_nullLogo_doesNotThrow() {
        testSettings.setLogo(null);

        // Should complete without exception
        assertDoesNotThrow(() -> {
            byte[] pdfBytes = pdfService.generate(testClearance, testResident, testSettings);
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        });
    }

    @Test
    void generate_withSmallLogo_doesNotThrow() {
        // Create a minimal 1x1 PNG
        byte[] smallPng = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // width=1, height=1
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE, // bit depth, color type, etc.
                0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, // IDAT chunk
                0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00, 0x03, 0x01, 0x01, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, // IEND chunk
                0x42, 0x60, (byte) 0x82
        };

        testSettings.setLogo(smallPng);

        // Should complete without exception
        assertDoesNotThrow(() -> {
            byte[] pdfBytes = pdfService.generate(testClearance, testResident, testSettings);
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
        });
    }

    @Test
    void generate_withDifferentResidents_producesDifferentPdfs() {
        Resident resident2 = Resident.builder()
                .id(UUID.randomUUID())
                .firstName("Maria")
                .lastName("Santos")
                .middleName("Lopez")
                .birthDate(LocalDate.of(1988, 3, 20))
                .gender(Gender.FEMALE)
                .address("456 Oak Avenue")
                .build();

        byte[] pdf1 = pdfService.generate(testClearance, testResident, testSettings);

        ClearanceRequest clearance2 = ClearanceRequest.builder()
                .id(UUID.randomUUID())
                .residentId(resident2.getId())
                .requestedBy(UUID.randomUUID())
                .purpose(Purpose.EMPLOYMENT)
                .urgency(Urgency.STANDARD)
                .feeAmount(new BigDecimal("50.00"))
                .copies(1)
                .status(ClearanceStatus.RELEASED)
                .paymentStatus(ClearancePaymentStatus.PAID)
                .clearanceNumber("2025-03-0002")
                .issuedAt(Instant.now())
                .build();

        byte[] pdf2 = pdfService.generate(clearance2, resident2, testSettings);

        // Both should be valid PDFs
        assertTrue(isPdfValid(pdf1));
        assertTrue(isPdfValid(pdf2));

        // They should be different (different residents, different clearance numbers)
        assertNotEquals(new String(pdf1), new String(pdf2));
    }

    @Test
    void generate_withDifferentSettings_producesDifferentPdfs() {
        byte[] pdf1 = pdfService.generate(testClearance, testResident, testSettings);

        BarangaySettings settings2 = BarangaySettings.builder()
                .id(1)
                .barangayName("Barangay Different")
                .municipality("Makati")
                .province("Metro Manila")
                .captainName("Hon. Juan Reyes")
                .logo(null)
                .build();

        byte[] pdf2 = pdfService.generate(testClearance, testResident, settings2);

        // Both should be valid PDFs
        assertTrue(isPdfValid(pdf1));
        assertTrue(isPdfValid(pdf2));

        // They should be different (different barangay names)
        assertNotEquals(new String(pdf1), new String(pdf2));
    }

    @Test
    void generate_withAllFields_producesPdfWithValidStructure() {
        // Create a fully populated clearance and resident
        ClearanceRequest fullClearance = ClearanceRequest.builder()
                .id(UUID.randomUUID())
                .residentId(testResident.getId())
                .requestedBy(UUID.randomUUID())
                .purpose(Purpose.OTHER)
                .purposeOther("Travel abroad")
                .urgency(Urgency.RUSH)
                .feeAmount(new BigDecimal("100.00"))
                .copies(3)
                .status(ClearanceStatus.RELEASED)
                .paymentStatus(ClearancePaymentStatus.PAID)
                .clearanceNumber("2025-03-0999")
                .issuedAt(Instant.now())
                .notes("Special request - priority processing")
                .build();

        byte[] pdfBytes = pdfService.generate(fullClearance, testResident, testSettings);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF should be substantial size");
        assertTrue(isPdfValid(pdfBytes));
    }

    @Test
    void generate_clearedSameResident_producesPdfs() {
        // Test generating PDFs on the same resident multiple times
        byte[] pdf1 = pdfService.generate(testClearance, testResident, testSettings);
        byte[] pdf2 = pdfService.generate(testClearance, testResident, testSettings);

        assertTrue(isPdfValid(pdf1));
        assertTrue(isPdfValid(pdf2));
        // Both PDFs should be valid; PDFBox may include timestamps or other metadata,
        // so byte-for-byte equality is not guaranteed
        assertTrue(pdf1.length > 0);
        assertTrue(pdf2.length > 0);
    }

    // ─────────────── Helper Methods ───────────────────

    /**
     * Validates that the byte array is a proper PDF by checking:
     * 1. Starts with %PDF
     * 2. Contains %%EOF at the end
     */
    private boolean isPdfValid(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length < 4) {
            return false;
        }

        // Check PDF signature
        if (pdfBytes[0] != (byte) 0x25 ||
                pdfBytes[1] != 0x50 ||
                pdfBytes[2] != 0x44 ||
                pdfBytes[3] != 0x46) {
            return false;
        }

        // Check for PDF end marker (%%EOF)
        String pdfContent = new String(pdfBytes);
        return pdfContent.contains("%%EOF");
    }
}
