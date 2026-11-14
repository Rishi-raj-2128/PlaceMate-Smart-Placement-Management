package com.placemate.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placemate.application.ApplicationRepository;
import com.placemate.job.Job;
import com.placemate.job.JobRepository;
import com.placemate.job.JobStatus;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JobRepository jobs;
    @Autowired ApplicationRepository applications;
    @Autowired NotificationRepository notifications;

    private final ObjectMapper mapper = new ObjectMapper();
    private String suffix, recruiterEmail, studentEmail, otherStudentEmail;
    private String recruiterToken, studentToken, otherStudentToken;
    private Long applicationId;

    @BeforeEach
    void setUp() throws Exception {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        recruiterEmail = "notify-recruiter-" + suffix + "@example.test";
        studentEmail = "notify-student-" + suffix + "@example.test";
        otherStudentEmail = "notify-other-" + suffix + "@example.test";
        recruiterToken = register("""
            {"fullName":"Notify Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Notify Co"}
            """.formatted(recruiterEmail));
        studentToken = register("""
            {"fullName":"Notify Student","email":"%s","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"NOT-%s","department":"CSE"}
            """.formatted(studentEmail, suffix));
        otherStudentToken = register("""
            {"fullName":"Other Notify Student","email":"%s","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"OTH-%s","department":"ECE"}
            """.formatted(otherStudentEmail, suffix));

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
        users.findByEmailIgnoreCase(otherStudentEmail).ifPresent(other -> {
            notifications.deleteAll(notifications.findTop100ByUser_IdOrderByCreatedAtDesc(other.getId()));
            users.delete(other);
        });
    }

    @Test
    void recipientCanListCountMarkOneReadAndMarkAllRead() throws Exception {
        MvcResult listed = mvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("APPLICATION"))
            .andExpect(jsonPath("$[0].title").value("New application received"))
            .andReturn();
        long notificationId = mapper.readTree(listed.getResponse().getContentAsString()).get(0).get("id").asLong();

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk()).andExpect(content().string("1"));

        mvc.perform(patch("/api/notifications/" + notificationId + "/read")
                .header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.readAt").isNotEmpty());

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk()).andExpect(content().string("0"));

        mvc.perform(patch("/api/recruiter/applications/" + applicationId + "/status")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SHORTLISTED\"}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk()).andExpect(content().string("1"));

        mvc.perform(post("/api/notifications/read-all").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk()).andExpect(content().string("0"));
    }

    @Test
    void notificationCannotBeReadByAnotherUser() throws Exception {
        MvcResult listed = mvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk()).andReturn();
        long notificationId = mapper.readTree(listed.getResponse().getContentAsString()).get(0).get("id").asLong();

        mvc.perform(patch("/api/notifications/" + notificationId + "/read")
                .header("Authorization", "Bearer " + otherStudentToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void anonymousNotificationRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/notifications/read-all")).andExpect(status().isUnauthorized());
    }

    private String register(String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String jobRequest() {
        return """
            {"title":"Notification Test Engineer","description":"Test notification workflow.","companyName":"Notify Co",
             "location":"Mohali","employmentType":"FULL_TIME","minCgpa":6.0,"requiredSkills":["Java"],"deadline":"2099-12-31"}
            """;
    }
}
