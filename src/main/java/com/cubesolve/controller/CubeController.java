package com.cubesolve.controller;

import com.cubesolve.dto.*;
import com.cubesolve.model.User;
import com.cubesolve.service.CubeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Rubik's Cube operations.
 * Strictly delegating to CubeService — no business logic in controller.
 */
@RestController
@RequestMapping("/api/cube")
public class CubeController {

    private final CubeService cubeService;

    @Autowired
    public CubeController(CubeService cubeService) {
        this.cubeService = cubeService;
    }

    /**
     * POST /api/cube/validate
     * Validates whether a 54-facelet cube configuration represents a solvable Rubik's Cube.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validate(@Valid @RequestBody ValidateRequest request) {
        ValidationResponse response = cubeService.validate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/cube/solve
     * Solves the given cube configuration using the Java Kociemba solver.
     */
    @PostMapping("/solve")
    public ResponseEntity<SolveResponse> solve(
            @Valid @RequestBody SolveRequest request,
            @AuthenticationPrincipal User user) {
        SolveResponse response = cubeService.solve(request, user);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/cube/scramble
     * Generates a random scramble sequence and resulting cube state.
     */
    @PostMapping("/scramble")
    public ResponseEntity<ScrambleResponse> scramblePost(@RequestBody(required = false) ScrambleRequest request) {
        ScrambleResponse response = cubeService.scramble(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cube/scramble
     * Convenience GET endpoint for scramble generation.
     */
    @GetMapping("/scramble")
    public ResponseEntity<ScrambleResponse> scrambleGet() {
        ScrambleResponse response = cubeService.scramble(null);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/cube/apply-move
     * Applies a move or sequence of moves to a cube state.
     */
    @PostMapping("/apply-move")
    public ResponseEntity<ApplyMoveResponse> applyMove(@Valid @RequestBody ApplyMoveRequest request) {
        ApplyMoveResponse response = cubeService.applyMove(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cube/solved
     * Returns a standard solved 3x3x3 Rubik's Cube state.
     */
    @GetMapping("/solved")
    public ResponseEntity<SolvedCubeResponse> getSolvedCube() {
        SolvedCubeResponse response = cubeService.getSolvedCube();
        return ResponseEntity.ok(response);
    }
}
