package com.barangay.clearance.residents.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "residents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Optional link to a portal user account. Null for walk-in residents. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResidentStatus status = ResidentStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum ResidentStatus {
        ACTIVE, INACTIVE
    }
}
