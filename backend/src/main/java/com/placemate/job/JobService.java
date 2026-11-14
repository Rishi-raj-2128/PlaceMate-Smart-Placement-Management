package com.placemate.job;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;

@Service
public class JobService {
    private final JobRepository jobs;
    private final UserRepository users;

    public JobService(JobRepository jobs, UserRepository users) {
        this.jobs = jobs;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<JobResponse> browse(String query, String location, String employmentType) {
        String q = normalize(query);
        String loc = normalize(location);
        String type = normalize(employmentType);
        return jobs.findByStatusOrderByCreatedAtDesc(JobStatus.APPROVED).stream()
            .filter(job -> q.isEmpty() || contains(job.getTitle(), q) ||
                    contains(job.getCompanyName(), q) || contains(job.getDescription(), q) ||
                    job.getRequiredSkills().stream().anyMatch(skill -> contains(skill, q)))
            .filter(job -> loc.isEmpty() || contains(job.getLocation(), loc))
            .filter(job -> type.isEmpty() || type.equals("all") ||
                    normalizeType(job.getEmploymentType()).equals(normalizeType(type)))
            .filter(job -> job.getDeadline() == null || !job.getDeadline().isBefore(java.time.LocalDate.now()))
            .map(JobResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public JobResponse getPublicJob(Long id) {
        Job job = jobs.findById(id)
            .filter(j -> j.getStatus() == JobStatus.APPROVED)
            .filter(j -> j.getDeadline() == null || !j.getDeadline().isBefore(java.time.LocalDate.now()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approved, non-expired job not found."));
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse create(String recruiterEmail, JobRequest request) {
        User recruiter = requireUser(recruiterEmail, Role.RECRUITER);
        validateSalary(request);
        Job job = new Job();
        job.setCreatedBy(recruiter);
        job.updateFrom(request);
        return JobResponse.from(jobs.save(job));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> myJobs(String recruiterEmail) {
        User recruiter = requireUser(recruiterEmail, Role.RECRUITER);
        return jobs.findByCreatedByIdOrderByCreatedAtDesc(recruiter.getId())
            .stream().map(JobResponse::from).toList();
    }

    @Transactional
    public JobResponse update(String recruiterEmail, Long id, JobRequest request) {
        User recruiter = requireUser(recruiterEmail, Role.RECRUITER);
        validateSalary(request);
        Job job = jobs.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
        if (!job.getCreatedBy().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit jobs posted by your account.");
        }
        job.updateFrom(request);
        return JobResponse.from(jobs.save(job));
    }

    @Transactional
    public void delete(String recruiterEmail, Long id) {
        User recruiter = requireUser(recruiterEmail, Role.RECRUITER);
        Job job = jobs.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
        if (!job.getCreatedBy().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete jobs posted by your account.");
        }
        jobs.delete(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> pendingJobs() {
        return jobs.findByStatusOrderByCreatedAtAsc(JobStatus.PENDING)
            .stream().map(JobResponse::from).toList();
    }

    @Transactional
    public JobResponse review(Long id, JobReviewRequest request) {
        if (request.status() == JobStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A review decision must be APPROVED or REJECTED.");
        }
        Job job = jobs.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
        job.setStatus(request.status());
        job.setReviewNote(request.reviewNote() == null ? null : request.reviewNote().trim());
        return JobResponse.from(jobs.save(job));
    }

    private User requireUser(String email, Role role) {
        User user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account does not have permission for this action.");
        }
        return user;
    }

    private void validateSalary(JobRequest request) {
        if (request.salaryMin() != null && request.salaryMax() != null &&
                request.salaryMin().compareTo(request.salaryMax()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum salary cannot exceed maximum salary.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeType(String value) {
        return normalize(value).replace("_", " ").replace("-", " ");
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
