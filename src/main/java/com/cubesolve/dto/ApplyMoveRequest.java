package com.cubesolve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for apply-move endpoint.
 */
public class ApplyMoveRequest {

    @NotNull(message = "Facelets array is required.")
    @Size(min = 54, max = 54, message = "Facelets array must have exactly 54 elements.")
    private int[] facelets;

    @NotBlank(message = "Move sequence is required.")
    @Size(max = 2000, message = "Move sequence cannot exceed 2000 characters.")
    private String move;

    public ApplyMoveRequest() {}

    public ApplyMoveRequest(int[] facelets, String move) {
        this.facelets = facelets;
        this.move = move;
    }

    public int[] getFacelets() {
        return facelets;
    }

    public void setFacelets(int[] facelets) {
        this.facelets = facelets;
    }

    public String getMove() {
        return move;
    }

    public void setMove(String move) {
        this.move = move;
    }
}
