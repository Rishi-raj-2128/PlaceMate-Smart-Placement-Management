package com.placemate.job;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JobReviewRequest(
    @NotNull JobStatus status,
    @Size(max = 1000) String reviewNote
) {}
