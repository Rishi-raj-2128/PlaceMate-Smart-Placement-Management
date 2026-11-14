package com.placemate.admin;

public record AdminOverviewResponse(long students,long recruiters,long companies,long jobsPending,long jobsApproved,
 long applications,long shortlisted,long hired) {}
