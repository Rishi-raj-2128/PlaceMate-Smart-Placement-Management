package com.placemate.job;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record JobResponse(
    Long id,
    String title,
    String description,
    String companyName,
    String location,
    String employmentType,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    BigDecimal minCgpa,
    Set<String> requiredSkills,
    LocalDate deadline,
    JobStatus status,
    String reviewNote,
    String recruiterName,
    Instant createdAt,
    Instant updatedAt
) {
    public static JobResponse from(Job job) {
        return new JobResponse(job.getId(), job.getTitle(), job.getDescription(),
            job.getCompanyName(), job.getLocation(), job.getEmploymentType(),
            job.getSalaryMin(), job.getSalaryMax(), job.getMinCgpa(),
            Set.copyOf(job.getRequiredSkills()), job.getDeadline(), job.getStatus(),
            job.getReviewNote(), job.getCreatedBy().getFullName(),
            job.getCreatedAt(), job.getUpdatedAt());
    }
}
