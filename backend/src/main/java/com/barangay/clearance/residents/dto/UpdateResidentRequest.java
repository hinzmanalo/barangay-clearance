package com.barangay.clearance.residents.dto;

import com.barangay.clearance.residents.entity.Resident;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateResidentRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String middleName;

    @Size(max = 100)
    private String lastName;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    private Resident.Gender gender;

    private String address;

    @Size(max = 20)
    private String contactNumber;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;

    private Resident.ResidentStatus status;

    /** Link or re-link to an existing portal user account. */
    private UUID userId;
}
