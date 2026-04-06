package com.opticoms.optinmscore.domain.system.dto;

import com.opticoms.optinmscore.domain.system.model.User;
import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private User.Role role;
    private boolean active;
    private String tenantId;
    private Long createdAt;
    private Long updatedAt;
}
