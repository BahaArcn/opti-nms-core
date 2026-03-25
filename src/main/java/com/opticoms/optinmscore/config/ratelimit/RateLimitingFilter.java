package com.opticoms.optinmscore.config.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limiting filter placed AFTER authentication so that the resolved
 * principal (username) can be used as the bucket key for authenticated users.
 * Unauthenticated requests are keyed by client IP.
 *
 * Auth endpoints (/api/v1/auth/**) use stricter limits to mitigate brute force.
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties props;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled()) return true;
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/webjars");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isAuthEndpoint = path.startsWith("/api/v1/auth");

        int maxRequests = isAuthEndpoint ? props.getAuthRequestsPerWindow() : props.getRequestsPerWindow();
        int windowSeconds = isAuthEndpoint ? props.getAuthWindowSeconds() : props.getWindowSeconds();

        String key = resolveKey(request, isAuthEndpoint);

        RateLimiter.ConsumeResult result = rateLimiter.tryConsume(key, maxRequests, windowSeconds);

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(result.remaining(), 0)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetEpochSecond()));

        if (!result.allowed()) {
            log.warn("Rate limit exceeded for key={} on path={}", key, path);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Rate limit exceeded. Try again after "
                    + result.resetEpochSecond() + ".\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request, boolean isAuthEndpoint) {
        if (isAuthEndpoint) {
            return "auth:" + getClientIp(request);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }

        return "ip:" + getClientIp(request);
    }

    /**
     * Returns the client IP from the servlet request.
     * Uses {@code remoteAddr} directly to prevent X-Forwarded-For spoofing.
     * If a trusted reverse proxy is in front, configure
     * {@code server.forward-headers-strategy=NATIVE} so that the container
     * resolves the real client IP into {@code remoteAddr}.
     */
    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
