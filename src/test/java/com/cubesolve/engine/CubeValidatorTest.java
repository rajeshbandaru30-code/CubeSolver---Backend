package com.cubesolve.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CubeValidator.
 */
class CubeValidatorTest {

    @Test
    @DisplayName("Solved cube passes validation")
    void solvedCubeIsValid() {
        CubeValidator.ValidationResult result = CubeValidator.validate(new CubeState());
        assertTrue(result.valid);
        assertTrue(result.errors.isEmpty());
    }

    @Test
    @DisplayName("Scrambled cube (from moves) passes validation")
    void scrambledCubeIsValid() {
        CubeState cube = CubeMoves.applyMoves(new CubeState(),
            "U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2");
        CubeValidator.ValidationResult result = CubeValidator.validate(cube);
        assertTrue(result.valid);
        assertTrue(result.errors.isEmpty());
    }

    @Test
    @DisplayName("Wrong color count fails validation (missing / duplicate pieces)")
    void wrongColorCountFails() {
        int[] f = new CubeState().getFacelets();
        f[0] = 1; // change U0 from White to Red → two Reds, zero Whites at this position
        CubeState cube = new CubeState(f);
        CubeValidator.ValidationResult result = CubeValidator.validate(cube);
        assertFalse(result.valid, "Should detect wrong color count");
        assertFalse(result.errors.isEmpty());
        assertTrue(result.errors.toString().contains("appears"));
    }

    @Test
    @DisplayName("Invalid facelets length throws exception (malformed input)")
    void invalidLengthThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new CubeState(new int[]{1, 2, 3}));
    }

    @Test
    @DisplayName("Center modification fails validation")
    void modifiedCenterFails() {
        int[] f = new CubeState().getFacelets();
        // Swap center of U (index 4) with center of R (index 13)
        int tmp = f[4]; f[4] = f[13]; f[13] = tmp;
        CubeState cube = new CubeState(f);
        CubeValidator.ValidationResult result = CubeValidator.validate(cube);
        assertFalse(result.valid);
        assertTrue(result.errors.toString().contains("Centers are fixed"));
    }

    @Test
    @DisplayName("Invalid edge orientation sum fails validation")
    void invalidEdgeOrientationFails() {
        // Flipping exactly one edge (UR)
        int[] f = new CubeState().getFacelets();
        // UR edge is U5 and R1
        int tmp = f[5]; f[5] = f[9+1]; f[9+1] = tmp;
        CubeState cube = new CubeState(f);
        CubeValidator.ValidationResult result = CubeValidator.validate(cube);
        assertFalse(result.valid);
        assertTrue(result.errors.toString().contains("Edge orientation sum"));
    }

    @Test
    @DisplayName("Invalid corner orientation sum fails validation")
    void invalidCornerOrientationFails() {
        // Twisting exactly one corner (URF)
        int[] f = new CubeState().getFacelets();
        // URF corner is U8, R0, F2
        int tmp = f[8]; f[8] = f[9]; f[9] = f[18+2]; f[18+2] = tmp;
        CubeState cube = new CubeState(f);
        CubeValidator.ValidationResult result = CubeValidator.validate(cube);
        assertFalse(result.valid);
        assertTrue(result.errors.toString().contains("Corner orientation sum"));
    }

    @Test
    @DisplayName("Impossible permutation parity fails validation")
    void invalidParityFails() {
        // Swapping exactly two edges (UR and UL) creates an odd parity
        int[] f = new CubeState().getFacelets();
        // UR: U5, R1. UL: U3, L1
        int tmp1 = f[5]; f[5] = f[3]; f[3] = tmp1;
        int tmp2 = f[9+1]; f[9+1] = f[36+1]; f[36+1] = tmp2;
        CubeState cube = new CubeState(f);
        CubeValidator.ValidationResult result = CubeValidator.validate(cube);
        assertFalse(result.valid);
        assertTrue(result.errors.toString().contains("Permutation parity mismatch"));
    }
}
