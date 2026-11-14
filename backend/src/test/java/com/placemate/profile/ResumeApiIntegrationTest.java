package com.placemate.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placemate.application.ApplicationRepository;
import com.placemate.job.Job;
import com.placemate.job.JobRepository;
import com.placemate.job.JobStatus;
import com.placemate.notification.NotificationRepository;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = ResumeApiIntegrationTest.UploadDirectoryInitializer.class)
class ResumeApiIntegrationTest {
    @TempDir static Path uploadDir;

    static class UploadDirectoryInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of("placemate.upload-dir=" + uploadDir.toAbsolutePath())
                .applyTo(context.getEnvironment());
        }
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JobRepository jobs;
    @Autowired ApplicationRepository applications;
    @Autowired StudentProfileRepository profiles;
    @Autowired NotificationRepository notifications;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String suffix;
    private String studentEmail;
    private String recruiterEmail;
    private String otherRecruiterEmail;
    private String studentToken;
    private String recruiterToken;
    private String otherRecruiterToken;
    private Long applicationId;

    @BeforeEach
    void setUp() throws Exception {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        studentEmail = "resume-student-" + suffix + "@example.test";
        recruiterEmail = "resume-recruiter-" + suffix + "@example.test";
        otherRecruiterEmail = "resume-other-" + suffix + "@example.test";
        studentToken = register("""
            {"fullName":"Resume Student","email":"%s","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"RES-%s","department":"CSE"}
            """.formatted(studentEmail, suffix));
        recruiterToken = register("""
            {"fullName":"Resume Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Resume Test Co"}
            """.formatted(recruiterEmail));
        otherRecruiterToken = register("""
            {"fullName":"Other Resume Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Other Resume Co"}
            """.formatted(otherRecruiterEmail));

        MvcResult created = mvc.perform(post("/api/recruiter/jobs")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jobRequest()))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode jobJson = objectMapper.readTree(created.getResponse().getContentAsString());
        Job job = jobs.findById(jobJson.get("id").asLong()).orElseThrow();
        job.setStatus(JobStatus.APPROVED);
        jobs.saveAndFlush(job);

        MvcResult applied = mvc.perform(post("/api/student/jobs/" + job.getId() + "/applications")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated())
            .andReturn();
        applicationId = objectMapper.readTree(applied.getResponse().getContentAsString()).get("id").asLong();
    }

    @AfterEach
    void cleanUp() {
        users.findByEmailIgnoreCase(studentEmail).ifPresent(student -> {
            notifications.deleteAll(notifications.findTop100ByUser_IdOrderByCreatedAtDesc(student.getId()));
            applications.deleteAll(applications.findByStudentIdOrderByAppliedAtDesc(student.getId()));
            profiles.findByUser_Id(student.getId()).ifPresent(profiles::delete);
            users.delete(student);
        });
        users.findByEmailIgnoreCase(recruiterEmail).ifPresent(recruiter -> {
            notifications.deleteAll(notifications.findTop100ByUser_IdOrderByCreatedAtDesc(recruiter.getId()));
            jobs.findByCreatedByIdOrderByCreatedAtDesc(recruiter.getId()).forEach(jobs::delete);
            users.delete(recruiter);
        });
        users.findByEmailIgnoreCase(otherRecruiterEmail).ifPresent(users::delete);
    }

    @Test
    void studentCanUploadAndDownloadOwnResume() throws Exception {
        byte[] pdf = "%PDF-1.7 PlaceMate integration test resume".getBytes();
        mvc.perform(multipart("/api/student/profile/resume")
                .file(new MockMultipartFile("file", "my-resume.pdf", "application/pdf", pdf))
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stored").value(true))
            .andExpect(jsonPath("$.downloadEndpoint").value("/api/student/resume"));

        mvc.perform(get("/api/student/resume").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(content().bytes(pdf));

        mvc.perform(get("/api/student/profile").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumeUrl").isNotEmpty());
    }

    @Test
    void recruiterCanDownloadResumeForApplicantToTheirOwnJob() throws Exception {
        uploadResume();

        mvc.perform(get("/api/recruiter/applications/" + applicationId + "/resume")
                .header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void anotherRecruiterCannotDownloadApplicantsResume() throws Exception {
        uploadResume();

        mvc.perform(get("/api/recruiter/applications/" + applicationId + "/resume")
                .header("Authorization", "Bearer " + otherRecruiterToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidResumeAndOversizedUploadAreRejected() throws Exception {
        mvc.perform(multipart("/api/student/profile/resume")
                .file(new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a pdf".getBytes()))
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isBadRequest());

        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        mvc.perform(multipart("/api/student/profile/resume")
                .file(new MockMultipartFile("file", "large.pdf", "application/pdf", oversized))
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void anonymousAndWrongRoleResumeRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/student/resume")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/student/profile/resume")
                .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-1.7".getBytes())))
            .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/student/resume").header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/student/profile/resume")
                .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-1.7".getBytes()))
                .header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isForbidden());
    }

    private void uploadResume() throws Exception {
        mvc.perform(multipart("/api/student/profile/resume")
                .file(new MockMultipartFile("file", "student-cv.pdf", "application/pdf", "%PDF-1.7 student cv".getBytes()))
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk());
    }

    private String register(String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String jobRequest() {
        return """
            {
              "title":"Resume API Integration Role",
              "description":"A role used to validate secure resume access.",
              "companyName":"Resume Test Co",
              "location":"Mohali",
              "employmentType":"FULL_TIME",
              "minCgpa":6.0,
              "requiredSkills":["Java"],
              "deadline":"2099-12-31"
            }
            """;
    }
}
