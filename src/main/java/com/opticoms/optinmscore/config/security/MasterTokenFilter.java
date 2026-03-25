package com.opticoms.optinmscore.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MasterTokenFilter extends OncePerRequestFilter {

    private final String expectedToken;

    public MasterTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/slave/")) {
            String token = request.getHeader("X-Master-Token");
            if (token == null || !MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8),
                    expectedToken.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid master token");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
