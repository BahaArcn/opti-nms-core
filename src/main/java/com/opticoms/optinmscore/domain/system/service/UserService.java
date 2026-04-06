package com.opticoms.optinmscore.domain.system.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MIN_PASSWORD_LENGTH = 8;

    @Audited(action = AuditAction.CREATE, entityType = "User")
    public User createUser(String tenantId, String username, String email,
                           String password, User.Role role) {
        if (role == User.Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "SUPER_ADMIN role cannot be assigned via tenant user management");
        }
        validatePassword(password);

        if (userRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username already exists: " + username);
        }
        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already exists: " + email);
        }

        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);

        log.info("Creating user: username={}, role={}, tenant={}", username, role, tenantId);
        return userRepository.save(user);
    }

    public User getUserById(String tenantId, String userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + userId));
    }

    public Page<User> listUsers(String tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable);
    }

    public long countUsers(String tenantId) {
        return userRepository.countByTenantId(tenantId);
    }

    @Audited(action = AuditAction.UPDATE, entityType = "User")
    public User updateUserRole(String tenantId, String userId, User.Role newRole) {
        if (newRole == User.Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "SUPER_ADMIN role cannot be assigned via tenant user management");
        }
        User user = getUserById(tenantId, userId);
        user.setRole(newRole);
        log.info("Updating role for user {}: {} -> {}", user.getUsername(), user.getRole(), newRole);
        return userRepository.save(user);
    }

    @Audited(action = AuditAction.STATUS_CHANGE, entityType = "User")
    public User toggleUserActive(String tenantId, String userId, boolean active) {
        User user = getUserById(tenantId, userId);
        user.setActive(active);
        log.info("User {} active status set to {}", user.getUsername(), active);
        return userRepository.save(user);
    }

    @Audited(action = AuditAction.CHANGE_PASSWORD, entityType = "User")
    public void changePassword(String tenantId, String userId,
                               String currentPassword, String newPassword) {
        User user = getUserById(tenantId, userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        validatePassword(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

    @Audited(action = AuditAction.RESET_PASSWORD, entityType = "User")
    public void resetPassword(String tenantId, String userId, String newPassword) {
        User user = getUserById(tenantId, userId);
        validatePassword(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset by admin for user: {}", user.getUsername());
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }

    @Audited(action = AuditAction.DELETE, entityType = "User")
    public void deleteUser(String tenantId, String userId) {
        User user = getUserById(tenantId, userId);
        if (user.isSystemProtected()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This system account cannot be deleted");
        }
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
    }
}
