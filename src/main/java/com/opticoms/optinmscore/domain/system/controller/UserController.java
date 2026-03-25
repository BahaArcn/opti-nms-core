package com.opticoms.optinmscore.domain.system.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a new user (ADMIN only)")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            HttpServletRequest request,
            @Valid @RequestBody CreateUserRequest req) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        User user = userService.createUser(
                tenantId, req.getUsername(), req.getEmail(),
                req.getPassword(), req.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @Operation(summary = "List all users with pagination")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        Page<UserResponse> users = userService.listUsers(tenantId, pageable)
                .map(UserResponse::from);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            HttpServletRequest request,
            @PathVariable String userId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(UserResponse.from(userService.getUserById(tenantId, userId)));
    }

    @Operation(summary = "Update user role (ADMIN only)")
    @PutMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateRole(
            HttpServletRequest request,
            @PathVariable String userId,
            @Valid @RequestBody UpdateRoleRequest req) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        User user = userService.updateUserRole(tenantId, userId, req.getRole());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @Operation(summary = "Enable or disable a user (ADMIN only)")
    @PutMapping("/{userId}/status")
    public ResponseEntity<UserResponse> toggleActive(
            HttpServletRequest request,
            @PathVariable String userId,
            @Valid @RequestBody ToggleActiveRequest req) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        User user = userService.toggleUserActive(tenantId, userId, req.isActive());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @Operation(summary = "Change own password (any authenticated user)")
    @PutMapping("/{userId}/password")
    public ResponseEntity<Void> changePassword(
            HttpServletRequest request,
            @PathVariable String userId,
            @Valid @RequestBody ChangePasswordRequest req) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        userService.changePassword(tenantId, userId,
                req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Admin reset password for any user")
    @PutMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(
            HttpServletRequest request,
            @PathVariable String userId,
            @Valid @RequestBody ResetPasswordRequest req) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        userService.resetPassword(tenantId, userId, req.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete a user (ADMIN only)")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            HttpServletRequest request,
            @PathVariable String userId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        userService.deleteUser(tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    // --- DTOs ---

    @Data
    @NoArgsConstructor
    public static class CreateUserRequest {
        @NotBlank private String username;
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 8) private String password;
        @NotNull private User.Role role;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateRoleRequest {
        @NotNull private User.Role role;
    }

    @Data
    @NoArgsConstructor
    public static class ToggleActiveRequest {
        private boolean active;
    }

    @Data
    @NoArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank private String currentPassword;
        @NotBlank @Size(min = 8) private String newPassword;
    }

    @Data
    @NoArgsConstructor
    public static class ResetPasswordRequest {
        @NotBlank @Size(min = 8) private String newPassword;
    }

    @Data
    public static class UserResponse {
        private String id;
        private String username;
        private String email;
        private User.Role role;
        private boolean active;
        private String tenantId;
        private Long createdAt;

        public static UserResponse from(User user) {
            UserResponse r = new UserResponse();
            r.setId(user.getId());
            r.setUsername(user.getUsername());
            r.setEmail(user.getEmail());
            r.setRole(user.getRole());
            r.setActive(user.isActive());
            r.setTenantId(user.getTenantId());
            r.setCreatedAt(user.getCreatedAt());
            return r;
        }
    }
}
