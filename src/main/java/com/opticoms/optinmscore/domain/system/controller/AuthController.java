package com.opticoms.optinmscore.domain.system.controller;

import com.opticoms.optinmscore.domain.system.dto.AuthResponse;
import com.opticoms.optinmscore.domain.system.dto.LoginRequest;
import com.opticoms.optinmscore.domain.system.model.Permission;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
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
        String[] parts = request.getUsername().split("@", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String username = parts[0];
        String tenantId = parts[1];

        log.debug("Login attempt for user: {} in tenant: {}", username, tenantId);

        User domainUser = (User) userDetailsService.loadUserByUsernameAndTenantId(username, tenantId);

        if (!domainUser.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), domainUser.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        log.debug("Authentication successful: username={}, role={}, tenantId={}",
                domainUser.getUsername(), domainUser.getRole(), domainUser.getTenantId());

        String token = jwtService.generateToken(domainUser);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUsername(domainUser.getUsername());
        response.setEmail(domainUser.getEmail());
        response.setRole(domainUser.getRole().name());
        response.setTenantId(domainUser.getTenantId());
        response.setPermissions(Permission.forRole(domainUser.getRole()));

        return ResponseEntity.ok(response);
    }
}
