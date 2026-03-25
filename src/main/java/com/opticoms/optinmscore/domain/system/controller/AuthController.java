package com.opticoms.optinmscore.domain.system.controller;

import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Authenticate user", description = "Returns a JWT token for authenticated users. No authorization header required.")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login attempt for user: {} in tenant: {}", request.getUsername(), request.getTenantId());

        User domainUser = (User) userDetailsService.loadUserByUsernameAndTenantId(
                request.getUsername(), request.getTenantId());

        if (!domainUser.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), domainUser.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        log.debug("Authentication successful: username={}, role={}, tenantId={}",
                domainUser.getUsername(), domainUser.getRole(), domainUser.getTenantId());

        String token = jwtService.generateToken(domainUser);
        log.debug("JWT token generated successfully");

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUsername(domainUser.getUsername());
        response.setEmail(domainUser.getEmail());
        response.setRole(domainUser.getRole().name());
        response.setTenantId(domainUser.getTenantId());

        return ResponseEntity.ok(response);
    }

    // --- DTO CLASSES ---

    @Data
    @NoArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Tenant ID is required")
        private String tenantId;
        @NotBlank(message = "Username is required")
        private String username;
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String token;
        private String username;
        private String email;
        private String role;
        private String tenantId;
    }
}