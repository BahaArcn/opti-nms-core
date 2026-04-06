package com.opticoms.optinmscore.domain.system.mapper;

import com.opticoms.optinmscore.domain.system.dto.UserResponse;
import com.opticoms.optinmscore.domain.system.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserResponse toResponse(User entity);
    List<UserResponse> toResponseList(List<User> entities);
}
