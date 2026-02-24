package com.barangay.clearance.identity.dto;

import com.barangay.clearance.residents.entity.Resident;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

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
}
