package com.placemate.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.placemate.application.Application;
import com.placemate.application.ApplicationRepository;
import com.placemate.job.Job;
import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class ResumeControllerTest {
    @TempDir Path uploadDir;

    private UserRepository users;
    private StudentProfileRepository profiles;
    private ApplicationRepository applications;
    private ResumeController controller;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        profiles = mock(StudentProfileRepository.class);
        applications = mock(ApplicationRepository.class);
        controller = new ResumeController(users, profiles, applications, uploadDir.toString());
    }

    @Test
    void rejectsUnsupportedFileExtension() {
        User student = user(11L, Role.STUDENT);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controller.upload(auth("student@example.com"),
                new MockMultipartFile("file", "resume.exe", "application/octet-stream", new byte[]{1, 2, 3})));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(profiles);
    }

    @Test
    void rejectsPdfWithInvalidSignature() {
        User student = user(11L, Role.STUDENT);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controller.upload(auth("student@example.com"),
                new MockMultipartFile("file", "resume.pdf", "application/pdf", "not a pdf".getBytes())));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(profiles);
    }

    @Test
    void acceptsValidPdfAndStoresGeneratedStudentScopedName() throws Exception {
        User student = user(11L, Role.STUDENT);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(profiles.findByUser_Id(11L)).thenReturn(Optional.empty());
        when(profiles.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        byte[] pdf = "%PDF-1.7 test document".getBytes();

        var response = controller.upload(auth("student@example.com"),
            new MockMultipartFile("file", "../../my-resume.pdf", "application/pdf", pdf));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var profileCaptor = org.mockito.ArgumentCaptor.forClass(StudentProfile.class);
        verify(profiles).save(profileCaptor.capture());
        String storedName = profileCaptor.getValue().getResumeUrl();
        assertTrue(storedName.matches("11-[0-9a-fA-F-]{36}\\\\.pdf"));
        assertArrayEquals(pdf, Files.readAllBytes(uploadDir.resolve(storedName)));
    }

    @Test
    void studentCanDownloadOnlyTheirOwnStoredResume() throws Exception {
        User student = user(11L, Role.STUDENT);
        StudentProfile profile = mock(StudentProfile.class);
        String storedName = "11-12345678-1234-1234-1234-123456789abc.pdf";
        Files.write(uploadDir.resolve(storedName), "%PDF-1.7".getBytes());
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(profiles.findByUser_Id(11L)).thenReturn(Optional.of(profile));
        when(profile.getResumeUrl()).thenReturn(storedName);

        var response = controller.studentResume(auth("student@example.com"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains(storedName));
    }

    @Test
    void recruiterCannotDownloadResumeForAnotherRecruitersJob() throws Exception {
        User recruiter = user(22L, Role.RECRUITER);
        User owner = user(33L, Role.RECRUITER);
        User student = user(11L, Role.STUDENT);
        Job job = mock(Job.class);
        Application application = mock(Application.class);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applications.findById(90L)).thenReturn(Optional.of(application));
        when(application.getJob()).thenReturn(job);
        when(job.getCreatedBy()).thenReturn(owner);
        when(owner.getId()).thenReturn(33L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controller.recruiterResume(auth("recruiter@example.com"), 90L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(profiles);
    }

    @Test
    void recruiterCanDownloadResumeForApplicantToTheirOwnJob() throws Exception {
        User recruiter = user(22L, Role.RECRUITER);
        User student = user(11L, Role.STUDENT);
        Job job = mock(Job.class);
        Application application = mock(Application.class);
        StudentProfile profile = mock(StudentProfile.class);
        String storedName = "11-12345678-1234-1234-1234-123456789abc.pdf";
        Files.write(uploadDir.resolve(storedName), "%PDF-1.7".getBytes());
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applications.findById(90L)).thenReturn(Optional.of(application));
        when(application.getJob()).thenReturn(job);
        when(job.getCreatedBy()).thenReturn(recruiter);
        when(recruiter.getId()).thenReturn(22L);
        when(application.getStudent()).thenReturn(student);
        when(student.getId()).thenReturn(11L);
        when(profiles.findByUser_Id(11L)).thenReturn(Optional.of(profile));
        when(profile.getResumeUrl()).thenReturn(storedName);

        var response = controller.recruiterResume(auth("recruiter@example.com"), 90L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void nonStudentCannotUseStudentResumeDownloadEndpoint() {
        User recruiter = user(22L, Role.RECRUITER);
        when(users.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.of(recruiter));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controller.studentResume(auth("recruiter@example.com")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(profiles);
    }

    private Authentication auth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    private User user(Long id, Role role) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        return user;
    }
}
