package com.placemate.interview;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placemate.application.ApplicationRepository;
import com.placemate.interview.InterviewRepository;
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
class InterviewApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JobRepository jobs;
    @Autowired ApplicationRepository applications;
    @Autowired InterviewRepository interviews;
    @Autowired NotificationRepository notifications;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String suffix;
    private String recruiterEmail;
    private String studentEmail;
    private String otherRecruiterEmail;
    private String recruiterToken;
    private String studentToken;
    private String otherRecruiterToken;
    private Long applicationId;

    @BeforeEach
    void setUp() throws Exception {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        recruiterEmail = "interview-recruiter-" + suffix + "@example.test";
        studentEmail = "interview-student-" + suffix + "@example.test";
        otherRecruiterEmail = "interview-other-" + suffix + "@example.test";

        recruiterToken = register("""
            {"fullName":"Interview Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Interview Test Co"}
            """.formatted(recruiterEmail));
        studentToken = register("""
            {"fullName":"Interview Student","email":"%s","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"INT-%s","department":"CSE"}
            """.formatted(studentEmail, suffix));
        otherRecruiterToken = register("""
            {"fullName":"Other Interview Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Other Interview Co"}
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
        users.findByEmailIgnoreCase(recruiterEmail).ifPresent(recruiter ->
            interviews.deleteAll(interviews.findByCreatedBy_IdOrderByScheduledAtAsc(recruiter.getId())));
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
    void recruiterCanScheduleAndBothRolesCanViewTheirOwnInterview() throws Exception {
        long interviewId = scheduleInterview();

        mvc.perform(get("/api/student/interviews").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(interviewId))
            .andExpect(jsonPath("$[0].jobTitle").value("Interview API Engineer"))
            .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        mvc.perform(get("/api/recruiter/interviews").header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(interviewId))
            .andExpect(jsonPath("$[0].studentEmail").value(studentEmail));
    }

    @Test
    void recruiterCanRescheduleInterviewAndUpdateItsStatus() throws Exception {
        long interviewId = scheduleInterview();

        mvc.perform(put("/api/recruiter/interviews/" + interviewId)
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(interviewRequest(7200, "IN_PERSON")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("IN_PERSON"))
            .andExpect(jsonPath("$.location").value("CGC Campus"));

        mvc.perform(patch("/api/recruiter/interviews/" + interviewId + "/status?status=COMPLETED")
                .header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(put("/api/recruiter/interviews/" + interviewId)
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(interviewRequest(10800, "ONLINE")))
            .andExpect(status().isConflict());
    }

    @Test
    void anotherRecruiterCannotRescheduleOrUpdateInterview() throws Exception {
        long interviewId = scheduleInterview();

        mvc.perform(put("/api/recruiter/interviews/" + interviewId)
                .header("Authorization", "Bearer " + otherRecruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(interviewRequest(7200, "ONLINE")))
            .andExpect(status().isForbidden());

        mvc.perform(patch("/api/recruiter/interviews/" + interviewId + "/status?status=CANCELLED")
                .header("Authorization", "Bearer " + otherRecruiterToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousAndWrongRoleUsersCannotAccessInterviewEndpoints() throws Exception {
        mvc.perform(get("/api/student/interviews")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/recruiter/interviews")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/recruiter/applications/" + applicationId + "/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(interviewRequest(3600, "ONLINE")))
            .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/student/interviews").header("Authorization", "Bearer " + recruiterToken))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/recruiter/interviews").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/recruiter/applications/" + applicationId + "/interviews")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(interviewRequest(3600, "ONLINE")))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidInterviewRequestIsRejected() throws Exception {
        mvc.perform(post("/api/recruiter/applications/" + applicationId + "/interviews")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"scheduledAt":"2000-01-01T10:00:00Z","durationMinutes":5,"mode":"UNKNOWN","meetingLink":"javascript:alert(1)"}
                    """))
            .andExpect(status().isBadRequest());
    }

    private long scheduleInterview() throws Exception {
        MvcResult result = mvc.perform(post("/api/recruiter/applications/" + applicationId + "/interviews")
                .header("Authorization", "Bearer " + recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(interviewRequest(3600, "ONLINE")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.durationMinutes").value(45))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String interviewRequest(long secondsFromNow, String mode) {
        String time = Instant.now().plusSeconds(secondsFromNow).toString();
        String location = "IN_PERSON".equals(mode) ? "\"location\":\"CGC Campus\"" : "\"meetingLink\":\"https://meet.example.test/placemate\"";
        return """
            {
              "scheduledAt":"%s",
              "durationMinutes":45,
              "mode":"%s",
              %s,
              "notes":"Technical interview round"
            }
            """.formatted(time, mode, location);
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
              "title":"Interview API Engineer",
              "description":"A role used to validate interview scheduling and access.",
              "companyName":"Interview Test Co",
              "location":"Mohali",
              "employmentType":"FULL_TIME",
              "minCgpa":6.0,
              "requiredSkills":["Java"],
              "deadline":"2099-12-31"
            }
            """;
    }
}
