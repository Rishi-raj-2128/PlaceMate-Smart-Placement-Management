package com.placemate.auth.dto;

import com.placemate.user.Role;
import com.placemate.user.User;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public static AuthResponse from(String token, long expiresIn, User user) {
        return new AuthResponse(token, "Bearer", expiresIn, new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getEnrollmentNumber(),
                user.getDepartment(),
                user.getCompanyName(),
                user.getCompanyWebsite()
        ));
    }

    public record UserSummary(
            Long id,
            String fullName,
            String email,
            Role role,
            String enrollmentNumber,
            String department,
            String companyName,
            String companyWebsite
    ) {
    }
}
