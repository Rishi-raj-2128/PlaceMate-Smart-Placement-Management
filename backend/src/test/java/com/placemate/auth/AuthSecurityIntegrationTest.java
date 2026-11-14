package com.placemate.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired UserRepository users;

    private String suffix;
    private String studentEmail;
    private String recruiterEmail;

    @BeforeEach
    void setUp() {
        suffix=UUID.randomUUID().toString().substring(0,8);
        studentEmail="student-"+suffix+"@example.test";
        recruiterEmail="recruiter-"+suffix+"@example.test";
    }

    @AfterEach
    void cleanUp() {
        users.findByEmailIgnoreCase(studentEmail).ifPresent(users::delete);
        users.findByEmailIgnoreCase(recruiterEmail).ifPresent(users::delete);
    }

    @Test
    void authenticationMeEndpointRejectsAnonymousRequest() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicRegistrationCreatesStudentAndReturnsBearerToken() throws Exception {
        String token=registerStudent();

        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(studentEmail))
            .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void publicAdminRegistrationIsForbidden() throws Exception {
        String body="""
            {"fullName":"Platform Admin","email":"admin-%s@example.test","password":"StrongPass123","role":"ADMIN"}
            """.formatted(suffix);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentTokenCannotAccessRecruiterOrAdminApis() throws Exception {
        String token=registerStudent();

        mvc.perform(get("/api/recruiter/jobs").header("Authorization","Bearer "+token))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/overview").header("Authorization","Bearer "+token))
            .andExpect(status().isForbidden());
    }

    @Test
    void recruiterTokenCannotAccessStudentOrAdminApis() throws Exception {
        String token=registerRecruiter();

        mvc.perform(get("/api/student/profile").header("Authorization","Bearer "+token))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/overview").header("Authorization","Bearer "+token))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidBearerTokenCannotAccessAuthenticatedEndpoint() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer invalid.token.value"))
            .andExpect(status().isUnauthorized());
    }

    private String registerStudent() throws Exception {
        String body="""
            {"fullName":"Integration Student","email":"%s","password":"StrongPass123","role":"STUDENT","enrollmentNumber":"ENR-%s","department":"CSE"}
            """.formatted(studentEmail,suffix);
        return register(body);
    }

    private String registerRecruiter() throws Exception {
        String body="""
            {"fullName":"Integration Recruiter","email":"%s","password":"StrongPass123","role":"RECRUITER","companyName":"Example Technologies"}
            """.formatted(recruiterEmail);
        return register(body);
    }

    private String register(String body) throws Exception {
        MvcResult result=mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn();
        JsonNode json=objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
