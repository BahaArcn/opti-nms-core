package com.opticoms.optinmscore.security;

import com.opticoms.optinmscore.domain.system.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyString",
                "TestJwtSecretKeyForUnitTesting2026Pad");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
        jwtService.init();
    }

    @Test
    void generateToken_andExtractUsername() {
        User user = buildUser("testuser", "OPTC-0001/0001/01");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("testuser", jwtService.extractUsername(token));
    }

    @Test
    void generateToken_containsTenantId() {
        User user = buildUser("testuser", "OPTC-0001/0001/01");

        String token = jwtService.generateToken(user);

        assertEquals("OPTC-0001/0001/01", jwtService.extractTenantId(token));
    }

    @Test
    void generateToken_containsRoles() {
        User user = buildUser("admin", "OPTC-0001/0001/01");
        user.setRole(User.Role.ADMIN);

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        String roles = jwtService.extractClaim(token, claims -> claims.get("roles").toString());
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        User user = buildUser("testuser", "OPTC-0001/0001/01");
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_wrongUser_returnsFalse() {
        User user = buildUser("testuser", "OPTC-0001/0001/01");
        User otherUser = buildUser("otheruser", "OPTC-0001/0001/01");
        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void isTokenValid_expiredToken_throwsOrReturnsFalse() {
        JwtService shortLived = new JwtService();
        ReflectionTestUtils.setField(shortLived, "secretKeyString",
                "TestJwtSecretKeyForUnitTesting2026Pad");
        ReflectionTestUtils.setField(shortLived, "jwtExpiration", -1000L);
        shortLived.init();

        User user = buildUser("testuser", "OPTC-0001/0001/01");
        String token = shortLived.generateToken(user);

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.isTokenValid(token, user));
    }

    @Test
    void generateToken_withExtraClaims() {
        User user = buildUser("testuser", "OPTC-0001/0001/01");
        Map<String, Object> claims = new HashMap<>();
        claims.put("customField", "customValue");

        String token = jwtService.generateToken(claims, user);

        String customField = jwtService.extractClaim(token,
                c -> c.get("customField", String.class));
        assertEquals("customValue", customField);
    }

    @Test
    void extractClaim_emailClaim() {
        User user = buildUser("testuser", "OPTC-0001/0001/01");
        user.setEmail("test@example.com");

        String token = jwtService.generateToken(user);

        String email = jwtService.extractClaim(token, c -> c.get("email", String.class));
        assertEquals("test@example.com", email);
    }

    private User buildUser(String username, String tenantId) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setEmail(username + "@example.com");
        user.setRole(User.Role.OPERATOR);
        user.setTenantId(tenantId);
        user.setActive(true);
        return user;
    }
}
