package com.placemate.job;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.*;

public record JobRequest(
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 10000) String description,
    @NotBlank @Size(max = 150) String companyName,
    @NotBlank @Size(max = 150) String location,
    @NotBlank @Size(max = 40) String employmentType,
    @DecimalMin("0.0") BigDecimal salaryMin,
    @DecimalMin("0.0") BigDecimal salaryMax,
    @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal minCgpa,
    @Size(max = 20) List<@Size(max = 60) String> requiredSkills,
    @FutureOrPresent LocalDate deadline
) {}
