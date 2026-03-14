package com.barangay.clearance.residents.service.mapper;

import com.barangay.clearance.residents.dto.CreateResidentRequest;
import com.barangay.clearance.residents.dto.ResidentDTO;
import com.barangay.clearance.residents.entity.Resident;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ResidentMapper {

    /**
     * Map a {@link Resident} entity to a {@link ResidentDTO}.
     * The {@code hasPortalAccount} field is computed from whether {@code userId} is
     * non-null. The {@code portalStatus} is set separately by the service.
     */
    @Mapping(target = "hasPortalAccount", expression = "java(resident.getUserId() != null)")
    @Mapping(target = "portalStatus", ignore = true)
    ResidentDTO toDTO(Resident resident);

    /**
     * Map a {@link CreateResidentRequest} to a new {@link Resident} entity.
     * {@code id}, {@code userId}, {@code createdAt}, and {@code updatedAt} are
     * ignored here and set by JPA / default values.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Resident toEntity(CreateResidentRequest request);
}
