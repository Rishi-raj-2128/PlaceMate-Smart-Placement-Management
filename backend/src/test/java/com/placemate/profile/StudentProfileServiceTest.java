package com.placemate.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.placemate.job.Job;
import com.placemate.job.JobRepository;
import com.placemate.job.JobStatus;
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
class StudentProfileServiceTest {
    @Mock private StudentProfileRepository profiles;
    @Mock private UserRepository users;
    @Mock private JobRepository jobs;
    @InjectMocks private StudentProfileService service;

    @Test
    void studentWithMatchingCgpaAndSkillsIsEligible() {
        User student = student();
        StudentProfile profile = mock(StudentProfile.class);
        Job job = job(new BigDecimal("7.00"), Set.of("Java", "SQL"), null);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(profiles.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        when(profile.getCgpa()).thenReturn(new BigDecimal("8.20"));
        when(profile.getSkills()).thenReturn(Set.of("java", "SQL"));
        when(jobs.findByStatusOrderByCreatedAtDesc(JobStatus.APPROVED)).thenReturn(List.of(job));

        List<JobMatchResponse> matches = service.matches("student@example.com");

        assertEquals(1, matches.size());
        assertTrue(matches.get(0).eligible());
        assertEquals(100, matches.get(0).matchPercentage());
        assertTrue(matches.get(0).reasons().contains("Your CGPA meets the minimum requirement."));
        assertTrue(matches.get(0).reasons().contains("You have all required skills."));
    }

    @Test
    void missingProfileDataIsNotTreatedAsEligible() {
        User student = student();
        Job job = job(new BigDecimal("7.00"), Set.of("Java"), null);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(profiles.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(jobs.findByStatusOrderByCreatedAtDesc(JobStatus.APPROVED)).thenReturn(List.of(job));

        List<JobMatchResponse> matches = service.matches("student@example.com");

        assertFalse(matches.get(0).eligible());
        assertEquals(0, matches.get(0).matchPercentage());
        assertTrue(matches.get(0).reasons().stream().anyMatch(s -> s.contains("Add your CGPA")));
        assertTrue(matches.get(0).reasons().stream().anyMatch(s -> s.contains("Add your skills")));
    }

    @Test
    void partialSkillMatchProducesPartialScoreAndIneligibility() {
        User student = student();
        StudentProfile profile = mock(StudentProfile.class);
        Job job = job(null, Set.of("Java", "SQL"), null);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(profiles.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        when(profile.getCgpa()).thenReturn(new BigDecimal("8.00"));
        when(profile.getSkills()).thenReturn(Set.of("Java"));
        when(jobs.findByStatusOrderByCreatedAtDesc(JobStatus.APPROVED)).thenReturn(List.of(job));

        JobMatchResponse match = service.matches("student@example.com").get(0);

        assertFalse(match.eligible());
        assertEquals(75, match.matchPercentage());
        assertTrue(match.reasons().contains("Matched 1 of 2 required skills."));
    }

    @Test
    void expiredApprovedJobsAreExcludedFromMatches() {
        User student = student();
        Job expired = job(null, Set.of(), LocalDate.now().minusDays(1));
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(profiles.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(jobs.findByStatusOrderByCreatedAtDesc(JobStatus.APPROVED)).thenReturn(List.of(expired));

        assertTrue(service.matches("student@example.com").isEmpty());
    }

    @Test
    void recruiterCannotReadStudentProfileOrMatches() {
        User recruiter = mock(User.class);
        when(recruiter.getRole()).thenReturn(Role.RECRUITER);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.matches("recruiter@example.com"));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(profiles, jobs);
    }

    @Test
    void studentCannotAssignAnotherStudentsUploadedResumeKey() {
        User student = student();
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.updateProfile("student@example.com",
                new StudentProfileRequest(new BigDecimal("8.0"), 2027, "BTech",
                    "CSE", Set.of("Java"), "99-12345678-1234-1234-1234-123456789abc.pdf")));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(profiles, never()).save(any());
    }

    private User student() {
        User student = mock(User.class);
        when(student.getId()).thenReturn(1L);
        when(student.getRole()).thenReturn(Role.STUDENT);
        return student;
    }

    private Job job(BigDecimal minCgpa, Set<String> skills, LocalDate deadline) {
        User recruiter = mock(User.class);
        when(recruiter.getFullName()).thenReturn("Example Recruiter");
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(10L);
        when(job.getTitle()).thenReturn("Backend Engineer");
        when(job.getDescription()).thenReturn("Build APIs");
        when(job.getCompanyName()).thenReturn("Example Co");
        when(job.getLocation()).thenReturn("Mohali");
        when(job.getEmploymentType()).thenReturn("FULL_TIME");
        when(job.getMinCgpa()).thenReturn(minCgpa);
        when(job.getRequiredSkills()).thenReturn(skills);
        doReturn(deadline).when(job).getDeadline();
        when(job.getStatus()).thenReturn(JobStatus.APPROVED);
        when(job.getCreatedBy()).thenReturn(recruiter);
        return job;
    }
}
