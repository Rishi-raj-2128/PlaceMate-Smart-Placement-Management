package com.placemate.interview;

import java.time.Instant;
import jakarta.validation.constraints.*;

public record InterviewRequest(
    @NotNull @Future Instant scheduledAt,
    @NotNull @Min(15) @Max(480) Integer durationMinutes,
    @NotBlank @Pattern(regexp="(?i)ONLINE|IN_PERSON|PHONE") String mode,
    @Size(max=500) @Pattern(regexp="^(https?://.*)?$") String meetingLink,
    @Size(max=250) String location,
    @Size(max=1000) String notes
) {}
