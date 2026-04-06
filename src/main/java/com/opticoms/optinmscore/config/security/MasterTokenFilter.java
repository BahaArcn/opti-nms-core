package com.opticoms.optinmscore.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class MasterTokenFilter extends OncePerRequestFilter {

    private final String expectedToken;

    private static final List<String> MASTER_TOKEN_PATHS = List.of(
            "/api/v1/slave/",
            "/api/v1/master/slaves/register",
            "/api/v1/master/slaves/heartbeat"
    );

    public MasterTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (requiresMasterToken(path)) {
            String token = request.getHeader("X-Master-Token");
            if (token == null || !MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8),
                    expectedToken.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid master token");
                return;
            }

            String tenantId = request.getHeader("X-Tenant-ID");
            if (tenantId != null && !tenantId.isBlank()) {
                request.setAttribute("tenantId", tenantId);
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "MASTER_NODE", null,
                    List.of(new SimpleGrantedAuthority("ROLE_MASTER_NODE"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresMasterToken(String path) {
        for (String tokenPath : MASTER_TOKEN_PATHS) {
            if (path.startsWith(tokenPath)) {
                return true;
            }
        }
        return false;
    }
}
