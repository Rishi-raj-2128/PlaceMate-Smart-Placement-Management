package com.placemate.job;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class JobController {
    private final JobService service;

    public JobController(JobService service) { this.service = service; }

    @GetMapping("/jobs")
    public List<JobResponse> browse(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String type
    ) {
        return service.browse(q, location, type);
    }

    @GetMapping("/jobs/{id}")
    public JobResponse get(@PathVariable Long id) { return service.getPublicJob(id); }

    @PostMapping("/recruiter/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(Authentication auth, @Valid @RequestBody JobRequest request) {
        return service.create(auth.getName(), request);
    }

    @GetMapping("/recruiter/jobs")
    public List<JobResponse> mine(Authentication auth) { return service.myJobs(auth.getName()); }

    @PutMapping("/recruiter/jobs/{id}")
    public JobResponse update(Authentication auth, @PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return service.update(auth.getName(), id, request);
    }

    @DeleteMapping("/recruiter/jobs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) { service.delete(auth.getName(), id); }

    @GetMapping("/admin/jobs/pending")
    public List<JobResponse> pending() { return service.pendingJobs(); }

    @PatchMapping("/admin/jobs/{id}/review")
    public JobResponse review(@PathVariable Long id, @Valid @RequestBody JobReviewRequest request) {
        return service.review(id, request);
    }
}
