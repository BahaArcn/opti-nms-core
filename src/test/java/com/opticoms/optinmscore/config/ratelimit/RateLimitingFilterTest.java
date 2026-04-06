package com.opticoms.optinmscore.config.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock private RateLimiter rateLimiter;
    @Mock private FilterChain filterChain;

    private RateLimitProperties props;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        props = new RateLimitProperties();
        props.setEnabled(true);
        props.setRequestsPerWindow(100);
        props.setWindowSeconds(60);
        props.setAuthRequestsPerWindow(10);
        props.setAuthWindowSeconds(60);
        filter = new RateLimitingFilter(rateLimiter, props);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowedRequest_passesThrough_withHeaders() throws ServletException, IOException {
        when(rateLimiter.tryConsume(anyString(), eq(100), eq(60)))
                .thenReturn(new RateLimiter.ConsumeResult(true, 99, 1700000060L, 100));

        var request = apiRequest("/api/v1/subscribers/list");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
        assertThat(response.getHeader("X-RateLimit-Reset")).isEqualTo("1700000060");
    }

    @Test
    void rateLimitExceeded_returns429() throws ServletException, IOException {
        when(rateLimiter.tryConsume(anyString(), eq(100), eq(60)))
                .thenReturn(new RateLimiter.ConsumeResult(false, 0, 1700000060L, 100));

        var request = apiRequest("/api/v1/subscribers/list");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Too Many Requests");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    }

    @Test
    void authEndpoint_usesStricterLimits() throws ServletException, IOException {
        when(rateLimiter.tryConsume(startsWith("auth:"), eq(10), eq(60)))
                .thenReturn(new RateLimiter.ConsumeResult(true, 9, 1700000060L, 10));

        var request = apiRequest("/api/v1/auth/login");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimiter).tryConsume(startsWith("auth:"), eq(10), eq(60));
        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
    }

    @Test
    void authenticatedUser_keyedByUsername() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, Collections.emptyList()));

        when(rateLimiter.tryConsume(eq("user:admin"), eq(100), eq(60)))
                .thenReturn(new RateLimiter.ConsumeResult(true, 50, 1700000060L, 100));

        var request = apiRequest("/api/v1/subscribers/list");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimiter).tryConsume("user:admin", 100, 60);
    }

    @Test
    void unauthenticatedRequest_keyedByIp() throws ServletException, IOException {
        when(rateLimiter.tryConsume(eq("ip:192.168.1.100"), eq(100), eq(60)))
                .thenReturn(new RateLimiter.ConsumeResult(true, 99, 1700000060L, 100));

        var request = apiRequest("/api/v1/subscribers/list");
        request.setRemoteAddr("192.168.1.100");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimiter).tryConsume("ip:192.168.1.100", 100, 60);
    }

    @Test
    void xForwardedFor_ignoredUsesRemoteAddr() throws ServletException, IOException {
        when(rateLimiter.tryConsume(eq("ip:127.0.0.1"), eq(100), eq(60)))
                .thenReturn(new RateLimiter.ConsumeResult(true, 99, 1700000060L, 100));

        var request = apiRequest("/api/v1/subscribers/list");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 172.16.0.1");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimiter).tryConsume("ip:127.0.0.1", 100, 60);
    }

    @Test
    void swaggerPath_skipsFilter() throws ServletException, IOException {
        var request = apiRequest("/swagger-ui/index.html");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void actuatorPath_skipsFilter() throws ServletException, IOException {
        var request = apiRequest("/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void disabledRateLimit_skipsFilter() throws ServletException, IOException {
        props.setEnabled(false);
        var request = apiRequest("/api/v1/subscribers/list");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void remainingNegative_clampedToZero() throws ServletException, IOException {
        when(rateLimiter.tryConsume(anyString(), eq(100), eq(60)))
                .thenReturn(new RateLimiter.ConsumeResult(false, -1, 1700000060L, 100));

        var request = apiRequest("/api/v1/subscribers/list");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private MockHttpServletRequest apiRequest(String path) {
        var request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
