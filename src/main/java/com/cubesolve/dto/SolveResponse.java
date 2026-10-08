package com.cubesolve.dto;

import java.util.List;

/**
 * Response DTO returned after successfully solving a cube.
 */
public class SolveResponse {

    private boolean success;
    private String solutionMoves;      // e.g. "U R2 F' B R"
    private List<String> moveList;     // same moves as individual strings for animation
    private int moveCount;
    private long solveTimeMs;
    private Long solveRecordId;        // ID in the database
    private String message;

    // --- Static factory methods ---

    public static SolveResponse success(String solutionMoves, List<String> moveList,
                                        int moveCount, long solveTimeMs, Long recordId) {
        SolveResponse r = new SolveResponse();
        r.success = true;
        r.solutionMoves = solutionMoves;
        r.moveList = moveList;
        r.moveCount = moveCount;
        r.solveTimeMs = solveTimeMs;
        r.solveRecordId = recordId;
        r.message = "Solved in " + moveCount + " moves.";
        return r;
    }

    public static SolveResponse alreadySolved() {
        SolveResponse r = new SolveResponse();
        r.success = true;
        r.solutionMoves = "";
        r.moveList = List.of();
        r.moveCount = 0;
        r.solveTimeMs = 0;
        r.message = "The cube is already solved!";
        return r;
    }

    public static SolveResponse error(String message) {
        SolveResponse r = new SolveResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    // --- Getters ---
    public boolean isSuccess() { return success; }
    public String getSolutionMoves() { return solutionMoves; }
    public List<String> getMoveList() { return moveList; }
    public int getMoveCount() { return moveCount; }
    public long getSolveTimeMs() { return solveTimeMs; }
    public Long getSolveRecordId() { return solveRecordId; }
    public String getMessage() { return message; }

    // --- Setters (for Jackson) ---
    public void setSuccess(boolean success) { this.success = success; }
    public void setSolutionMoves(String solutionMoves) { this.solutionMoves = solutionMoves; }
    public void setMoveList(List<String> moveList) { this.moveList = moveList; }
    public void setMoveCount(int moveCount) { this.moveCount = moveCount; }
    public void setSolveTimeMs(long solveTimeMs) { this.solveTimeMs = solveTimeMs; }
    public void setSolveRecordId(Long solveRecordId) { this.solveRecordId = solveRecordId; }
    public void setMessage(String message) { this.message = message; }
}
