package com.barangay.clearance.clearance.service.mapper;

import com.barangay.clearance.clearance.dto.ClearanceRequestDTO;
import com.barangay.clearance.clearance.entity.ClearanceRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between {@link ClearanceRequest} entity and
 * {@link ClearanceRequestDTO}.
 * <p>
 * {@code residentName} is a denormalised field that cannot be resolved by
 * MapStruct.
 * The service layer sets it after mapping via a separate call.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface ClearanceMapper {

    /**
     * Map entity → DTO. {@code residentName} is left null here; the service
     * enriches it from the Resident registry after mapping.
     */
    @Mapping(target = "residentName", ignore = true)
    ClearanceRequestDTO toDTO(ClearanceRequest entity);
}
