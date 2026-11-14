package com.placemate.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placemate.job.Job;
import com.placemate.job.JobRepository;
import com.placemate.job.JobStatus;
import com.placemate.notification.NotificationRepository;
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
class StudentProfileApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JobRepository jobs;
    @Autowired StudentProfileRepository profiles;
    @Autowired NotificationRepository notifications;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String suffix;
    private String studentEmail;
    private String recruiterEmail;
    private String studentToken;
    private String recruiterToken;

    @BeforeEach
    void setUp() throws Exception {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        studentEmail = "profile-student-" + suffix + "@example.test";
        recruiterEmail = "profile-recruiter-" + suffix + "@example.test";
        studentToken = register("""
            {"fullName":"Profile Student","email":"%s","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"PRO-%s","department":"CSE"}
            """.formatted(studentEmail, suffix));
        recruiterToken = register("""
            {"fullName":"Profile Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Profile Test Co"}
            """.formatted(recruiterEmail));
        createApprovedJob();
    }

    @AfterEach
    void cleanUp() {
        users.findByEmailIgnoreCase(studentEmail).ifPresent(student -> {
            notifications.deleteAll(notifications.findTop100ByUser_IdOrderByCreatedAtDesc(student.getId()));
            profiles.findByUser_Id(student.getId()).ifPresent(profiles::delete);
            users.delete(student);
        });
        users.findByEmailIgnoreCase(recruiterEmail).ifPresent(recruiter -> {
            notifications.deleteAll(notifications.findTop100ByUser_IdOrderByCreatedAtDesc(recruiter.getId()));
            jobs.findByCreatedByIdOrderByCreatedAtDesc(recruiter.getId()).forEach(jobs::delete);
            users.delete(recruiter);
        });
    }

    @Test
    void studentCanReadAndUpdateOwnProfile() throws Exception {
        mvc.perform(get("/api/student/profile").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(studentEmail))
            .andExpect(jsonPath("$.skills").isArray());

        mvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileRequest()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cgpa").value(8.5))
            .andExpect(jsonPath("$.graduationYear").value(2027))
            .andExpect(jsonPath("$.degree").value("B.Tech"))
            .andExpect(jsonPath("$.branch").value("Computer Science"))
            .andExpect(jsonPath("$.skills.length()").value(2));

        mvc.perform(get("/api/student/profile").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cgpa").value(8.5))
            .andExpect(jsonPath("$.skills").isArray());
    }

    @Test
    void jobMatchesReflectCgpaAndCaseInsensitiveSkills() throws Exception {
        mvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileRequest()))
            .andExpect(status().isOk());

        mvc.perform(get("/api/student/jobs/matches").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].job.title").value("Profile Match Engineer"))
            .andExpect(jsonPath("$[0].eligible").value(true))
            .andExpect(jsonPath("$[0].matchPercentage").value(100));
    }

    @Test
    void incompleteProfileIsNotMarkedEligibleForRequirementsItCannotVerify() throws Exception {
        mvc.perform(get("/api/student/jobs/matches").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eligible").value(false))
            .andExpect(jsonPath("$[0].matchPercentage").value(0));
    }

    @Test
    void invalidProfileAndForeignUploadedResumeReferenceAreRejected() throws Exception {
        mvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cgpa\":11.0,\"skills\":[]}"))
            .andExpect(status().isBadRequest());

        mvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"cgpa":8.0,"graduationYear":2027,"degree":"B.Tech","branch":"CSE","skills":["Java"],"resumeUrl":"999-00000000-0000-0000-0000-000000000000.pdf"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousAndRecruiterCannotAccessStudentProfileOrMatches() throws Exception {
        mvc.perform(get("/api/student/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/student/jobs/matches")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/student/profile").header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/student/profile")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileRequest()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/student/jobs/matches").header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isForbidden());
    }

    private void createApprovedJob() throws Exception {
        MvcResult created = mvc.perform(post("/api/recruiter/jobs")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title":"Profile Match Engineer",
                      "description":"Build Java services and REST APIs.",
                      "companyName":"Profile Test Co",
                      "location":"Mohali",
                      "employmentType":"FULL_TIME",
                      "minCgpa":7.0,
                      "requiredSkills":["Java","Spring Boot"],
                      "deadline":"2099-12-31"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode response = objectMapper.readTree(created.getResponse().getContentAsString());
        Job job = jobs.findById(response.get("id").asLong()).orElseThrow();
        job.setStatus(JobStatus.APPROVED);
        jobs.saveAndFlush(job);
    }

    private String register(String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String profileRequest() {
        return """
            {
              "cgpa":8.5,
              "graduationYear":2027,
              "degree":"B.Tech",
              "branch":"Computer Science",
              "skills":["Java","spring boot"]
            }
            """;
    }
}
