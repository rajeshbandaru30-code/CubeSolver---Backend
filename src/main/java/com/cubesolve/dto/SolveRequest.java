package com.cubesolve.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting a cube state to be solved.
 *
 * The facelets array must be exactly 54 integers, each in range 0–5:
 *   0=White(U), 1=Red(R), 2=Green(F), 3=Yellow(D), 4=Orange(L), 5=Blue(B)
 *
 * Layout: facelets[face * 9 + position]
 *   Faces: 0=U, 1=R, 2=F, 3=D, 4=L, 5=B
 *   Positions within a face (reading order):
 *     0 1 2
 *     3 4 5
 *     6 7 8
 */
public class SolveRequest {

    @NotNull(message = "Facelets array is required.")
    @Size(min = 54, max = 54, message = "Facelets array must have exactly 54 elements.")
    private int[] facelets;

    /**
     * Optional: the scramble that was applied (if user used our scramble generator).
     */
    @Size(max = 2000, message = "Scramble string cannot exceed 2000 characters.")
    private String scrambleMoves;

    public int[] getFacelets() { return facelets; }
    public void setFacelets(int[] facelets) { this.facelets = facelets; }

    public String getScrambleMoves() { return scrambleMoves; }
    public void setScrambleMoves(String scrambleMoves) { this.scrambleMoves = scrambleMoves; }
}
