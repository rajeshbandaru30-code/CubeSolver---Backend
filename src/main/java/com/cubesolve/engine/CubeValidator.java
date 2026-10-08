package com.cubesolve.engine;

/**
 * Validates a Rubik's Cube state.
 *
 * A valid Rubik's Cube state must satisfy all of:
 *   1. Each of the 6 colors appears exactly 9 times (54 stickers total).
 *   2. Center stickers are fixed (color of center = face number).
 *   3. Corner cubie orientation sum ≡ 0 (mod 3).
 *   4. Edge cubie orientation sum ≡ 0 (mod 2).
 *   5. Permutation parity of corners and edges is consistent (same parity).
 *
 * These 5 conditions are NECESSARY and SUFFICIENT for solvability of a
 * standard 3x3 Rubik's Cube.
 *
 * References:
 *   - Kociemba, Herbert. "Cube Explorer"
 *   - Reid, M. "Superflip requires 20 face turns" (1995)
 */
public class CubeValidator {

    private CubeValidator() { }

    public static class ValidationResult {
        public final boolean valid;
        public final java.util.List<String> errors;

        public ValidationResult(boolean valid, java.util.List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public java.util.List<String> getErrors() {
            return errors;
        }
    }

    /**
     * Returns a ValidationResult with boolean valid and a list of errors.
     *
     * @param cube the cube state to validate
     * @return structured ValidationResult
     */
    public static ValidationResult validate(CubeState cube) {
        java.util.List<String> errors = new java.util.ArrayList<>();
        int[] f = cube.getFacelets();

        // --- Check 1: Color counts ---
        int[] count = new int[6];
        for (int i = 0; i < 54; i++) {
            if (f[i] < 0 || f[i] > 5) {
                errors.add("Invalid color code " + f[i] + " at index " + i + ". Must be 0–5.");
                return new ValidationResult(false, errors); // Fatal, can't continue
            }
            count[f[i]]++;
        }
        for (int c = 0; c < 6; c++) {
            if (count[c] != 9) {
                errors.add("Color " + CubeState.COLOR_NAMES[c] + " appears " + count[c] + " times (expected 9).");
            }
        }
        
        if (!errors.isEmpty()) {
            return new ValidationResult(false, errors); // Cannot do physical checks if colors are wrong
        }

        // --- Check 2: Center stickers ---
        // Centers are at positions 4 of each face (face*9+4)
        // In a standard cube the center of face F always has color F
        for (int face = 0; face < 6; face++) {
            if (f[face * 9 + 4] != face) {
                errors.add("Center of face " + face + " is " + f[face * 9 + 4] + " but must be " + face + ". Centers are fixed.");
            }
        }

        // --- Checks 3, 4, 5: Use the cubie (corner + edge) model ---
        // We need to map the facelet array to corner and edge cubies.
        // Corner cubie definitions (each entry: 3 facelets that form one corner cubie)
        // Order: URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB
        int[][] cornerFacelets = {
            {CubeState.U*9+8, CubeState.R*9+0, CubeState.F*9+2},  // URF
            {CubeState.U*9+6, CubeState.F*9+0, CubeState.L*9+2},  // UFL
            {CubeState.U*9+0, CubeState.L*9+0, CubeState.B*9+2},  // ULB
            {CubeState.U*9+2, CubeState.B*9+0, CubeState.R*9+2},  // UBR
            {CubeState.D*9+2, CubeState.F*9+8, CubeState.R*9+6},  // DFR
            {CubeState.D*9+0, CubeState.L*9+8, CubeState.F*9+6},  // DLF
            {CubeState.D*9+6, CubeState.B*9+8, CubeState.L*9+6},  // DBL
            {CubeState.D*9+8, CubeState.R*9+8, CubeState.B*9+6},  // DRB
        };
        // Color of each corner position (face color at the U/D slot, R/L slot, F/B slot)
        int[][] cornerColors = {
            {CubeState.U, CubeState.R, CubeState.F},
            {CubeState.U, CubeState.F, CubeState.L},
            {CubeState.U, CubeState.L, CubeState.B},
            {CubeState.U, CubeState.B, CubeState.R},
            {CubeState.D, CubeState.F, CubeState.R},
            {CubeState.D, CubeState.L, CubeState.F},
            {CubeState.D, CubeState.B, CubeState.L},
            {CubeState.D, CubeState.R, CubeState.B},
        };

        // Edge cubie definitions (each: 2 facelets)
        // Order: UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR
        int[][] edgeFacelets = {
            {CubeState.U*9+5, CubeState.R*9+1},  // UR
            {CubeState.U*9+7, CubeState.F*9+1},  // UF
            {CubeState.U*9+3, CubeState.L*9+1},  // UL
            {CubeState.U*9+1, CubeState.B*9+1},  // UB
            {CubeState.D*9+5, CubeState.R*9+7},  // DR
            {CubeState.D*9+1, CubeState.F*9+7},  // DF
            {CubeState.D*9+3, CubeState.L*9+7},  // DL
            {CubeState.D*9+7, CubeState.B*9+7},  // DB
            {CubeState.F*9+5, CubeState.R*9+3},  // FR
            {CubeState.F*9+3, CubeState.L*9+5},  // FL
            {CubeState.B*9+5, CubeState.L*9+3},  // BL
            {CubeState.B*9+3, CubeState.R*9+5},  // BR
        };
        int[][] edgeColors = {
            {CubeState.U, CubeState.R},
            {CubeState.U, CubeState.F},
            {CubeState.U, CubeState.L},
            {CubeState.U, CubeState.B},
            {CubeState.D, CubeState.R},
            {CubeState.D, CubeState.F},
            {CubeState.D, CubeState.L},
            {CubeState.D, CubeState.B},
            {CubeState.F, CubeState.R},
            {CubeState.F, CubeState.L},
            {CubeState.B, CubeState.L},
            {CubeState.B, CubeState.R},
        };

        // --- Check 3: Corner orientation ---
        int[] cornerPerm = new int[8];
        int[] cornerOrient = new int[8];
        boolean[] cornerUsed = new boolean[8];

        for (int i = 0; i < 8; i++) {
            int c0 = f[cornerFacelets[i][0]];
            int c1 = f[cornerFacelets[i][1]];
            int c2 = f[cornerFacelets[i][2]];

            // Find which corner cubie this is
            int found = -1;
            int ori = 0;
            outer:
            for (int j = 0; j < 8; j++) {
                for (int o = 0; o < 3; o++) {
                    int[] ref = cornerColors[j];
                    int rc0 = ref[o % 3];
                    int rc1 = ref[(o + 1) % 3];
                    int rc2 = ref[(o + 2) % 3];
                    if (c0 == rc0 && c1 == rc1 && c2 == rc2) {
                        found = j;
                        ori = o;
                        break outer;
                    }
                }
            }
            if (found == -1) {
                errors.add("Invalid corner cubie at position " + i + ". Colors: "
                    + CubeState.COLOR_NAMES[c0] + CubeState.COLOR_NAMES[c1] + CubeState.COLOR_NAMES[c2]);
                return new ValidationResult(false, errors);
            }
            if (cornerUsed[found]) {
                errors.add("Corner cubie " + found + " appears more than once.");
                return new ValidationResult(false, errors);
            }
            cornerUsed[found] = true;
            cornerPerm[i] = found;
            cornerOrient[i] = ori;
        }

        int cornerOrientSum = 0;
        for (int o : cornerOrient) cornerOrientSum += o;
        if (cornerOrientSum % 3 != 0) {
            errors.add("Corner orientation sum is " + cornerOrientSum + ", must be divisible by 3.");
        }

        // --- Check 4: Edge orientation ---
        int[] edgePerm = new int[12];
        int[] edgeOrient = new int[12];
        boolean[] edgeUsed = new boolean[12];

        for (int i = 0; i < 12; i++) {
            int e0 = f[edgeFacelets[i][0]];
            int e1 = f[edgeFacelets[i][1]];

            int found = -1;
            int ori = 0;
            for (int j = 0; j < 12; j++) {
                if (e0 == edgeColors[j][0] && e1 == edgeColors[j][1]) {
                    found = j; ori = 0; break;
                }
                if (e0 == edgeColors[j][1] && e1 == edgeColors[j][0]) {
                    found = j; ori = 1; break;
                }
            }
            if (found == -1) {
                errors.add("Invalid edge cubie at position " + i + ". Colors: "
                    + CubeState.COLOR_NAMES[e0] + CubeState.COLOR_NAMES[e1]);
                return new ValidationResult(false, errors);
            }
            if (edgeUsed[found]) {
                errors.add("Edge cubie " + found + " appears more than once.");
                return new ValidationResult(false, errors);
            }
            edgeUsed[found] = true;
            edgePerm[i] = found;
            edgeOrient[i] = ori;
        }

        int edgeOrientSum = 0;
        for (int o : edgeOrient) edgeOrientSum += o;
        if (edgeOrientSum % 2 != 0) {
            errors.add("Edge orientation sum is " + edgeOrientSum + ", must be even.");
        }

        // --- Check 5: Permutation parity ---
        if (errors.isEmpty()) {
            int cornerParity = permutationParity(cornerPerm);
            int edgeParity = permutationParity(edgePerm);
            if (cornerParity != edgeParity) {
                errors.add("Permutation parity mismatch: corner parity=" + cornerParity + ", edge parity=" + edgeParity
                    + ". The cube is not physically achievable.");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Returns the parity (0=even, 1=odd) of a permutation array.
     */
    private static int permutationParity(int[] perm) {
        boolean[] visited = new boolean[perm.length];
        int parity = 0;
        for (int i = 0; i < perm.length; i++) {
            if (!visited[i]) {
                int cycleLen = 0;
                int j = i;
                while (!visited[j]) {
                    visited[j] = true;
                    j = perm[j];
                    cycleLen++;
                }
                if (cycleLen % 2 == 0) parity ^= 1;
            }
        }
        return parity;
    }

    /**
     * Throws an {@link IllegalArgumentException} if the cube state is invalid.
     */
    public static void validateOrThrow(CubeState cube) {
        ValidationResult result = validate(cube);
        if (!result.valid) {
            throw new IllegalArgumentException("Invalid cube state: " + String.join(", ", result.errors));
        }
    }
}
