package com.barangay.clearance.residents.dto;

import lombok.Data;

@Data
public class ResidentSearchRequest {

    /**
     * Free-text query matched against first name + last name (case-insensitive).
     */
    private String q;

    /** Filter by purok/zone — matched as a substring of the address field. */
    private String purok;
}
