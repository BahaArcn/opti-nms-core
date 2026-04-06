package com.opticoms.optinmscore.domain.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank @Size(min = 8)
    private String newPassword;
}
