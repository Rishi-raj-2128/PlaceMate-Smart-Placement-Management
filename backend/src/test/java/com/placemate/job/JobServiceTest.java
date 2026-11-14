package com.placemate.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {
    @Mock private JobRepository jobs;
    @Mock private UserRepository users;
    @InjectMocks private JobService service;

    private User recruiter;

    @BeforeEach
    void setUp() {
        recruiter = mock(User.class);
        lenient().when(recruiter.getId()).thenReturn(10L);
        lenient().when(recruiter.getRole()).thenReturn(Role.RECRUITER);
    }

    @Test
    void createRejectsNonRecruiterAccounts() {
        User student = mock(User.class);
        when(student.getRole()).thenReturn(Role.STUDENT);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.create("student@example.com", validRequest()));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(jobs, never()).save(any());
    }

    @Test
    void createRejectsMinimumSalaryAboveMaximumSalary() {
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        JobRequest invalid = new JobRequest("Engineer", "Build software", "Acme", "Mohali",
            "FULL_TIME", new BigDecimal("12"), new BigDecimal("8"), new BigDecimal("7.0"),
            java.util.List.of("Java"), null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.create("recruiter@example.com", invalid));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(jobs, never()).save(any());
    }

    @Test
    void updateRejectsJobsOwnedByAnotherRecruiter() {
        User otherRecruiter = mock(User.class);
        when(otherRecruiter.getId()).thenReturn(22L);
        Job job = mock(Job.class);
        when(job.getCreatedBy()).thenReturn(otherRecruiter);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(jobs.findById(5L)).thenReturn(Optional.of(job));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.update("recruiter@example.com", 5L, validRequest()));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(jobs, never()).save(any());
    }

    @Test
    void deleteRejectsJobsOwnedByAnotherRecruiter() {
        User otherRecruiter = mock(User.class);
        when(otherRecruiter.getId()).thenReturn(22L);
        Job job = mock(Job.class);
        when(job.getCreatedBy()).thenReturn(otherRecruiter);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(jobs.findById(5L)).thenReturn(Optional.of(job));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.delete("recruiter@example.com", 5L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(jobs, never()).delete(any());
    }

    @Test
    void adminReviewCannotReturnAJobToPending() {
        JobReviewRequest invalid = new JobReviewRequest(JobStatus.PENDING, "Not a decision");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.review(5L, invalid));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(jobs, never()).findById(any());
    }

    private JobRequest validRequest() {
        return new JobRequest("Software Engineer", "Build and maintain software",
            "Acme Technologies", "Mohali", "FULL_TIME", new BigDecimal("5"),
            new BigDecimal("10"), new BigDecimal("7.0"), java.util.List.of("Java"), null);
    }
}
