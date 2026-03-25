package com.opticoms.optinmscore.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class RequestLoggingConfig {

    @Bean
    public ApiRequestLoggingFilter apiRequestLoggingFilter() {
        return new ApiRequestLoggingFilter();
    }

    @Slf4j
    public static class ApiRequestLoggingFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            long startTime = System.currentTimeMillis();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String tenantId = (String) request.getAttribute("tenantId");
            String remoteAddr = request.getRemoteAddr();

            try {
                filterChain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                int status = response.getStatus();

                if (uri.startsWith("/api/")) {
                    log.info("HTTP {} {} | status={} | tenant={} | ip={} | duration={}ms",
                            method, uri, status, tenantId, remoteAddr, duration);
                }

                if (status >= 400 && uri.startsWith("/api/")) {
                    log.warn("HTTP {} {} returned error status={} | tenant={} | ip={} | duration={}ms",
                            method, uri, status, tenantId, remoteAddr, duration);
                }
            }
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            return path.startsWith("/swagger") ||
                    path.startsWith("/v3/api-docs") ||
                    path.startsWith("/webjars") ||
                    path.startsWith("/actuator");
        }
    }
}
