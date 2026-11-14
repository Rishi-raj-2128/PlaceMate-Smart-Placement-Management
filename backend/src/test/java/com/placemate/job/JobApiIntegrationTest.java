package com.placemate.job;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class JobApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JobRepository jobs;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String suffix;
    private String recruiterEmail;
    private String studentEmail;
    private String recruiterToken;
    private String studentToken;
    private Long createdJobId;

    @BeforeEach
    void setUp() throws Exception {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        recruiterEmail = "jobs-recruiter-" + suffix + "@example.test";
        studentEmail = "jobs-student-" + suffix + "@example.test";
        recruiterToken = register("""
            {"fullName":"Jobs Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Example Technologies"}
            """.formatted(recruiterEmail));
        studentToken = register("""
            {"fullName":"Jobs Student","email":"%s","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"ENR-%s","department":"CSE"}
            """.formatted(studentEmail, suffix));
    }

    @AfterEach
    void cleanUp() {
        users.findByEmailIgnoreCase(recruiterEmail).ifPresent(user -> {
            jobs.findByCreatedByIdOrderByCreatedAtDesc(user.getId()).forEach(jobs::delete);
            users.delete(user);
        });
        users.findByEmailIgnoreCase(studentEmail).ifPresent(users::delete);
    }

    @Test
    void recruiterCanCreateAndListOwnPendingJobButPublicCannotViewIt() throws Exception {
        MvcResult result = mvc.perform(post("/api/recruiter/jobs")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jobRequest()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Backend Developer"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        createdJobId = response.get("id").asLong();

        mvc.perform(get("/api/recruiter/jobs").header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + createdJobId + ")]").exists());

        mvc.perform(get("/api/jobs/" + createdJobId))
            .andExpect(status().isNotFound());
    }

    @Test
    void anonymousUsersCannotCreateRecruiterJobs() throws Exception {
        mvc.perform(post("/api/recruiter/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jobRequest()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotCreateOrListRecruiterJobs() throws Exception {
        mvc.perform(post("/api/recruiter/jobs")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jobRequest()))
            .andExpect(status().isForbidden());

        mvc.perform(get("/api/recruiter/jobs").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCannotAccessAdminJobReviewQueue() throws Exception {
        mvc.perform(get("/api/admin/jobs/pending").header("Authorization", "Bearer " + recruiterToken))
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
