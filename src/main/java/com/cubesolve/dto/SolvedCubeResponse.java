package com.cubesolve.dto;

/**
 * Response DTO for solved cube endpoint.
 */
public class SolvedCubeResponse {

    private int[] facelets;
    private boolean solved;

    public SolvedCubeResponse() {}

    public SolvedCubeResponse(int[] facelets, boolean solved) {
        this.facelets = facelets;
        this.solved = solved;
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
}
