package com.placemate.admin;
import jakarta.validation.constraints.NotNull;
public record CompanyVerificationRequest(@NotNull Boolean verified) {}
