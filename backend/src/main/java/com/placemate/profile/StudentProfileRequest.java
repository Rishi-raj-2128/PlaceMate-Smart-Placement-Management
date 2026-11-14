package com.placemate.profile;

import java.math.BigDecimal;
import java.util.Set;
import jakarta.validation.constraints.*;

public record StudentProfileRequest(
    @DecimalMin("0.00") @DecimalMax("10.00") BigDecimal cgpa,
    @Min(2000) @Max(2100) Integer graduationYear,
    @Size(max = 120) String degree,
    @Size(max = 120) String branch,
    @Size(max = 40) Set<@NotBlank @Size(max = 60) String> skills,
    @Size(max = 500) @Pattern(regexp = "^(https?://.*)?$", message = "Resume link must start with http:// or https://") String resumeUrl
) {}
