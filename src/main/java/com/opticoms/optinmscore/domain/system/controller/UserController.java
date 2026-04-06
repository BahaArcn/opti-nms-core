package com.opticoms.optinmscore.domain.system.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.system.dto.*;
import com.opticoms.optinmscore.domain.system.mapper.UserMapper;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(summary = "Create a new user (ADMIN only)")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            HttpServletRequest request,
            @Valid @RequestBody CreateUserRequest req) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        User user = userService.createUser(
                tenantId, req.getUsername(), req.getEmail(),
                req.getPassword(), req.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(user));
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
                .map(userMapper::toResponse);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            HttpServletRequest request,
            @PathVariable String userId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(userMapper.toResponse(userService.getUserById(tenantId, userId)));
    }

    @Operation(summary = "Update user role (ADMIN only)")
    @PutMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateRole(
            HttpServletRequest request,
            @PathVariable String userId,
            @Valid @RequestBody UpdateRoleRequest req) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        User user = userService.updateUserRole(tenantId, userId, req.getRole());
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @Operation(summary = "Enable or disable a user (ADMIN only)")
    @PutMapping("/{userId}/status")
    public ResponseEntity<UserResponse> toggleActive(
            HttpServletRequest request,
            @PathVariable String userId,
            @Valid @RequestBody ToggleActiveRequest req) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        User user = userService.toggleUserActive(tenantId, userId, req.isActive());
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @Operation(summary = "Change own password (any authenticated user)")
    @PutMapping("/{userId}/password")
    public ResponseEntity<Void> changePassword(
            HttpServletRequest request,
            @PathVariable String userId,
            @Valid @RequestBody ChangePasswordRequest req) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        User principal = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (!principal.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only change your own password");
        }
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
}
