package com.barangay.clearance.pdf.service;

import com.barangay.clearance.clearance.entity.ClearanceRequest;
import com.barangay.clearance.residents.entity.Resident;
import com.barangay.clearance.settings.entity.BarangaySettings;

/**
 * Generates barangay clearance certificates as PDF documents.
 *
 * <p>
 * Implementations produce an A4 PDF containing:
 * <ul>
 * <li>Header with optional logo and barangay info</li>
 * <li>Clearance title and metadata (number, date, validity)</li>
 * <li>Body paragraph with resident details</li>
 * <li>Signature block with captain name</li>
 * </ul>
 * </p>
 */
public interface ClearancePdfService {

    /**
     * Generates a PDF byte array for the given clearance, resident, and settings.
     *
     * @param clearance the released clearance request (must have clearance number)
     * @param resident  the resident associated with the clearance
     * @param settings  barangay-wide settings (name, captain, logo)
     * @return the PDF document as a byte array
     */
    byte[] generate(ClearanceRequest clearance, Resident resident, BarangaySettings settings);
}
