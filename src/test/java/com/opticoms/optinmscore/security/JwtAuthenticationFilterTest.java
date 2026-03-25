package com.opticoms.optinmscore.security;

import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyString",
                "OptiNmsCoreDefaultJwtSecretKeyPleaseChangeInProduction2026");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
        jwtService.init();

        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_activeUser_setsAuthentication() throws Exception {
        User user = buildUser("operator1", true);
        String token = jwtService.generateToken(user);

        when(userDetailsService.loadUserByUsernameAndTenantId("operator1", TENANT)).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("operator1", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(TENANT, request.getAttribute("tenantId"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validToken_deactivatedUser_doesNotAuthenticate() throws Exception {
        User activeUser = buildUser("deactivated1", true);
        String token = jwtService.generateToken(activeUser);

        User deactivatedUser = buildUser("deactivated1", false);
        when(userDetailsService.loadUserByUsernameAndTenantId("deactivated1", TENANT)).thenReturn(deactivatedUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void noBearerToken_chainsWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void invalidToken_securityContextCleared() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer this.is.not.a.valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    private User buildUser(String username, boolean active) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setEmail(username + "@example.com");
        user.setRole(User.Role.OPERATOR);
        user.setTenantId(TENANT);
        user.setActive(active);
        return user;
    }
}
