package com.opticoms.optinmscore.domain.system.dto;

import com.opticoms.optinmscore.domain.system.model.User;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    @NotNull
    private User.Role role;
}
