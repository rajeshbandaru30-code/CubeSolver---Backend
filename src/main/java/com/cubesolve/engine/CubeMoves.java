package com.cubesolve.engine;

/**
 * Applies the 18 standard Rubik's Cube moves to a CubeState.
 *
 * Move naming convention (standard WCA):
 *   U, U2, U' (Up face clockwise, 180°, counter-clockwise)
 *   D, D2, D' (Down)
 *   R, R2, R' (Right)
 *   L, L2, L' (Left)
 *   F, F2, F' (Front)
 *   B, B2, B' (Back)
 *
 * Face layout reminder:
 *   0 1 2
 *   3 4 5
 *   6 7 8
 *
 * All moves return a NEW CubeState (immutable pattern).
 */
public class CubeMoves {

    private CubeMoves() {
        // Utility class — not instantiable
    }

    // -----------------------------------------------------------------------
    // Public API: apply a named move
    // -----------------------------------------------------------------------

    /**
     * Applies a named move (e.g. "U", "R'", "F2") to a cube and returns
     * a new CubeState.
     *
     * @param cube the source cube
     * @param move the move name
     * @return new cube state after the move
     */
    public static CubeState applyMove(CubeState cube, String move) {
        if (cube == null) {
            throw new IllegalArgumentException("Cube state cannot be null.");
        }
        if (move == null || move.isBlank()) {
            throw new IllegalArgumentException("Move cannot be null or blank.");
        }
        return switch (move) {
            case "U"  -> moveU(cube);
            case "U2" -> moveU2(cube);
            case "U'" -> moveUPrime(cube);
            case "D"  -> moveD(cube);
            case "D2" -> moveD2(cube);
            case "D'" -> moveDPrime(cube);
            case "R"  -> moveR(cube);
            case "R2" -> moveR2(cube);
            case "R'" -> moveRPrime(cube);
            case "L"  -> moveL(cube);
            case "L2" -> moveL2(cube);
            case "L'" -> moveLPrime(cube);
            case "F"  -> moveF(cube);
            case "F2" -> moveF2(cube);
            case "F'" -> moveFPrime(cube);
            case "B"  -> moveB(cube);
            case "B2" -> moveB2(cube);
            case "B'" -> moveBPrime(cube);
            default -> throw new IllegalArgumentException("Unknown move: " + move);
        };
    }

    /**
     * Applies a sequence of space-separated moves and returns the final state.
     */
    public static CubeState applyMoves(CubeState cube, String moveSequence) {
        if (moveSequence == null || moveSequence.isBlank()) {
            return new CubeState(cube);
        }
        CubeState current = new CubeState(cube);
        for (String move : moveSequence.trim().split("\\s+")) {
            current = applyMove(current, move);
        }
        return current;
    }

