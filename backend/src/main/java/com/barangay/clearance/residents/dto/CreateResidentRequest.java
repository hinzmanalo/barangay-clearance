package com.barangay.clearance.residents.dto;

import com.barangay.clearance.residents.entity.Resident;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateResidentRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @NotNull(message = "Gender is required")
    private Resident.Gender gender;

    @NotBlank(message = "Address is required")
    private String address;

    @Size(max = 20)
    private String contactNumber;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;
}
