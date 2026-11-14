package com.placemate.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.placemate.auth.dto.AuthResponse;
import com.placemate.auth.dto.LoginRequest;
import com.placemate.auth.dto.RegisterRequest;
import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository users;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtService jwt;
    @InjectMocks private AuthService service;

    @Test
    void studentRegistrationNormalizesEmailAndHashesPassword() {
        when(users.existsByEmailIgnoreCase("student@example.com")).thenReturn(false);
        when(encoder.encode("StrongPass123")).thenReturn("hashed-password");
        when(jwt.generateToken(any(User.class))).thenReturn("test.jwt.token");
        when(jwt.getExpirationMs()).thenReturn(86_400_000L);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = service.register(new RegisterRequest(
            "  Aarav Sharma  ", " Student@Example.com ", "StrongPass123", "student",
            " CGC2026CS001 ", " Computer Science ", null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Aarav Sharma", saved.getFullName());
        assertEquals("student@example.com", saved.getEmail());
        assertEquals("hashed-password", saved.getPassword());
        assertEquals(Role.STUDENT, saved.getRole());
        assertEquals("CGC2026CS001", saved.getEnrollmentNumber());
        assertEquals("Computer Science", saved.getDepartment());
        assertEquals("Bearer", response.tokenType());
        assertEquals("test.jwt.token", response.token());
    }

    @Test
    void publicRegistrationRejectsAdminRole() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.register(new RegisterRequest("Admin", "admin@example.com",
                "StrongPass123", "ADMIN", null, null, null, null)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(users, never()).save(any());
    }

    @Test
    void registrationRejectsDuplicateEmail() {
        when(users.existsByEmailIgnoreCase("student@example.com")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.register(new RegisterRequest("Aarav", " Student@Example.com ",
                "StrongPass123", "STUDENT", "CGC001", "CSE", null, null)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(encoder, never()).encode(any());
        verify(users, never()).save(any());
    }

    @Test
    void studentRegistrationRequiresEnrollmentAndDepartment() {
        when(users.existsByEmailIgnoreCase("student@example.com")).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.register(new RegisterRequest("Aarav", "student@example.com",
                "StrongPass123", "STUDENT", null, "CSE", null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(users, never()).save(any());
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = new User("Aarav", "student@example.com", "stored-hash", Role.STUDENT);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("WrongPass123", "stored-hash")).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.login(new LoginRequest(" Student@Example.com ", "WrongPass123")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(jwt, never()).generateToken(any());
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.login(new LoginRequest("Missing@Example.com", "StrongPass123")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(encoder, never()).matches(any(), any());
    }
}
