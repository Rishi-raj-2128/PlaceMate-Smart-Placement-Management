package com.placemate.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ApplicationController {
    private final ApplicationService service;

    public ApplicationController(ApplicationService service) { this.service = service; }

    @PostMapping("/student/jobs/{jobId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(Authentication auth, @PathVariable Long jobId) {
        return service.apply(auth.getName(), jobId);
    }

    @GetMapping("/student/applications")
    public List<ApplicationResponse> mine(Authentication auth) {
        return service.myApplications(auth.getName());
    }

    @GetMapping("/recruiter/jobs/{jobId}/applications")
    public List<ApplicationResponse> applicants(Authentication auth, @PathVariable Long jobId) {
        return service.applicants(auth.getName(), jobId);
    }

    @PatchMapping("/recruiter/applications/{applicationId}/status")
    public ApplicationResponse updateStatus(
        Authentication auth,
        @PathVariable Long applicationId,
        @Valid @RequestBody ApplicationStatusRequest request
    ) {
        return service.updateStatus(auth.getName(), applicationId, request);
    }
}
