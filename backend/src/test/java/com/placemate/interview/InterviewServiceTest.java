package com.placemate.interview;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.placemate.application.*;
import com.placemate.job.Job;
import com.placemate.notification.NotificationService;
import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {
    @Mock private InterviewRepository interviews;
    @Mock private ApplicationRepository applications;
    @Mock private UserRepository users;
    @Mock private NotificationService notifications;
    @InjectMocks private InterviewService service;

    @Test
    void recruiterCanScheduleInterviewForApplicantToOwnJobAndStudentIsNotified() {
        User recruiter=user(20L,Role.RECRUITER);
        User student=user(10L,Role.STUDENT);
        Application app=application(student,recruiter,ApplicationStatus.SHORTLISTED);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applications.findById(5L)).thenReturn(Optional.of(app));
        when(interviews.save(any(Interview.class))).thenAnswer(invocation->invocation.getArgument(0));

        InterviewResponse response=service.schedule("recruiter@example.com",5L,request());

        assertEquals(5L,response.applicationId());
        assertEquals(InterviewStatus.SCHEDULED,response.status());
        assertEquals("ONLINE",response.mode());
        ArgumentCaptor<Interview> captor=ArgumentCaptor.forClass(Interview.class);
        verify(interviews).save(captor.capture());
        assertEquals(20L,captor.getValue().getCreatedBy().getId());
        verify(notifications).create(eq(student),eq("INTERVIEW"),eq("Interview scheduled"),
            contains("Software Engineer"),eq("/interviews"));
    }

    @Test
    void recruiterCannotScheduleInterviewForAnotherRecruitersApplicant() {
        User recruiter=user(20L,Role.RECRUITER);
        User owner=user(30L,Role.RECRUITER);
        Application app=application(user(10L,Role.STUDENT),owner,ApplicationStatus.APPLIED);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applications.findById(5L)).thenReturn(Optional.of(app));

        ResponseStatusException error=assertThrows(ResponseStatusException.class,
            ()->service.schedule("recruiter@example.com",5L,request()));

        assertEquals(HttpStatus.FORBIDDEN,error.getStatusCode());
        verify(interviews,never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void rejectedApplicationCannotBeScheduled() {
        User recruiter=user(20L,Role.RECRUITER);
        Application app=application(user(10L,Role.STUDENT),recruiter,ApplicationStatus.REJECTED);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applications.findById(5L)).thenReturn(Optional.of(app));

        ResponseStatusException error=assertThrows(ResponseStatusException.class,
            ()->service.schedule("recruiter@example.com",5L,request()));

        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        verify(interviews,never()).save(any());
    }

    @Test
    void studentCannotScheduleInterview() {
        User student=user(10L,Role.STUDENT);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));

        ResponseStatusException error=assertThrows(ResponseStatusException.class,
            ()->service.schedule("student@example.com",5L,request()));

        assertEquals(HttpStatus.FORBIDDEN,error.getStatusCode());
        verifyNoInteractions(applications,interviews,notifications);
    }

    @Test
    void recruiterCanRescheduleOwnScheduledInterviewAndStudentIsNotified() {
        User recruiter=user(20L,Role.RECRUITER);
        User student=user(10L,Role.STUDENT);
        Application app=application(student,recruiter,ApplicationStatus.APPLIED);
        Interview interview=new Interview(app,recruiter,request());
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(interviews.findById(7L)).thenReturn(Optional.of(interview));
        when(interviews.save(interview)).thenReturn(interview);

        InterviewResponse response=service.reschedule("recruiter@example.com",7L,request());

        assertEquals(InterviewStatus.SCHEDULED,response.status());
        verify(notifications).create(eq(student),eq("INTERVIEW_RESCHEDULED"),eq("Interview rescheduled"),
            contains("Software Engineer"),eq("/interviews"));
        verify(interviews).save(interview);
    }

    @Test
    void recruiterCannotRescheduleAnotherRecruitersInterview() {
        User recruiter=user(20L,Role.RECRUITER);
        User owner=user(30L,Role.RECRUITER);
        Interview interview=new Interview(application(user(10L,Role.STUDENT),owner,ApplicationStatus.APPLIED),owner,request());
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(interviews.findById(7L)).thenReturn(Optional.of(interview));

        ResponseStatusException error=assertThrows(ResponseStatusException.class,
            ()->service.reschedule("recruiter@example.com",7L,request()));

        assertEquals(HttpStatus.FORBIDDEN,error.getStatusCode());
        verify(interviews,never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void completedInterviewCannotBeRescheduled() {
        User recruiter=user(20L,Role.RECRUITER);
        Interview interview=new Interview(application(user(10L,Role.STUDENT),recruiter,ApplicationStatus.APPLIED),recruiter,request());
        interview.setStatus(InterviewStatus.COMPLETED);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(interviews.findById(7L)).thenReturn(Optional.of(interview));

        ResponseStatusException error=assertThrows(ResponseStatusException.class,
            ()->service.reschedule("recruiter@example.com",7L,request()));

        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        verify(interviews,never()).save(any());
    }

    @Test
    void studentCanListOnlyTheirOwnInterviews() {
        User student=user(10L,Role.STUDENT);
        User recruiter=user(20L,Role.RECRUITER);
        Interview interview=new Interview(application(student,recruiter,ApplicationStatus.APPLIED),recruiter,request());
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(interviews.findByApplication_Student_IdOrderByScheduledAtAsc(10L)).thenReturn(List.of(interview));

        List<InterviewResponse> result=service.mine("student@example.com");

        assertEquals(1,result.size());
        assertEquals(10L,result.get(0).studentId());
        verify(interviews).findByApplication_Student_IdOrderByScheduledAtAsc(10L);
    }

    @Test
    void recruiterCanUpdateOwnInterviewStatusAndStudentIsNotified() {
        User recruiter=user(20L,Role.RECRUITER);
        User student=user(10L,Role.STUDENT);
        Interview interview=new Interview(application(student,recruiter,ApplicationStatus.APPLIED),recruiter,request());
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(interviews.findById(7L)).thenReturn(Optional.of(interview));
        when(interviews.save(interview)).thenReturn(interview);

        InterviewResponse response=service.updateStatus("recruiter@example.com",7L,InterviewStatus.CANCELLED);

        assertEquals(InterviewStatus.CANCELLED,response.status());
        verify(notifications).create(eq(student),eq("INTERVIEW_UPDATE"),eq("Interview updated"),
            contains("cancelled"),eq("/interviews"));
    }

    @Test
    void recruiterCannotUpdateAnotherRecruitersInterview() {
        User recruiter=user(20L,Role.RECRUITER);
        User owner=user(30L,Role.RECRUITER);
        Interview interview=new Interview(application(user(10L,Role.STUDENT),owner,ApplicationStatus.APPLIED),owner,request());
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(interviews.findById(7L)).thenReturn(Optional.of(interview));

        ResponseStatusException error=assertThrows(ResponseStatusException.class,
            ()->service.updateStatus("recruiter@example.com",7L,InterviewStatus.COMPLETED));

        assertEquals(HttpStatus.FORBIDDEN,error.getStatusCode());
        verify(interviews,never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void recruiterCanListOnlyInterviewsTheyCreated() {
        User recruiter=user(20L,Role.RECRUITER);
        Interview interview=new Interview(application(user(10L,Role.STUDENT),recruiter,ApplicationStatus.APPLIED),recruiter,request());
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(interviews.findByCreatedBy_IdOrderByScheduledAtAsc(20L)).thenReturn(List.of(interview));

        List<InterviewResponse> result=service.recruiterList("recruiter@example.com");

        assertEquals(1,result.size());
        verify(interviews).findByCreatedBy_IdOrderByScheduledAtAsc(20L);
    }

    private InterviewRequest request() {
        return new InterviewRequest(Instant.now().plusSeconds(3600),45,"online",
            "https://meet.example.com/room",null,"Technical round");
    }

    private Application application(User student,User recruiter,ApplicationStatus status) {
        Job job=mock(Job.class);
        when(job.getCreatedBy()).thenReturn(recruiter);
        when(job.getTitle()).thenReturn("Software Engineer");
        when(job.getCompanyName()).thenReturn("Example Co");
        when(job.getId()).thenReturn(99L);
        when(student.getFullName()).thenReturn("Aarav Student");
        when(student.getEmail()).thenReturn("student@example.com");
        Application app=mock(Application.class);
        when(app.getId()).thenReturn(5L);
        when(app.getStudent()).thenReturn(student);
        when(app.getJob()).thenReturn(job);
        when(app.getStatus()).thenReturn(status);
        return app;
    }

    private User user(Long id,Role role) {
        User user=mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(user.getFullName()).thenReturn(role==Role.STUDENT?"Aarav Student":"Example Recruiter");
        when(user.getEmail()).thenReturn(role==Role.STUDENT?"student@example.com":"recruiter@example.com");
        return user;
    }
}
