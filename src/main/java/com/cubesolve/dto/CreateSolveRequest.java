package com.cubesolve.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for recording a completed solve in Practice Mode.
 */
public class CreateSolveRequest {

    @Size(max = 500, message = "Cube state string cannot exceed 500 characters")
    private String cubeState;

    @Size(max = 2000, message = "Scramble moves string cannot exceed 2000 characters")
    private String scrambleMoves;

    @NotBlank(message = "Solution moves cannot be blank")
    @Size(max = 2000, message = "Solution moves string cannot exceed 2000 characters")
    private String solutionMoves;

    @Min(value = 1, message = "Move count must be at least 1")
    @Max(value = 1000, message = "Move count cannot exceed 1000")
    private int moveCount;

    @Min(value = 1, message = "Solve time must be positive")
    @Max(value = 86400000, message = "Solve time exceeds maximum reasonable session")
    private long solveTimeMs;

    public CreateSolveRequest() {}

    public CreateSolveRequest(String cubeState, String scrambleMoves, String solutionMoves, int moveCount, long solveTimeMs) {
        this.cubeState = cubeState;
        this.scrambleMoves = scrambleMoves;
        this.solutionMoves = solutionMoves;
        this.moveCount = moveCount;
        this.solveTimeMs = solveTimeMs;
    }

    public String getCubeState() {
        return cubeState;
    }

    public void setCubeState(String cubeState) {
        this.cubeState = cubeState;
    }

    public String getScrambleMoves() {
        return scrambleMoves;
    }

    public void setScrambleMoves(String scrambleMoves) {
        this.scrambleMoves = scrambleMoves;
    }

    public String getSolutionMoves() {
        return solutionMoves;
    }

    public void setSolutionMoves(String solutionMoves) {
        this.solutionMoves = solutionMoves;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public void setMoveCount(int moveCount) {
        this.moveCount = moveCount;
    }

    public long getSolveTimeMs() {
        return solveTimeMs;
    }

    public void setSolveTimeMs(long solveTimeMs) {
        this.solveTimeMs = solveTimeMs;
    }
}
