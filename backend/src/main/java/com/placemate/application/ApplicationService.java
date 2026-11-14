package com.placemate.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placemate.job.Job;
import com.placemate.job.JobRepository;
import com.placemate.job.JobStatus;
import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;
import com.placemate.notification.NotificationService;

@Service
public class ApplicationService {
    private final ApplicationRepository applications;
    private final JobRepository jobs;
    private final UserRepository users;
    private final NotificationService notifications;

    public ApplicationService(ApplicationRepository applications, JobRepository jobs, UserRepository users, NotificationService notifications) {
        this.applications = applications;
        this.jobs = jobs;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public ApplicationResponse apply(String studentEmail, Long jobId) {
        User student = requireUser(studentEmail, Role.STUDENT);
        Job job = jobs.findById(jobId)
            .filter(j -> j.getStatus() == JobStatus.APPROVED)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approved job not found."));
        if (job.getDeadline() != null && job.getDeadline().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The application deadline has passed.");
        }
        if (applications.existsByStudentIdAndJobId(student.getId(), jobId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied for this job.");
        }
        try {
            Application saved = applications.saveAndFlush(new Application(student, job));
            notifications.create(job.getCreatedBy(), "APPLICATION", "New application received",
                student.getFullName() + " applied for " + job.getTitle() + ".", "/applicants?jobId=" + job.getId());
            return ApplicationResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied for this job.");
        }
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> myApplications(String studentEmail) {
        User student = requireUser(studentEmail, Role.STUDENT);
        return applications.findByStudentIdOrderByAppliedAtDesc(student.getId())
            .stream().map(ApplicationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> applicants(String recruiterEmail, Long jobId) {
        User recruiter = requireUser(recruiterEmail, Role.RECRUITER);
        Job job = jobs.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
        if (!job.getCreatedBy().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view applicants for your own jobs.");
        }
        return applications.findByJob_IdAndJob_CreatedBy_IdOrderByAppliedAtDesc(jobId, recruiter.getId())
            .stream().map(ApplicationResponse::from).toList();
    }

    @Transactional
    public ApplicationResponse updateStatus(String recruiterEmail, Long applicationId, ApplicationStatusRequest request) {
        User recruiter = requireUser(recruiterEmail, Role.RECRUITER);
        if (request.status() == ApplicationStatus.APPLIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recruiters cannot reset an application to APPLIED.");
        }
        Application application = applications.findById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));
        if (!application.getJob().getCreatedBy().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update applicants for your own jobs.");
        }
        application.updateStatus(request.status(), request.recruiterNote());
        Application saved = applications.save(application);
        notifications.create(application.getStudent(), "APPLICATION_UPDATE", "Application status updated",
            "Your application for " + application.getJob().getTitle() + " is now " + request.status().name().replace('_', ' ').toLowerCase() + ".", "/applications");
        return ApplicationResponse.from(saved);
    }

    private User requireUser(String email, Role role) {
        User user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account does not have permission for this action.");
        }
        return user;
    }
}
