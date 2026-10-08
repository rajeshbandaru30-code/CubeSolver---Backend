package com.cubesolve.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for cube validation endpoint.
 */
public class ValidateRequest {

    @NotNull(message = "Facelets array is required.")
    @Size(min = 54, max = 54, message = "Facelets array must have exactly 54 elements.")
    private int[] facelets;

    public ValidateRequest() {}

    public ValidateRequest(int[] facelets) {
        this.facelets = facelets;
    }

    public int[] getFacelets() {
        return facelets;
    }

    public void setFacelets(int[] facelets) {
        this.facelets = facelets;
    }
}
