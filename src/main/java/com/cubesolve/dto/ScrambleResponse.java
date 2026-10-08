package com.cubesolve.dto;

import java.util.List;

/**
 * Response DTO for scramble generation endpoint.
 */
public class ScrambleResponse {

    private String scrambleMoves;
    private List<String> moveList;
    private int[] facelets;

    public ScrambleResponse() {}

    public ScrambleResponse(String scrambleMoves, List<String> moveList, int[] facelets) {
        this.scrambleMoves = scrambleMoves;
        this.moveList = moveList;
        this.facelets = facelets;
    }

    public String getScrambleMoves() {
        return scrambleMoves;
    }

    public void setScrambleMoves(String scrambleMoves) {
        this.scrambleMoves = scrambleMoves;
    }

    public List<String> getMoveList() {
        return moveList;
    }

    public void setMoveList(List<String> moveList) {
        this.moveList = moveList;
    }

    public int[] getFacelets() {
        return facelets;
    }

    public void setFacelets(int[] facelets) {
        this.facelets = facelets;
    }
}
