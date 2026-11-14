package com.placemate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;

/**
 * Creates an admin account only when an explicitly enabled isolated E2E
 * environment supplies credentials. Public registration remains restricted.
 */
@Component
@ConditionalOnProperty(prefix = "placemate.e2e.admin", name = "enabled", havingValue = "true")
public class E2eAdminInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public E2eAdminInitializer(
        UserRepository users,
        PasswordEncoder passwordEncoder,
        @Value("${PLACEMATE_E2E_ADMIN_EMAIL}") String email,
        @Value("${PLACEMATE_E2E_ADMIN_PASSWORD}") String password
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email == null || email.isBlank() || password == null || password.length() < 12) {
            throw new IllegalStateException("E2E admin credentials must be supplied and the password must contain at least 12 characters.");
        }
        if (users.findByEmailIgnoreCase(email).isEmpty()) {
            users.save(new User("PlaceMate E2E Admin", email.trim().toLowerCase(), passwordEncoder.encode(password), Role.ADMIN));
        }
    }
}
