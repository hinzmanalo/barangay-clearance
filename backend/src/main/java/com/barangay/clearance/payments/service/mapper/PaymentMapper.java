package com.barangay.clearance.payments.service.mapper;

import com.barangay.clearance.payments.dto.PaymentDTO;
import com.barangay.clearance.payments.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between {@link Payment} entity and {@link PaymentDTO}.
 *
 * <p>
 * The {@code idempotent} flag is not persisted in the entity; it is set
 * programmatically by the service layer before returning the DTO.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper {

    /**
     * Maps a {@link Payment} entity to a {@link PaymentDTO}.
     * The {@code idempotent} field defaults to {@code false} and must be
     * set explicitly by the caller when serving a cached replay.
     */
    @Mapping(target = "idempotent", constant = "false")
    PaymentDTO toDTO(Payment payment);
}
