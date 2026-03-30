package com.opticoms.optinmscore.config;

import com.opticoms.optinmscore.config.ratelimit.RateLimiter;
import com.opticoms.optinmscore.config.ratelimit.RateLimitProperties;
import com.opticoms.optinmscore.config.ratelimit.RateLimitingFilter;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final CustomUserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Subscriber management: ADMIN only for write, all roles for read
                        .requestMatchers(HttpMethod.POST, "/api/v1/subscriber/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/subscriber/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/subscriber/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/subscriber/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // Network configuration: ADMIN only for write, all roles for read
                        .requestMatchers(HttpMethod.PUT, "/api/v1/network/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/network/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/network/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // Alarms: all authenticated users can read, ADMIN + OPERATOR can write/acknowledge
                        .requestMatchers(HttpMethod.POST, "/api/v1/fault/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/fault/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/fault/**").authenticated()

                        // Performance metrics: all authenticated users can read
                        .requestMatchers(HttpMethod.GET, "/api/v1/performance/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/performance/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Dashboard: all authenticated users
                        .requestMatchers("/api/v1/dashboard/**").authenticated()

                        // User management: ADMIN only for create/delete/role, password change for self
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/reset-password").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/password").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // SUCI profile management: ADMIN for write, ADMIN+OPERATOR for read
                        .requestMatchers(HttpMethod.POST, "/api/v1/suci/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/suci/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/suci/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/suci/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Certificate management: ADMIN for write, ADMIN+OPERATOR for read
                        // (private key icerdigi icin VIEWER okuyamaz — SUCI ile ayni pattern)
                        .requestMatchers(HttpMethod.POST, "/api/v1/certificates/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/certificates/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/certificates/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/certificates/**").hasAnyRole("ADMIN", "OPERATOR")

                        // APN/DNN profile management: ADMIN for write, all roles for read
                        // (hassas veri degil, network config — subscriber pattern ile ayni)
                        .requestMatchers(HttpMethod.POST, "/api/v1/apn/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/apn/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/apn/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/apn/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // Edge location management: ADMIN for write, all roles for read
                        .requestMatchers(HttpMethod.POST, "/api/v1/edge-locations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/edge-locations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/edge-locations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/edge-locations/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // Policy management: ADMIN for write, OPERATOR+ for read
                        .requestMatchers(HttpMethod.POST, "/api/v1/policies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/policies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/policies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/policies/**").hasAnyRole("ADMIN", "OPERATOR")

                        // License management: ADMIN for write, OPERATOR+ for read/status
                        .requestMatchers(HttpMethod.POST, "/api/v1/licenses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/licenses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/licenses/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Firewall management: ADMIN only (OS-level iptables)
                        .requestMatchers("/api/v1/firewall/**").hasRole("ADMIN")

                        // Open5GS deploy: ADMIN only (K8s deploy + rollout restart)
                        .requestMatchers("/api/v1/open5gs/**").hasRole("ADMIN")

                        // Inventory: all authenticated users
                        .requestMatchers("/api/v1/inventory/**").authenticated()

                        // Master endpoints: ADMIN only
                        .requestMatchers("/api/v1/master/**").hasRole("ADMIN")

                        // Slave endpoints: MasterTokenFilter tarafindan korunuyor, JWT bypass
                        .requestMatchers("/api/v1/slave/**").permitAll()

                        // Tenant management: SUPER_ADMIN only
                        .requestMatchers("/api/v1/system/tenants/**").hasRole("SUPER_ADMIN")

                        // System endpoints: SUPER_ADMIN only
                        .requestMatchers("/api/v1/system/**").hasRole("SUPER_ADMIN")

                        // Audit logs: ADMIN only (hassas guvenlik verisi)
                        .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")

                        // Actuator (except health/info): ADMIN only
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new RateLimitingFilter(rateLimiter, rateLimitProperties), JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID"));
        configuration.setExposedHeaders(List.of(
                "X-Total-Count", "X-Total-Pages",
                "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}