package com.opticoms.optinmscore.domain.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @Schema(description = "Format: username@tenantId", example = "admin@TURK-0001/0001/01")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
