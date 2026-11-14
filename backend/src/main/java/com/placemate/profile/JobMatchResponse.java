package com.placemate.profile;

import java.util.List;
import com.placemate.job.JobResponse;

public record JobMatchResponse(
    JobResponse job,
    boolean eligible,
    int matchPercentage,
    List<String> reasons
) {}
