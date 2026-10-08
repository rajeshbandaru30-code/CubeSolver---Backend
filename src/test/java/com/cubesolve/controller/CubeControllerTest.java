package com.cubesolve.controller;

import com.cubesolve.dto.ApplyMoveRequest;
import com.cubesolve.dto.SolveRequest;
import com.cubesolve.dto.ValidateRequest;
import com.cubesolve.engine.CubeMoves;
import com.cubesolve.engine.CubeState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CubeController REST endpoints.
 * Tests every required scenario: valid cube, invalid cube, solved cube, scrambled cube, malformed request, solver execution.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CubeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/cube/solved returns a solved 54-facelet state")
    void getSolvedCube() throws Exception {
        mockMvc.perform(get("/api/cube/solved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.solved", is(true)))
            .andExpect(jsonPath("$.facelets", hasSize(54)));
    }

    @Test
    @DisplayName("POST /api/cube/validate returns valid=true for a solved cube")
    void validateSolvedCube() throws Exception {
        int[] facelets = new CubeState().getFacelets();
        ValidateRequest request = new ValidateRequest(facelets);

        mockMvc.perform(post("/api/cube/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid", is(true)))
            .andExpect(jsonPath("$.errors", empty()));
    }

    @Test
    @DisplayName("POST /api/cube/validate returns valid=false with errors for an invalid cube")
    void validateInvalidCube() throws Exception {
        int[] facelets = new CubeState().getFacelets();
        facelets[0] = 1; // Change White sticker to Red -> breaks color counts
        ValidateRequest request = new ValidateRequest(facelets);

        mockMvc.perform(post("/api/cube/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid", is(false)))
            .andExpect(jsonPath("$.errors", not(empty())));
    }

    @Test
    @DisplayName("POST /api/cube/scramble generates a valid scramble and facelets")
    void scramblePost() throws Exception {
        mockMvc.perform(post("/api/cube/scramble")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scrambleMoves", not(blankOrNullString())))
            .andExpect(jsonPath("$.moveList", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.facelets", hasSize(54)));
    }

    @Test
    @DisplayName("GET /api/cube/scramble returns scramble response")
    void scrambleGet() throws Exception {
        mockMvc.perform(get("/api/cube/scramble"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scrambleMoves", not(blankOrNullString())))
            .andExpect(jsonPath("$.facelets", hasSize(54)));
    }

    @Test
    @DisplayName("POST /api/cube/apply-move applies U move correctly")
    void applyMoveValid() throws Exception {
        int[] facelets = new CubeState().getFacelets();
        ApplyMoveRequest request = new ApplyMoveRequest(facelets, "U");

        mockMvc.perform(post("/api/cube/apply-move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.appliedMoves", is("U")))
            .andExpect(jsonPath("$.solved", is(false)))
            .andExpect(jsonPath("$.facelets", hasSize(54)));
    }

    @Test
    @DisplayName("POST /api/cube/apply-move with invalid move returns HTTP 400 Bad Request")
    void applyMoveInvalidMove() throws Exception {
        int[] facelets = new CubeState().getFacelets();
        ApplyMoveRequest request = new ApplyMoveRequest(facelets, "INVALID_MOVE_X99");

        mockMvc.perform(post("/api/cube/apply-move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }

    @Test
    @DisplayName("POST /api/cube/solve on solved cube returns HTTP 200 OK")
    void solveSolvedCube() throws Exception {
        SolveRequest request = new SolveRequest();
        request.setFacelets(new CubeState().getFacelets());

        mockMvc.perform(post("/api/cube/solve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.moveCount", is(0)));
    }

    @Test
    @DisplayName("POST /api/cube/solve on scrambled cube invokes solver and returns actual solution")
    void solveScrambledCube() throws Exception {
        // Scramble: U R F
        CubeState scrambled = CubeMoves.applyMoves(new CubeState(), "U R F");
        SolveRequest request = new SolveRequest();
        request.setFacelets(scrambled.getFacelets());

        mockMvc.perform(post("/api/cube/solve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.solutionMoves", not(blankOrNullString())))
            .andExpect(jsonPath("$.moveCount", greaterThan(0)));
    }

    @Test
    @DisplayName("POST /api/cube/solve on invalid cube returns HTTP 400 Bad Request without stack trace")
    void solveInvalidCubeReturns400() throws Exception {
        int[] facelets = new CubeState().getFacelets();
        facelets[0] = 1; // invalid state
        SolveRequest request = new SolveRequest();
        request.setFacelets(facelets);

        mockMvc.perform(post("/api/cube/solve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", containsString("Invalid cube state")))
            .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("Malformed JSON request returns HTTP 400 Bad Request without stack trace")
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/cube/solve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"facelets\": \"not_an_array\" }"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", not(blankOrNullString())))
            .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("Request with array of wrong size returns HTTP 400 Bad Request validation error")
    void wrongSizeArrayReturns400() throws Exception {
        SolveRequest request = new SolveRequest();
        request.setFacelets(new int[]{1, 2, 3}); // size 3 instead of 54

        mockMvc.perform(post("/api/cube/solve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.errors.facelets", not(blankOrNullString())));
    }
}
