package com.opticoms.optinmscore.domain.system.service;

import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = buildUser("testuser", "test@example.com", User.Role.OPERATOR);
    }

    @Test
    void createUser_success() {
        when(userRepository.existsByTenantIdAndUsername(TENANT, "testuser")).thenReturn(false);
        when(userRepository.existsByTenantIdAndEmail(TENANT, "test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pwd");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.createUser(TENANT, "testuser", "test@example.com",
                "password123", User.Role.OPERATOR);

        assertEquals(TENANT, result.getTenantId());
        assertEquals("testuser", result.getUsername());
        assertEquals("encoded-pwd", result.getPassword());
        assertEquals(User.Role.OPERATOR, result.getRole());
        assertTrue(result.isActive());
    }

    @Test
    void createUser_duplicateUsername_throwsConflict() {
        when(userRepository.existsByTenantIdAndUsername(TENANT, "testuser")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createUser(TENANT, "testuser", "test@example.com",
                        "password123", User.Role.OPERATOR));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void createUser_duplicateEmail_throwsConflict() {
        when(userRepository.existsByTenantIdAndUsername(TENANT, "testuser")).thenReturn(false);
        when(userRepository.existsByTenantIdAndEmail(TENANT, "test@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createUser(TENANT, "testuser", "test@example.com",
                        "password123", User.Role.OPERATOR));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void getUserById_found() {
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));

        User result = service.getUserById(TENANT, "user-1");

        assertEquals(user, result);
    }

    @Test
    void getUserById_notFound_throws404() {
        when(userRepository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getUserById(TENANT, "missing"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void listUsers_delegatesToRepo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findByTenantId(TENANT, pageable)).thenReturn(page);

        Page<User> result = service.listUsers(TENANT, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void countUsers_delegatesToRepo() {
        when(userRepository.countByTenantId(TENANT)).thenReturn(10L);

        assertEquals(10L, service.countUsers(TENANT));
    }

    @Test
    void updateUserRole_success() {
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.updateUserRole(TENANT, "user-1", User.Role.ADMIN);

        assertEquals(User.Role.ADMIN, result.getRole());
    }

    @Test
    void toggleUserActive_success() {
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.toggleUserActive(TENANT, "user-1", false);

        assertFalse(result.isActive());
    }

    @Test
    void changePassword_success() {
        user.setPassword("encoded-current");
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPwd", "encoded-current")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded-new");

        service.changePassword(TENANT, "user-1", "currentPwd", "newPassword");

        verify(userRepository).save(argThat(u -> "encoded-new".equals(u.getPassword())));
    }

    @Test
    void changePassword_wrongCurrent_throwsBadRequest() {
        user.setPassword("encoded-current");
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPwd", "encoded-current")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.changePassword(TENANT, "user-1", "wrongPwd", "newPassword"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void changePassword_shortNewPassword_throwsBadRequest() {
        user.setPassword("encoded-current");
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPwd", "encoded-current")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.changePassword(TENANT, "user-1", "currentPwd", "abc"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void resetPassword_success() {
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded-new");

        service.resetPassword(TENANT, "user-1", "newPassword");

        verify(userRepository).save(argThat(u -> "encoded-new".equals(u.getPassword())));
    }

    @Test
    void resetPassword_shortPassword_throwsBadRequest() {
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(TENANT, "user-1", "ab"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void deleteUser_success() {
        user.setUsername("operator1");
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));

        service.deleteUser(TENANT, "user-1");

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_adminUser_throwsForbidden() {
        user.setUsername("admin");
        when(userRepository.findByIdAndTenantId("user-1", TENANT)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteUser(TENANT, "user-1"));
        assertEquals(403, ex.getStatusCode().value());
    }

    private User buildUser(String username, String email, User.Role role) {
        User u = new User();
        u.setTenantId(TENANT);
        u.setUsername(username);
        u.setEmail(email);
        u.setRole(role);
        u.setActive(true);
        return u;
    }
}
