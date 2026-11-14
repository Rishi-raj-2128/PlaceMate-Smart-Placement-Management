package com.placemate.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placemate.job.Job;
import com.placemate.job.JobRepository;
import com.placemate.job.JobStatus;
import com.placemate.notification.NotificationRepository;
import com.placemate.user.User;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JobRepository jobs;
    @Autowired ApplicationRepository applications;
    @Autowired NotificationRepository notifications;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String suffix;
    private String recruiterEmail;
    private String studentEmail;
    private String otherRecruiterEmail;
    private String recruiterToken;
    private String studentToken;
    private String otherRecruiterToken;
    private Long jobId;

    @BeforeEach
    void setUp() throws Exception {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        recruiterEmail = "app-recruiter-" + suffix + "@example.test";
        studentEmail = "app-student-" + suffix + "@example.test";
        otherRecruiterEmail = "other-recruiter-" + suffix + "@example.test";
        recruiterToken = register("""
            {"fullName":"Application Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Example Technologies"}
            """.formatted(recruiterEmail));
        studentToken = register("""
            {"fullName":"Application Student","email":"%s","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"ENR-%s","department":"CSE"}
            """.formatted(studentEmail, suffix));
        otherRecruiterToken = register("""
            {"fullName":"Other Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Other Technologies"}
            """.formatted(otherRecruiterEmail));

        MvcResult created = mvc.perform(post("/api/recruiter/jobs")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jobRequest()))
            .andExpect(status().isCreated())
            .andReturn();
        jobId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        Job job = jobs.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.APPROVED);
        jobs.saveAndFlush(job);
    }

    @AfterEach
    void cleanUp() {
        users.findByEmailIgnoreCase(studentEmail).ifPresent(student -> {
            notifications.deleteAll(notifications.findTop100ByUser_IdOrderByCreatedAtDesc(student.getId()));
            applications.deleteAll(applications.findByStudentIdOrderByAppliedAtDesc(student.getId()));
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
    void studentCanApplyAndRecruiterCanReviewApplication() throws Exception {
        MvcResult applied = mvc.perform(post("/api/student/jobs/" + jobId + "/applications")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("APPLIED"))
            .andExpect(jsonPath("$.jobTitle").value("Backend Developer"))
            .andReturn();

        long applicationId = objectMapper.readTree(applied.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/student/applications").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(applicationId));

        mvc.perform(get("/api/recruiter/jobs/" + jobId + "/applications")
                .header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentEmail").value(studentEmail));

        mvc.perform(patch("/api/recruiter/applications/" + applicationId + "/status")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SHORTLISTED\",\"recruiterNote\":\"Proceed to technical round\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SHORTLISTED"))
            .andExpect(jsonPath("$.recruiterNote").value("Proceed to technical round"));
    }

    @Test
    void duplicateApplicationIsRejected() throws Exception {
        mvc.perform(post("/api/student/jobs/" + jobId + "/applications")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/student/jobs/" + jobId + "/applications")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isConflict());
    }

    @Test
    void anotherRecruiterCannotReadOrUpdateApplicants() throws Exception {
        MvcResult applied = mvc.perform(post("/api/student/jobs/" + jobId + "/applications")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated())
            .andReturn();
        long applicationId = objectMapper.readTree(applied.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/recruiter/jobs/" + jobId + "/applications")
                .header("Authorization", "Bearer " + otherRecruiterToken))
            .andExpect(status().isForbidden());

        mvc.perform(patch("/api/recruiter/applications/" + applicationId + "/status")
                .header("Authorization", "Bearer " + otherRecruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousAndWrongRoleRequestsAreRejected() throws Exception {
        mvc.perform(post("/api/student/jobs/" + jobId + "/applications"))
            .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/student/applications")
                .header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isForbidden());
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
              "title":"Backend Developer",
              "description":"Build and maintain REST APIs for placement workflows.",
              "companyName":"Example Technologies",
              "location":"Mohali",
              "employmentType":"FULL_TIME",
              "salaryMin":500000,
              "salaryMax":800000,
              "minCgpa":7.0,
              "requiredSkills":["Java","Spring Boot"],
              "deadline":"2099-12-31"
            }
            """;
    }
}
