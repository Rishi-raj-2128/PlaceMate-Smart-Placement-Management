package com.placemate.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String role,
        @Size(max = 80) String enrollmentNumber,
        @Size(max = 100) String department,
        @Size(max = 150) String companyName,
        @Size(max = 255) String companyWebsite
) {
}
