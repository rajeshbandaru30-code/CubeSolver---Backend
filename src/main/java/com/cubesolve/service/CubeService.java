package com.cubesolve.service;

import com.cubesolve.dto.*;
import com.cubesolve.engine.CubeMoves;
import com.cubesolve.engine.CubeState;
import com.cubesolve.engine.CubeValidator;
import com.cubesolve.engine.KociembaSolver;
import com.cubesolve.engine.ScrambleGenerator;
import com.cubesolve.model.SolveRecord;
import com.cubesolve.model.User;
import com.cubesolve.repository.SolveRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Service for cube-related operations: validation, solving, scramble generation, move application.
 * All business logic lives here — never in controllers.
 */
@Service
public class CubeService {

    private static final Logger log = LoggerFactory.getLogger(CubeService.class);

    private final KociembaSolver solver;
    private final SolveRecordRepository solveRecordRepository;

    @Autowired
    public CubeService(SolveRecordRepository solveRecordRepository) {
        this.solver = new KociembaSolver();
        this.solveRecordRepository = solveRecordRepository;
    }

    /**
     * Validates a cube state from a request.
     */
    public ValidationResponse validate(ValidateRequest request) {
        if (request == null || request.getFacelets() == null) {
            throw new IllegalArgumentException("Facelets array must not be null.");
        }
        int[] facelets = request.getFacelets();
        if (facelets.length != 54) {
            throw new IllegalArgumentException("Facelets array must have exactly 54 elements.");
        }
        CubeState state = new CubeState(facelets);
        CubeValidator.ValidationResult result = CubeValidator.validate(state);
        return new ValidationResponse(result.isValid(), result.getErrors());
    }

    /**
     * Solves the cube described in the request, optionally saves to DB if user present,
     * and returns the solution.
     * Note: This method is intentionally NOT marked @Transactional so the CPU-bound Kociemba
     * search does not hold a database connection from HikariCP checked out for the entire solve duration.
     */
    public SolveResponse solve(SolveRequest request, User user) {
        long tReqStart = System.currentTimeMillis();
        if (request == null || request.getFacelets() == null) {
            throw new IllegalArgumentException("Facelets array must not be null.");
        }

        // 1. Validate cube state
        long tValStart = System.currentTimeMillis();
        CubeState cube = new CubeState(request.getFacelets());
        CubeValidator.ValidationResult validationResult = CubeValidator.validate(cube);
        long tVal = System.currentTimeMillis() - tValStart;

        if (!validationResult.isValid()) {
            String errorMsg = String.join("; ", validationResult.getErrors());
            log.warn("Invalid cube state submitted to solve: {}", errorMsg);
            throw new IllegalArgumentException("Invalid cube state: " + errorMsg);
        }

        // 2. Check if already solved
        if (cube.isSolved()) {
            log.info("Cube is already solved in {}ms.", System.currentTimeMillis() - tReqStart);
            return SolveResponse.alreadySolved();
        }

        // 3. Solve using Kociemba algorithm (in-memory CPU computation)
        long startTime = System.currentTimeMillis();
        String solutionMoves;
        try {
            solutionMoves = solver.solve(cube);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Solver execution error: ", e);
            throw new RuntimeException("Solver failed to solve the cube state: " + e.getMessage(), e);
        }
        long solveTimeMs = System.currentTimeMillis() - startTime;

        // 4. Verify solution correctness
        long tVerStart = System.currentTimeMillis();
        CubeState verified = CubeMoves.applyMoves(cube, solutionMoves);
        long tVer = System.currentTimeMillis() - tVerStart;

        if (!verified.isSolved()) {
            log.error("SOLVER BUG: Solution '{}' did not solve the cube!", solutionMoves);
            throw new IllegalStateException("Internal solver error: solution verification failed.");
        }

        // 5. Parse moves into list
        List<String> moveList = solutionMoves.isBlank()
            ? List.of()
            : Arrays.asList(solutionMoves.trim().split("\\s+"));
        int moveCount = moveList.size();

        // 6. Save to database if repository available (isolated quick transaction)
        long tDbStart = System.currentTimeMillis();
        Long recordId = persistSolveRecord(cube, request.getScrambleMoves(), solutionMoves, moveCount, solveTimeMs, user);
        long tDb = System.currentTimeMillis() - tDbStart;

        long totalMs = System.currentTimeMillis() - tReqStart;
        log.info("Solve request completed in {}ms [val: {}ms, engine: {}ms, verify: {}ms, db: {}ms, moves: {}]",
                totalMs, tVal, solveTimeMs, tVer, tDb, moveCount);

        return SolveResponse.success(solutionMoves, moveList, moveCount, solveTimeMs, recordId);
    }

    /**
     * Isolated, bounded transaction to persist solve record without holding
     * a connection during the CPU solver phase.
     */
    @Transactional
    public Long persistSolveRecord(CubeState cube, String scrambleMoves, String solutionMoves,
                                   int moveCount, long solveTimeMs, User user) {
        if (solveRecordRepository == null) return null;
        try {
            SolveRecord record = new SolveRecord();
            record.setCubeState(cube.toStorageString());
            record.setScrambleMoves(scrambleMoves);
            record.setSolutionMoves(solutionMoves);
            record.setMoveCount(moveCount);
            record.setSolveTimeMs(solveTimeMs);
            record.setUser(user);
            record = solveRecordRepository.save(record);
            return record.getId();
        } catch (Exception e) {
            log.warn("Failed to persist solve record: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generates a random scramble of requested length (default 20).
     */
    public ScrambleResponse scramble(ScrambleRequest request) {
        int length = (request != null && request.getLength() != null) ? request.getLength() : 20;
        ScrambleGenerator.ScrambleResult result = ScrambleGenerator.generateWithState(length);
        List<String> moveList = Arrays.asList(result.moveSequence().trim().split("\\s+"));
        return new ScrambleResponse(result.moveSequence(), moveList, result.cubeState().getFacelets());
    }

    /**
     * Applies a sequence of moves to a cube state and returns the resulting state.
     */
    public ApplyMoveResponse applyMove(ApplyMoveRequest request) {
        if (request == null || request.getFacelets() == null) {
            throw new IllegalArgumentException("Facelets array must not be null.");
        }
        if (request.getMove() == null || request.getMove().isBlank()) {
            throw new IllegalArgumentException("Move string must not be blank.");
        }

        int[] facelets = request.getFacelets();
        if (facelets.length != 54) {
            throw new IllegalArgumentException("Facelets array must have exactly 54 elements.");
        }

        CubeState state = new CubeState(facelets);
        CubeState result = CubeMoves.applyMoves(state, request.getMove());

        return new ApplyMoveResponse(result.getFacelets(), result.isSolved(), request.getMove().trim());
    }

    /**
     * Returns a solved 3x3x3 cube state.
     */
    public SolvedCubeResponse getSolvedCube() {
        CubeState solved = new CubeState();
        return new SolvedCubeResponse(solved.getFacelets(), true);
    }
}
