package com.cubesolve.controller;

import com.cubesolve.dto.LoginRequest;
import com.cubesolve.dto.RegisterRequest;
import com.cubesolve.dto.SolveRequest;
import com.cubesolve.engine.CubeMoves;
import com.cubesolve.engine.CubeState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for solve history endpoints and security authorization:
 *   GET /api/solves           (requires JWT, returns only user's solves)
 *   GET /api/solves/{id}      (requires JWT, user-ownership enforced to prevent IDOR)
 *   GET /api/solves/my        (requires JWT)
 *
 * Verifies that:
 *   - Unauthenticated access to solve endpoints is rejected (401)
 *   - Users can only access their own solve records (IDOR protection)
 *   - Solve records are persisted after authenticated solves
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SolveHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private static final String USERNAME = "historyuser";
    private static final String PASSWORD = "historypass123";
    private static final String EMAIL    = "historyuser@example.com";

    @BeforeEach
    void registerAndLogin() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(USERNAME);
        reg.setPassword(PASSWORD);
        reg.setEmail(EMAIL);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        LoginRequest login = new LoginRequest();
        login.setUsername(USERNAME);
        login.setPassword(PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(body).get("token").asText();
    }

    // ------------------------------------------------------------------
    // Security & Authorization tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/solves without JWT returns 401 Unauthorized")
    void getHistoryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/solves"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/solves with valid JWT returns 200 with user's own solves")
    void getHistoryAuthenticated() throws Exception {
        mockMvc.perform(get("/api/solves")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("GET /api/solves/{id} without JWT returns 401 Unauthorized")
    void getSolveByIdUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/solves/999999"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/solves/{id} returns 404 for non-existent record when authenticated")
    void getSolveByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/solves/999999")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/solves/my without JWT returns 401 Unauthorized")
    void getMyHistoryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/solves/my"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/solves/my with valid JWT returns 200 with user's own history")
    void getMyHistoryAuthenticated() throws Exception {
        mockMvc.perform(get("/api/solves/my")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("Solve record is persisted and appears in /api/solves/my after authenticated solve")
    void solvePersistedInHistory() throws Exception {
        CubeState scrambled = CubeMoves.applyMoves(new CubeState(), "R U R' U'");
        SolveRequest req = new SolveRequest();
        req.setFacelets(scrambled.getFacelets());

        mockMvc.perform(post("/api/cube/solve")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));

        mockMvc.perform(get("/api/solves/my")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[0].solutionMoves", not(blankOrNullString())))
            .andExpect(jsonPath("$[0].moveCount", greaterThan(0)));
    }

    @Test
    @DisplayName("GET /api/solves/{id} returns the specific record after authenticated solve")
    void getSolveByIdFound() throws Exception {
        CubeState scrambled = CubeMoves.applyMoves(new CubeState(), "F B' L");
        SolveRequest req = new SolveRequest();
        req.setFacelets(scrambled.getFacelets());

        MvcResult solveResult = mockMvc.perform(post("/api/cube/solve")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        Long recordId = objectMapper.readTree(solveResult.getResponse().getContentAsString())
            .get("solveRecordId").asLong();

        mockMvc.perform(get("/api/solves/" + recordId)
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(recordId.intValue())))
            .andExpect(jsonPath("$.solutionMoves", not(blankOrNullString())));
    }

    @Test
    @DisplayName("GET /api/solves/{id} belonging to another user returns 404 (IDOR prevention)")
    void getSolveByIdOtherUserDenied() throws Exception {
        RegisterRequest reg2 = new RegisterRequest();
        reg2.setUsername("idorotheruser");
        reg2.setPassword("otherpass123");
        reg2.setEmail("idorotheruser@example.com");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg2)));

        LoginRequest login2 = new LoginRequest();
        login2.setUsername("idorotheruser");
        login2.setPassword("otherpass123");
        MvcResult res2 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login2)))
            .andExpect(status().isOk())
            .andReturn();
        String token2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("token").asText();

        // Create solve as user 1
        CubeState scrambled = CubeMoves.applyMoves(new CubeState(), "R U");
        SolveRequest req = new SolveRequest();
        req.setFacelets(scrambled.getFacelets());
        MvcResult solveResult = mockMvc.perform(post("/api/cube/solve")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();
        Long recordId = objectMapper.readTree(solveResult.getResponse().getContentAsString()).get("solveRecordId").asLong();

        // User 2 tries to access User 1's solve -> Must be 404 (IDOR prevented)
        mockMvc.perform(get("/api/solves/" + recordId)
                .header("Authorization", "Bearer " + token2))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/solves/my with invalid JWT returns 401")
    void getMyHistoryInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/solves/my")
                .header("Authorization", "Bearer invalid.jwt.token"))
            .andExpect(status().isUnauthorized());
    }
}
