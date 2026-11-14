package com.placemate.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placemate.auth.dto.AuthResponse;
import com.placemate.auth.dto.LoginRequest;
import com.placemate.auth.dto.RegisterRequest;
import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Role role = parsePublicRole(request.role());
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        User user = new User(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                role
        );

        if (role == Role.STUDENT) {
            if (request.enrollmentNumber() == null || request.enrollmentNumber().isBlank()
                    || request.department() == null || request.department().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Enrollment number and department are required for student registration.");
            }
            user.setEnrollmentNumber(request.enrollmentNumber().trim());
            user.setDepartment(request.department().trim());
        } else {
            if (request.companyName() == null || request.companyName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Company name is required for recruiter registration.");
            }
            user.setCompanyName(request.companyName().trim());
            if (request.companyWebsite() != null && !request.companyWebsite().isBlank()) {
                user.setCompanyWebsite(request.companyWebsite().trim());
            }
        }

        User saved = userRepository.save(user);
        return createAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserSummary getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found."));
        return new AuthResponse.UserSummary(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getEnrollmentNumber(), user.getDepartment(),
                user.getCompanyName(), user.getCompanyWebsite()
        );
    }

    private AuthResponse createAuthResponse(User user) {
        return AuthResponse.from(jwtService.generateToken(user), jwtService.getExpirationMs(), user);
    }

    private Role parsePublicRole(String value) {
        try {
            Role role = Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (role == Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Admin accounts are provisioned by the placement team.");
            }
            return role;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be STUDENT or RECRUITER.");
        }
    }
}
