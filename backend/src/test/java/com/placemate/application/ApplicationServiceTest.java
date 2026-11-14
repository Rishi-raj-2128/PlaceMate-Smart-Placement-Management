package com.placemate.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import com.placemate.job.Job;
import com.placemate.job.JobRepository;
import com.placemate.job.JobStatus;
import com.placemate.notification.NotificationService;
import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceTest {
    @Mock private ApplicationRepository applications;
    @Mock private JobRepository jobs;
    @Mock private UserRepository users;
    @Mock private NotificationService notifications;
    @InjectMocks private ApplicationService service;

    @Test
    void studentCanApplyToApprovedJobAndRecruiterIsNotified() {
        User student = user(1L, Role.STUDENT);
        User recruiter = user(2L, Role.RECRUITER);
        Job job = job(7L, recruiter, JobStatus.APPROVED);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(jobs.findById(7L)).thenReturn(Optional.of(job));
        when(applications.existsByStudentIdAndJobId(1L, 7L)).thenReturn(false);
        when(applications.saveAndFlush(any(Application.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = service.apply("student@example.com", 7L);

        assertEquals(ApplicationStatus.APPLIED, response.status());
        verify(applications).saveAndFlush(any(Application.class));
        verify(notifications).create(eq(recruiter), eq("APPLICATION"),
            eq("New application received"), contains("applied for"), eq("/applicants?jobId=7"));
    }

    @Test
    void duplicateApplicationIsRejected() {
        User student = user(1L, Role.STUDENT);
        Job job = job(7L, user(2L, Role.RECRUITER), JobStatus.APPROVED);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(jobs.findById(7L)).thenReturn(Optional.of(job));
        when(applications.existsByStudentIdAndJobId(1L, 7L)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.apply("student@example.com", 7L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(applications, never()).saveAndFlush(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void applicationToUnapprovedJobIsRejected() {
        User student = user(1L, Role.STUDENT);
        User recruiter = user(2L, Role.RECRUITER);
        Job pendingJob = job(7L, recruiter, JobStatus.PENDING);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(jobs.findById(7L)).thenReturn(Optional.of(pendingJob));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.apply("student@example.com", 7L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(applications, notifications);
    }

    @Test
    void expiredJobCannotReceiveApplications() {
        User student = user(1L, Role.STUDENT);
        Job job = job(7L, user(2L, Role.RECRUITER), JobStatus.APPROVED);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(jobs.findById(7L)).thenReturn(Optional.of(job));
        doReturn(LocalDate.now().minusDays(1)).when(job).getDeadline();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.apply("student@example.com", 7L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void recruiterCannotUpdateAnotherRecruitersApplication() {
        User recruiter = user(2L, Role.RECRUITER);
        User owner = user(3L, Role.RECRUITER);
        Job job = job(7L, owner, JobStatus.APPROVED);
        Application application = mock(Application.class);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applications.findById(9L)).thenReturn(Optional.of(application));
        when(application.getJob()).thenReturn(job);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.updateStatus("recruiter@example.com", 9L,
                new ApplicationStatusRequest(ApplicationStatus.SHORTLISTED, "Selected")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(applications, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void recruiterCannotResetApplicationToApplied() {
        User recruiter = user(2L, Role.RECRUITER);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.updateStatus("recruiter@example.com", 9L,
                new ApplicationStatusRequest(ApplicationStatus.APPLIED, null)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(applications, never()).findById(any());
    }

    @Test
    void recruiterCannotViewApplicantsForAnotherRecruitersJob() {
        User recruiter = user(2L, Role.RECRUITER);
        Job job = job(7L, user(3L, Role.RECRUITER), JobStatus.APPROVED);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(jobs.findById(7L)).thenReturn(Optional.of(job));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.applicants("recruiter@example.com", 7L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(applications, never()).findByJob_IdAndJob_CreatedBy_IdOrderByAppliedAtDesc(anyLong(), anyLong());
    }

    private User user(Long id, Role role) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(user.getFullName()).thenReturn(role == Role.STUDENT ? "Aarav Student" : "Example Recruiter");
        return user;
    }

    private Job job(Long id, User recruiter, JobStatus status) {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(id);
        when(job.getTitle()).thenReturn("Software Engineer");
        when(job.getStatus()).thenReturn(status);
        when(job.getCreatedBy()).thenReturn(recruiter);
        when(job.getDeadline()).thenReturn(null);
        return job;
    }
}
