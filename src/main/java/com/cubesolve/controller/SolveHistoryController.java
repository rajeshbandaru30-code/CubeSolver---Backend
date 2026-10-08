package com.cubesolve.controller;

import com.cubesolve.dto.CreateSolveRequest;
import com.cubesolve.dto.SolveRecordDto;
import com.cubesolve.dto.UserStatsDto;
import com.cubesolve.model.User;
import com.cubesolve.service.SolveHistoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for solve history and practice stats.
 * No business logic — delegates entirely to SolveHistoryService.
 */
@RestController
@RequestMapping("/api/solves")
public class SolveHistoryController {

    private final SolveHistoryService historyService;

    @Autowired
    public SolveHistoryController(SolveHistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * POST /api/solves
     * Records a completed solve from Practice Mode.
     * Enforces authentication and mathematical verification that the cube is solved.
     */
    @PostMapping
    public ResponseEntity<?> recordSolve(
            @Valid @RequestBody CreateSolveRequest request,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required to save solve records."));
        }
        SolveRecordDto saved = historyService.recordSolve(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /api/solves/stats/my
     * Returns authentic user dashboard statistics computed directly from MySQL.
     */
    @GetMapping("/stats/my")
    public ResponseEntity<?> getMyStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required."));
        }
        UserStatsDto stats = historyService.getUserStats(user);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/solves
     * Returns the authenticated user's solve history. Requires JWT.
     * Prevents unauthorized global history exposure.
     */
    @GetMapping
    public ResponseEntity<List<SolveRecordDto>> getAll(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(historyService.getSolvesForUser(user));
    }

    /**
     * GET /api/solves/{id}
     * Returns a single solve record by ID, verifying that it belongs to the authenticated user.
     * Prevents Insecure Direct Object Reference (IDOR) attacks.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SolveRecordDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return historyService.getSolveByIdForUser(id, user)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/solves/my
     * Returns the authenticated user's solve history. Requires JWT.
     */
    @GetMapping("/my")
    public ResponseEntity<List<SolveRecordDto>> getMy(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(historyService.getSolvesForUser(user));
    }
}
