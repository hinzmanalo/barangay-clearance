package com.barangay.clearance.residents.dto;

import com.barangay.clearance.residents.entity.Resident;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ResidentDTO {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate birthDate;
    private Resident.Gender gender;
    private String address;
    private String contactNumber;
    private String email;
    private Resident.ResidentStatus status;

    /** True when this resident has a linked portal user account. */
    private boolean hasPortalAccount;

    private Instant createdAt;
    private Instant updatedAt;
}
