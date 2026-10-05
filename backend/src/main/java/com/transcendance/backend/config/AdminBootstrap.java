package com.transcendance.backend.config;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.transcendance.backend.model.User;
import com.transcendance.backend.services.UserRoleService;
import com.transcendance.backend.services.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Creates the administrator described by ADMIN_EMAIL, ADMIN_USERNAME and
 * ADMIN_PASSWORD at startup, and gives the ADMIN role to that account.
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LogManager.getLogger(AdminBootstrap.class);

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String DEFAULT_AVATAR_URL = "/api/default-avatar.png";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserService userService;
    private final UserRoleService userRoleService;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.username}")
    private String userName;

    @Value("${app.admin.password}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || userName.isBlank() || password.isBlank()) {
            log.info("No administrator configured (ADMIN_EMAIL, ADMIN_USERNAME, ADMIN_PASSWORD): none created");
            return;
        }

        Optional<User> existing = userService.getUserByEmail(email);
        if (existing.isPresent()) {
            userRoleService.setRole(existing.get(), ADMIN_ROLE);
            log.info("Administrator account already exists: {}", email);
            return;
        }

        if (userService.isInvalidEmail(email) || userService.isInvalidUsername(userName)
                || password.length() < MIN_PASSWORD_LENGTH) {
            log.error("Administrator not created: ADMIN_EMAIL, ADMIN_USERNAME or ADMIN_PASSWORD is invalid");
            return;
        }
        if (userService.isUserNameTaken(userName)) {
            log.error("Administrator not created: the username {} is already used", userName);
            return;
        }

        User admin = new User(userName, email, password);
        admin.setAvatarUrl(DEFAULT_AVATAR_URL);
        User savedAdmin = userService.createUser(admin);
        userRoleService.setRole(savedAdmin, ADMIN_ROLE);

        log.info("Administrator account created: {}", email);
    }
}
