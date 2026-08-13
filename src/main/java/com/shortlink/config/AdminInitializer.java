package com.shortlink.config;

import com.shortlink.user.Role;
import com.shortlink.user.User;
import com.shortlink.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes the default system administrator account upon application startup if it does not already exist.
 * Hardcoded credentials without external yml configuration requirements.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    public static final String DEFAULT_ADMIN_NAME = "Anurag Vishwakarma";
    public static final String DEFAULT_ADMIN_EMAIL = "anuragvishwakarma546@gmail.com";
    public static final String DEFAULT_ADMIN_PASSWORD = "anurag@12345D";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByEmail(DEFAULT_ADMIN_EMAIL).ifPresentOrElse(
                existingAdmin -> {
                    boolean updated = false;
                    if (existingAdmin.getRole() != Role.ADMIN) {
                        existingAdmin.setRole(Role.ADMIN);
                        updated = true;
                    }
                    if (!existingAdmin.isEnabled()) {
                        existingAdmin.setEnabled(true);
                        updated = true;
                    }
                    if (existingAdmin.isDeletionPending()) {
                        existingAdmin.setDeletionPending(false);
                        existingAdmin.setDeletionRequestedAt(null);
                        existingAdmin.setDeletionScheduledAt(null);
                        updated = true;
                    }
                    if (updated) {
                        userRepository.save(existingAdmin);
                        log.info("Updated existing ADMIN account state: email={}", DEFAULT_ADMIN_EMAIL);
                    } else {
                        log.info("ADMIN account verified and ready: email={}", DEFAULT_ADMIN_EMAIL);
                    }
                },
                () -> {
                    User admin = User.builder()
                            .name(DEFAULT_ADMIN_NAME)
                            .email(DEFAULT_ADMIN_EMAIL)
                            .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                            .role(Role.ADMIN)
                            .enabled(true)
                            .deletionPending(false)
                            .build();

                    userRepository.save(admin);
                    log.info("Successfully initialized default ADMIN account: email={}", DEFAULT_ADMIN_EMAIL);
                }
        );
    }
}
