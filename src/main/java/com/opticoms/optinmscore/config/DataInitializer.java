package com.opticoms.optinmscore.config;

import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.password:#{null}}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            String password;
            if (defaultAdminPassword != null) {
                password = defaultAdminPassword;
            } else {
                password = java.util.UUID.randomUUID().toString();
                log.warn("DEFAULT_ADMIN_PASSWORD env var not set. "
                       + "A random password was generated. "
                       + "Use the reset-password API to set a known password.");
            }

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(password));
            admin.setEmail("admin@opticoms.com");
            admin.setRole(User.Role.SUPER_ADMIN);
            admin.setActive(true);
            admin.setSystemProtected(true);
            admin.setTenantId("PLAT-0000/0000/00");

            userRepository.save(admin);
            log.info("Default admin user created. Change the password immediately via API.");

        }
    }
}