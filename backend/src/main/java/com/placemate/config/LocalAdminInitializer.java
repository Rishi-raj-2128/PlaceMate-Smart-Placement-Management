package com.placemate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;

/**
 * Provisions the convenience admin account only when the explicit local
 * development profile is active. Never activate this profile in production.
 */
@Component
@Profile("local")
public class LocalAdminInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public LocalAdminInitializer(
        UserRepository users,
        PasswordEncoder passwordEncoder,
        @Value("${PLACEMATE_LOCAL_ADMIN_EMAIL:admin@gmail.com}") String email,
        @Value("${PLACEMATE_LOCAL_ADMIN_PASSWORD:Admin@123}") String password
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email == null || email.isBlank() || password == null || password.length() < 8) {
            throw new IllegalStateException("Local admin credentials must be supplied; password must contain at least 8 characters.");
        }

        users.findByEmailIgnoreCase(email.trim()).ifPresentOrElse(existing -> {
            if (existing.getRole() != Role.ADMIN) {
                throw new IllegalStateException("The configured local admin email already belongs to a non-admin account.");
            }
        }, () -> users.save(new User(
            "PlaceMate Administrator",
            email.trim().toLowerCase(java.util.Locale.ROOT),
            passwordEncoder.encode(password),
            Role.ADMIN
        )));
    }
}
