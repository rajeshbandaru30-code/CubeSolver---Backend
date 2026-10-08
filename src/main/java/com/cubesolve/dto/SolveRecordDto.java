package com.cubesolve.dto;

import java.time.LocalDateTime;

/** DTO for returning a solve record via REST API. */
public class SolveRecordDto {

    private Long id;
    private String cubeState;
    private String scrambleMoves;
    private String solutionMoves;
    private int moveCount;
    private long solveTimeMs;
    private String username;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCubeState() { return cubeState; }
    public void setCubeState(String cubeState) { this.cubeState = cubeState; }

    public String getScrambleMoves() { return scrambleMoves; }
    public void setScrambleMoves(String scrambleMoves) { this.scrambleMoves = scrambleMoves; }

    public String getSolutionMoves() { return solutionMoves; }
    public void setSolutionMoves(String solutionMoves) { this.solutionMoves = solutionMoves; }

    public int getMoveCount() { return moveCount; }
    public void setMoveCount(int moveCount) { this.moveCount = moveCount; }

    public long getSolveTimeMs() { return solveTimeMs; }
    public void setSolveTimeMs(long solveTimeMs) { this.solveTimeMs = solveTimeMs; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
