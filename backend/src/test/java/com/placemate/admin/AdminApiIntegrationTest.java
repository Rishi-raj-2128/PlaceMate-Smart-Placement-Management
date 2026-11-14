package com.placemate.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placemate.application.ApplicationRepository;
import com.placemate.job.Job;
import com.placemate.job.JobRepository;
import com.placemate.job.JobStatus;
import com.placemate.notification.NotificationRepository;
import com.placemate.user.Role;
import com.placemate.user.User;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AdminApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JobRepository jobs;
    @Autowired ApplicationRepository applications;
    @Autowired NotificationRepository notifications;

    private final ObjectMapper mapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private String suffix, adminEmail, recruiterEmail, studentEmail;
    private String adminToken, recruiterToken, studentToken;
    private Long recruiterId, applicationId;

    @BeforeEach
    void setUp() throws Exception {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        adminEmail = "admin-test-" + suffix + "@example.test";
        recruiterEmail = "admin-recruiter-" + suffix + "@example.test";
        studentEmail = "admin-student-" + suffix + "@example.test";

        User admin = new User("Integration Admin", adminEmail, encoder.encode("StrongPass123"), Role.ADMIN);
        users.saveAndFlush(admin);
        adminToken = login(adminEmail);

        recruiterToken = register("""
            {"fullName":"Admin Test Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Admin Test Company","companyWebsite":"https://admin-test.example"}
            """.formatted(recruiterEmail));
        studentToken = register("""
            {"fullName":"Admin Test Student","email":"%s","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"ADM-%s","department":"CSE"}
            """.formatted(studentEmail, suffix));
        recruiterId = users.findByEmailIgnoreCase(recruiterEmail).orElseThrow().getId();

        MvcResult created = mvc.perform(post("/api/recruiter/jobs")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content(jobRequest()))
            .andExpect(status().isCreated()).andReturn();
        JsonNode jobJson = mapper.readTree(created.getResponse().getContentAsString());
        Job job = jobs.findById(jobJson.get("id").asLong()).orElseThrow();
        job.setStatus(JobStatus.APPROVED);
        jobs.saveAndFlush(job);

        MvcResult applied = mvc.perform(post("/api/student/jobs/" + job.getId() + "/applications")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated()).andReturn();
        applicationId = mapper.readTree(applied.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/recruiter/applications/" + applicationId + "/status")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIRED\"}"))
            .andExpect(status().isOk());
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
        users.findByEmailIgnoreCase(adminEmail).ifPresent(users::delete);
    }

    @Test
    void adminCanViewOverviewUsersCompaniesAndPlacementReport() throws Exception {
        mvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.students").value(1))
            .andExpect(jsonPath("$.recruiters").value(1))
            .andExpect(jsonPath("$.applications").value(1))
            .andExpect(jsonPath("$.hired").value(1));

        mvc.perform(get("/api/admin/users?role=STUDENT").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value(studentEmail));

        mvc.perform(get("/api/admin/users?role=RECRUITER").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value(recruiterEmail));

        mvc.perform(get("/api/admin/companies").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].companyName").value("Admin Test Company"));

        mvc.perform(get("/api/admin/applications").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(applicationId));

        mvc.perform(get("/api/admin/reports/placements").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].department").value("CSE"))
            .andExpect(jsonPath("$[0].hiredStudents").value(1));
    }

    @Test
    void adminCanVerifyAndUnverifyRecruiterCompany() throws Exception {
        mvc.perform(patch("/api/admin/recruiters/" + recruiterId + "/company-verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"verified\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyVerified").value(true));

        mvc.perform(get("/api/admin/companies").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].verified").value(true));

        mvc.perform(patch("/api/admin/recruiters/" + recruiterId + "/company-verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"verified\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyVerified").value(false));
    }

    @Test
    void adminRejectsInvalidRoleAndUnknownRecruiter() throws Exception {
        mvc.perform(get("/api/admin/users?role=ADMIN").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/users?role=INVALID").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/recruiters/999999/company-verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"verified\":true}"))
            .andExpect(status().isNotFound());
        mvc.perform(patch("/api/admin/recruiters/" + recruiterId + "/company-verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousAndNonAdminUsersCannotAccessAdminWorkspace() throws Exception {
        mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/companies").header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isForbidden());
    }

    private String register(String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String login(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+email+"\",\"password\":\"StrongPass123\"}"))
            .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String jobRequest() {
        return """
            {"title":"Admin Reporting Engineer","description":"Validate reporting metrics.","companyName":"Admin Test Company",
             "location":"Mohali","employmentType":"FULL_TIME","minCgpa":6.0,"requiredSkills":["Java"],"deadline":"2099-12-31"}
            """;
    }
}
