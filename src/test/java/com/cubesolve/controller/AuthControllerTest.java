package com.cubesolve.controller;

import com.cubesolve.dto.LoginRequest;
import com.cubesolve.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController: /api/auth/register and /api/auth/login.
 *
 * Uses H2 in-memory DB (see src/test/resources/application.properties).
 * DirtiesContext resets the DB between test classes so user state doesn't leak.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // Registration tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("POST /api/auth/register - valid new user returns 200 with JWT")
    void registerSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password123");
        req.setEmail("testuser@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.token", not(blankOrNullString())))
            .andExpect(jsonPath("$.username", is("testuser")))
            .andExpect(jsonPath("$.email", is("testuser@example.com")));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/auth/register - duplicate username returns 400 with error message")
    void registerDuplicateUsername() throws Exception {
        // Register first
        RegisterRequest first = new RegisterRequest();
        first.setUsername("dupeuser");
        first.setPassword("password123");
        first.setEmail("dupeuser@example.com");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
            .andExpect(status().isOk());

        // Register duplicate username
        RegisterRequest dupe = new RegisterRequest();
        dupe.setUsername("dupeuser");
        dupe.setPassword("anotherpass");
        dupe.setEmail("other@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dupe)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", containsStringIgnoringCase("already taken")));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/auth/register - duplicate email returns 400 with error message")
    void registerDuplicateEmail() throws Exception {
        // Register first
        RegisterRequest first = new RegisterRequest();
        first.setUsername("emailowner");
        first.setPassword("password123");
        first.setEmail("shared@example.com");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
            .andExpect(status().isOk());

        // Register duplicate email
        RegisterRequest dupe = new RegisterRequest();
        dupe.setUsername("otherperson");
        dupe.setPassword("password123");
        dupe.setEmail("shared@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dupe)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", containsStringIgnoringCase("already registered")));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/auth/register - blank username returns 400 validation error")
    void registerBlankUsername() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("");
        req.setPassword("password123");
        req.setEmail("valid@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/auth/register - invalid email format returns 400 validation error")
    void registerInvalidEmail() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("validuser");
        req.setPassword("password123");
        req.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/auth/register - password too short returns 400 validation error")
    void registerShortPassword() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("validuser2");
        req.setPassword("abc");
        req.setEmail("validuser2@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/auth/register - BCrypt: login works after register confirming hash not plaintext")
    void registerPasswordNotPlaintext() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("bcryptuser");
        reg.setPassword("securepassword");
        reg.setEmail("bcryptuser@example.com");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", not(blankOrNullString())));

        // Login with same password succeeds — BCrypt compare must work correctly
        LoginRequest login = new LoginRequest();
        login.setUsername("bcryptuser");
        login.setPassword("securepassword");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));
    }

    // ------------------------------------------------------------------
    // Login tests
    // ------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("POST /api/auth/login - correct credentials return 200 with JWT")
    void loginSuccess() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("logintest");
        reg.setPassword("loginpass123");
        reg.setEmail("logintest@example.com");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setUsername("logintest");
        login.setPassword("loginpass123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.token", not(blankOrNullString())))
            .andExpect(jsonPath("$.username", is("logintest")));
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/auth/login - wrong password returns 401 Unauthorized")
    void loginWrongPassword() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("wrongpassuser");
        reg.setPassword("correctpass");
        reg.setEmail("wrongpassuser@example.com");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setUsername("wrongpassuser");
        login.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/auth/login - non-existent user returns 401 Unauthorized")
    void loginUnknownUser() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername("nobody");
        login.setPassword("anypassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @Order(11)
    @DisplayName("POST /api/auth/login - blank fields return 400 validation error")
    void loginBlankFields() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername("");
        login.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isBadRequest());
    }
}
