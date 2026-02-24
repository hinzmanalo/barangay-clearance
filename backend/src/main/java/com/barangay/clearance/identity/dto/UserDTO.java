package com.barangay.clearance.identity.dto;

import com.barangay.clearance.identity.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDTO {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private User.Role role;
    private User.UserStatus status;
    private boolean mustChangePassword;
    private Instant createdAt;
}