    /**
     * Returns the inverse (reverse + opposite) of a move sequence.
     * Used for verification and algorithm design.
     */
    public static String invertMoveSequence(String moves) {
        if (moves == null || moves.isBlank()) return "";
        String[] parts = moves.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            if (i < parts.length - 1) sb.append(" ");
            sb.append(invertMove(parts[i]));
        }
        return sb.toString();
    }

    private static String invertMove(String move) {
        if (move.endsWith("2")) return move; // 180° is its own inverse
        if (move.endsWith("'")) return move.substring(0, move.length() - 1);
        return move + "'";
    }

    // -----------------------------------------------------------------------
    // Helper: rotate a face's 9 stickers clockwise
    // -----------------------------------------------------------------------

    /**
     * Rotates a face's 9 stickers 90° clockwise in-place on the array.
     * Position mapping for CW rotation:
     *   0→2, 1→5, 2→8, 3→1, 4→4, 5→7, 6→0, 7→3, 8→6
     */
    private static void rotateFaceCW(int[] f, int face) {
        int base = face * 9;
        int tmp = f[base];
        f[base]     = f[base + 6];
        f[base + 6] = f[base + 8];
        f[base + 8] = f[base + 2];
        f[base + 2] = tmp;

        tmp = f[base + 1];
        f[base + 1] = f[base + 3];
        f[base + 3] = f[base + 7];
        f[base + 7] = f[base + 5];
        f[base + 5] = tmp;
    }

    /**
     * Rotates a face's 9 stickers 90° counter-clockwise in-place.
     */
    private static void rotateFaceCCW(int[] f, int face) {
        // CCW = 3x CW
        rotateFaceCW(f, face);
        rotateFaceCW(f, face);
        rotateFaceCW(f, face);
    }

    // -----------------------------------------------------------------------
    // U moves — Up face
    // -----------------------------------------------------------------------

    private static CubeState moveU(CubeState cube) {
        int[] f = cube.getFacelets();
        rotateFaceCW(f, CubeState.U);
        // Cycle: F top row → L top row → B top row → R top row
        // F(0,1,2) → L(0,1,2), L(0,1,2) → B(0,1,2), B(0,1,2) → R(0,1,2), R(0,1,2) → F(0,1,2)
        // But B is stored with orientation so B top row goes right-to-left visually:
        // F0->R0, F1->R1, F2->R2, R0->B2, R1->B1, R2->B0? NO — standard definition:
        // U CW: F top→R top, R top→B top, B top→L top, L top→F top (looking at U from above)
        int tmp0 = f[CubeState.F * 9 + 0];
        int tmp1 = f[CubeState.F * 9 + 1];
        int tmp2 = f[CubeState.F * 9 + 2];

        f[CubeState.F * 9 + 0] = f[CubeState.R * 9 + 0];
        f[CubeState.F * 9 + 1] = f[CubeState.R * 9 + 1];
        f[CubeState.F * 9 + 2] = f[CubeState.R * 9 + 2];

        f[CubeState.R * 9 + 0] = f[CubeState.B * 9 + 0];
        f[CubeState.R * 9 + 1] = f[CubeState.B * 9 + 1];
        f[CubeState.R * 9 + 2] = f[CubeState.B * 9 + 2];

        f[CubeState.B * 9 + 0] = f[CubeState.L * 9 + 0];
        f[CubeState.B * 9 + 1] = f[CubeState.L * 9 + 1];
        f[CubeState.B * 9 + 2] = f[CubeState.L * 9 + 2];

        f[CubeState.L * 9 + 0] = tmp0;
        f[CubeState.L * 9 + 1] = tmp1;
        f[CubeState.L * 9 + 2] = tmp2;

        return new CubeState(f);
    }

    private static CubeState moveU2(CubeState cube) {
        return moveU(moveU(cube));
    }

    private static CubeState moveUPrime(CubeState cube) {
        return moveU(moveU(moveU(cube)));
    }

    // -----------------------------------------------------------------------
    // D moves — Down face
    // -----------------------------------------------------------------------

    private static CubeState moveD(CubeState cube) {
        int[] f = cube.getFacelets();
        rotateFaceCW(f, CubeState.D);
        // D CW (looking from below): F bottom→L bottom, L bottom→B bottom, B bottom→R bottom, R bottom→F bottom
        int tmp0 = f[CubeState.F * 9 + 6];
        int tmp1 = f[CubeState.F * 9 + 7];
        int tmp2 = f[CubeState.F * 9 + 8];

        f[CubeState.F * 9 + 6] = f[CubeState.L * 9 + 6];
        f[CubeState.F * 9 + 7] = f[CubeState.L * 9 + 7];
        f[CubeState.F * 9 + 8] = f[CubeState.L * 9 + 8];

        f[CubeState.L * 9 + 6] = f[CubeState.B * 9 + 6];
        f[CubeState.L * 9 + 7] = f[CubeState.B * 9 + 7];
        f[CubeState.L * 9 + 8] = f[CubeState.B * 9 + 8];

        f[CubeState.B * 9 + 6] = f[CubeState.R * 9 + 6];
        f[CubeState.B * 9 + 7] = f[CubeState.R * 9 + 7];
        f[CubeState.B * 9 + 8] = f[CubeState.R * 9 + 8];

        f[CubeState.R * 9 + 6] = tmp0;
        f[CubeState.R * 9 + 7] = tmp1;
        f[CubeState.R * 9 + 8] = tmp2;

        return new CubeState(f);
    }

    private static CubeState moveD2(CubeState cube) {
        return moveD(moveD(cube));
    }

    private static CubeState moveDPrime(CubeState cube) {
        return moveD(moveD(moveD(cube)));
    }

    // -----------------------------------------------------------------------
    // R moves — Right face
    // -----------------------------------------------------------------------

    private static CubeState moveR(CubeState cube) {
        int[] f = cube.getFacelets();
        rotateFaceCW(f, CubeState.R);
        // R CW: U right col → B left col (reversed), B left col → D right col, D right col → F right col, F right col → U right col
        // Right column indices: 2,5,8
        // U col-right: U2, U5, U8 → F2, F5, F8
        // F col-right: F2, F5, F8 → D2, D5, D8
        // D col-right: D2, D5, D8 → B6, B3, B0 (reversed because B faces away)
        // B left col (reversed): B6, B3, B0 → U2, U5, U8
        int tmpU2 = f[CubeState.U * 9 + 2];
        int tmpU5 = f[CubeState.U * 9 + 5];
        int tmpU8 = f[CubeState.U * 9 + 8];

        f[CubeState.U * 9 + 2] = f[CubeState.F * 9 + 2];
        f[CubeState.U * 9 + 5] = f[CubeState.F * 9 + 5];
        f[CubeState.U * 9 + 8] = f[CubeState.F * 9 + 8];

        f[CubeState.F * 9 + 2] = f[CubeState.D * 9 + 2];
        f[CubeState.F * 9 + 5] = f[CubeState.D * 9 + 5];
        f[CubeState.F * 9 + 8] = f[CubeState.D * 9 + 8];

        f[CubeState.D * 9 + 2] = f[CubeState.B * 9 + 6];
        f[CubeState.D * 9 + 5] = f[CubeState.B * 9 + 3];
        f[CubeState.D * 9 + 8] = f[CubeState.B * 9 + 0];

        f[CubeState.B * 9 + 0] = tmpU8;
        f[CubeState.B * 9 + 3] = tmpU5;
        f[CubeState.B * 9 + 6] = tmpU2;

        return new CubeState(f);
    }

    private static CubeState moveR2(CubeState cube) {
        return moveR(moveR(cube));
    }

    private static CubeState moveRPrime(CubeState cube) {
        return moveR(moveR(moveR(cube)));
    }

    // -----------------------------------------------------------------------
    // L moves — Left face
    // -----------------------------------------------------------------------

    private static CubeState moveL(CubeState cube) {
        int[] f = cube.getFacelets();
        rotateFaceCW(f, CubeState.L);
        // L CW: F left col → U left col, U left col → B right col (reversed), B right col → D left col, D left col → F left col
        // Left column indices: 0,3,6
        int tmpU0 = f[CubeState.U * 9 + 0];
        int tmpU3 = f[CubeState.U * 9 + 3];
        int tmpU6 = f[CubeState.U * 9 + 6];

        f[CubeState.U * 9 + 0] = f[CubeState.B * 9 + 8];
        f[CubeState.U * 9 + 3] = f[CubeState.B * 9 + 5];
        f[CubeState.U * 9 + 6] = f[CubeState.B * 9 + 2];

        f[CubeState.B * 9 + 2] = f[CubeState.D * 9 + 6];
        f[CubeState.B * 9 + 5] = f[CubeState.D * 9 + 3];
        f[CubeState.B * 9 + 8] = f[CubeState.D * 9 + 0];

        f[CubeState.D * 9 + 0] = f[CubeState.F * 9 + 0];
        f[CubeState.D * 9 + 3] = f[CubeState.F * 9 + 3];
        f[CubeState.D * 9 + 6] = f[CubeState.F * 9 + 6];

        f[CubeState.F * 9 + 0] = tmpU0;
        f[CubeState.F * 9 + 3] = tmpU3;
        f[CubeState.F * 9 + 6] = tmpU6;

        return new CubeState(f);
    }

    private static CubeState moveL2(CubeState cube) {
        return moveL(moveL(cube));
    }

    private static CubeState moveLPrime(CubeState cube) {
        return moveL(moveL(moveL(cube)));
    }

    // -----------------------------------------------------------------------
    // F moves — Front face
    // -----------------------------------------------------------------------

    private static CubeState moveF(CubeState cube) {
        int[] f = cube.getFacelets();
        rotateFaceCW(f, CubeState.F);
        // F CW: U bottom row → R left col, R left col → D top row (reversed), D top row → L right col, L right col → U bottom row
        // U bottom: U6, U7, U8
        // R left col: R0, R3, R6
        // D top: D0, D1, D2
        // L right col: L2, L5, L8
        int tmpU6 = f[CubeState.U * 9 + 6];
        int tmpU7 = f[CubeState.U * 9 + 7];
        int tmpU8 = f[CubeState.U * 9 + 8];

        f[CubeState.U * 9 + 6] = f[CubeState.L * 9 + 8];
        f[CubeState.U * 9 + 7] = f[CubeState.L * 9 + 5];
        f[CubeState.U * 9 + 8] = f[CubeState.L * 9 + 2];

        f[CubeState.L * 9 + 2] = f[CubeState.D * 9 + 0];
        f[CubeState.L * 9 + 5] = f[CubeState.D * 9 + 1];
        f[CubeState.L * 9 + 8] = f[CubeState.D * 9 + 2];

        f[CubeState.D * 9 + 0] = f[CubeState.R * 9 + 6];
        f[CubeState.D * 9 + 1] = f[CubeState.R * 9 + 3];
        f[CubeState.D * 9 + 2] = f[CubeState.R * 9 + 0];

        f[CubeState.R * 9 + 0] = tmpU6;
        f[CubeState.R * 9 + 3] = tmpU7;
        f[CubeState.R * 9 + 6] = tmpU8;

        return new CubeState(f);
    }

    private static CubeState moveF2(CubeState cube) {
        return moveF(moveF(cube));
    }

    private static CubeState moveFPrime(CubeState cube) {
        return moveF(moveF(moveF(cube)));
    }

    // -----------------------------------------------------------------------
    // B moves — Back face
    // -----------------------------------------------------------------------

    private static CubeState moveB(CubeState cube) {
        int[] f = cube.getFacelets();
        rotateFaceCW(f, CubeState.B);
        // B CW (from behind): U top row → L left col (reversed), L left col → D bottom row, D bottom row → R right col (reversed), R right col → U top row
        // U top: U0, U1, U2
        // R right col: R2, R5, R8
        // D bottom: D6, D7, D8
        // L left col: L0, L3, L6
        int tmpU0 = f[CubeState.U * 9 + 0];
        int tmpU1 = f[CubeState.U * 9 + 1];
        int tmpU2 = f[CubeState.U * 9 + 2];

        f[CubeState.U * 9 + 0] = f[CubeState.R * 9 + 2];
        f[CubeState.U * 9 + 1] = f[CubeState.R * 9 + 5];
        f[CubeState.U * 9 + 2] = f[CubeState.R * 9 + 8];

        f[CubeState.R * 9 + 2] = f[CubeState.D * 9 + 8];
        f[CubeState.R * 9 + 5] = f[CubeState.D * 9 + 7];
        f[CubeState.R * 9 + 8] = f[CubeState.D * 9 + 6];

        f[CubeState.D * 9 + 6] = f[CubeState.L * 9 + 0];
        f[CubeState.D * 9 + 7] = f[CubeState.L * 9 + 3];
        f[CubeState.D * 9 + 8] = f[CubeState.L * 9 + 6];

        f[CubeState.L * 9 + 0] = tmpU2;
        f[CubeState.L * 9 + 3] = tmpU1;
        f[CubeState.L * 9 + 6] = tmpU0;

        return new CubeState(f);
    }

    private static CubeState moveB2(CubeState cube) {
        return moveB(moveB(cube));
    }

    private static CubeState moveBPrime(CubeState cube) {
        return moveB(moveB(moveB(cube)));
    }

    /**
     * All 18 standard move names, in the canonical order used by the solver.
     */
    public static final String[] ALL_MOVES = {
        "U", "U2", "U'",
        "D", "D2", "D'",
        "R", "R2", "R'",
        "L", "L2", "L'",
        "F", "F2", "F'",
        "B", "B2", "B'"
    };

    /**
     * Checks if a move string is one of the 18 legal Rubik's cube moves.
     */
    public static boolean isLegalMove(String move) {
        if (move == null) return false;
        for (String m : ALL_MOVES) {
            if (m.equals(move)) return true;
        }
        return false;
    }

    /**
     * The 6 face letters.
     */
    public static final String[] FACE_LETTERS = {"U", "D", "R", "L", "F", "B"};
}
