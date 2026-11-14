package com.placemate.application;

import java.time.Instant;

public record ApplicationResponse(
    Long id,
    Long jobId,
    String jobTitle,
    String companyName,
    String jobLocation,
    Long studentId,
    String studentName,
    String studentEmail,
    String enrollmentNumber,
    String department,
    ApplicationStatus status,
    String recruiterNote,
    Instant appliedAt,
    Instant updatedAt
) {
    public static ApplicationResponse from(Application application) {
        var student = application.getStudent();
        var job = application.getJob();
        return new ApplicationResponse(
            application.getId(), job.getId(), job.getTitle(), job.getCompanyName(),
            job.getLocation(), student.getId(), student.getFullName(), student.getEmail(),
            student.getEnrollmentNumber(), student.getDepartment(), application.getStatus(),
            application.getRecruiterNote(), application.getAppliedAt(), application.getUpdatedAt()
        );
    }
}
