package com.cubesolve.dto;

/**
 * Response DTO for apply-move endpoint.
 */
public class ApplyMoveResponse {

    private int[] facelets;
    private boolean solved;
    private String appliedMoves;

    public ApplyMoveResponse() {}

    public ApplyMoveResponse(int[] facelets, boolean solved, String appliedMoves) {
        this.facelets = facelets;
        this.solved = solved;
        this.appliedMoves = appliedMoves;
    }

    public int[] getFacelets() {
        return facelets;
    }

    public void setFacelets(int[] facelets) {
        this.facelets = facelets;
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public String getAppliedMoves() {
        return appliedMoves;
    }

    public void setAppliedMoves(String appliedMoves) {
        this.appliedMoves = appliedMoves;
    }
}
