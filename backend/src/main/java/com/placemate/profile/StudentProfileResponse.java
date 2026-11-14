package com.placemate.profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record StudentProfileResponse(
    Long userId,
    String fullName,
    String email,
    String enrollmentNumber,
    String department,
    BigDecimal cgpa,
    Integer graduationYear,
    String degree,
    String branch,
    Set<String> skills,
    String resumeUrl,
    Instant updatedAt
) {
    public static StudentProfileResponse from(StudentProfile profile) {
        var user = profile.getUser();
        return new StudentProfileResponse(user.getId(), user.getFullName(), user.getEmail(),
            user.getEnrollmentNumber(), user.getDepartment(), profile.getCgpa(),
            profile.getGraduationYear(), profile.getDegree(), profile.getBranch(),
            Set.copyOf(profile.getSkills()), profile.getResumeUrl(), profile.getUpdatedAt());
    }
}
