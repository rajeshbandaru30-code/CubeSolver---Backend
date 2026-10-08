package com.cubesolve.engine;

/**
 * Represents the face positions of a 3x3 Rubik's Cube.
 * Face index convention:
 *   0 = U (Up/White)
 *   1 = R (Right/Red)
 *   2 = F (Front/Green)
 *   3 = D (Down/Yellow)
 *   4 = L (Left/Orange)
 *   5 = B (Back/Blue)
 *
 * Each face has 9 stickers indexed 0–8 in reading order (top-left to bottom-right):
 *   0 1 2
 *   3 4 5
 *   6 7 8
 *
 * Color encoding:
 *   0 = U face color (White)
 *   1 = R face color (Red)
 *   2 = F face color (Green)
 *   3 = D face color (Yellow)
 *   4 = L face color (Orange)
 *   5 = B face color (Blue)
 */
public class CubeState {

    // Face indices
    public static final int U = 0;
    public static final int R = 1;
    public static final int F = 2;
    public static final int D = 3;
    public static final int L = 4;
    public static final int B = 5;

    // Color names for JSON serialization / display
    public static final String[] COLOR_NAMES = {"W", "R", "G", "Y", "O", "B"};

    /**
     * The cube is stored as a flat array of 54 integers.
     * facelets[face * 9 + position] = color code
     */
    private final int[] facelets;

    /**
     * Creates a solved cube.
     */
    public CubeState() {
        facelets = new int[54];
        for (int face = 0; face < 6; face++) {
            for (int pos = 0; pos < 9; pos++) {
                facelets[face * 9 + pos] = face;
            }
        }
    }

    /**
     * Creates a cube from an existing facelet array (deep copy).
     *
     * @param facelets 54-element int array
     */
    public CubeState(int[] facelets) {
        if (facelets == null || facelets.length != 54) {
            throw new IllegalArgumentException("Facelet array must have exactly 54 elements.");
        }
        this.facelets = facelets.clone();
    }

    /**
     * Copy constructor.
     */
    public CubeState(CubeState other) {
        this.facelets = other.facelets.clone();
    }

    /**
     * Returns a deep copy of the raw facelet array.
     */
    public int[] getFacelets() {
        return facelets.clone();
    }

    /**
     * Gets the color at a specific face and position.
     *
     * @param face     0–5
     * @param position 0–8
     * @return color code 0–5
     */
    public int getColor(int face, int position) {
        return facelets[face * 9 + position];
    }

    /**
     * Sets the color at a specific face and position.
     */
    public void setColor(int face, int position, int color) {
        facelets[face * 9 + position] = color;
    }

    /**
     * Checks if this cube is in the solved state.
     */
    public boolean isSolved() {
        for (int face = 0; face < 6; face++) {
            int center = facelets[face * 9 + 4]; // center sticker defines face color
            for (int pos = 0; pos < 9; pos++) {
                if (facelets[face * 9 + pos] != center) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a human-readable string of the cube state.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CubeState[\n");
        String[] faceNames = {"U", "R", "F", "D", "L", "B"};
        for (int face = 0; face < 6; face++) {
            sb.append("  ").append(faceNames[face]).append(": ");
            for (int pos = 0; pos < 9; pos++) {
                sb.append(COLOR_NAMES[facelets[face * 9 + pos]]);
                if (pos < 8) sb.append(" ");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns the facelet array as a comma-separated string (for persistence).
     */
    public String toStorageString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 54; i++) {
            if (i > 0) sb.append(",");
            sb.append(facelets[i]);
        }
        return sb.toString();
    }

    /**
     * Parses a storage string back into a CubeState.
     *
     * @param s comma-separated 54 integers
     * @return CubeState
     */
    public static CubeState fromStorageString(String s) {
        String[] parts = s.split(",");
        if (parts.length != 54) {
            throw new IllegalArgumentException("Storage string must contain exactly 54 values.");
        }
        int[] f = new int[54];
        for (int i = 0; i < 54; i++) {
            f[i] = Integer.parseInt(parts[i].trim());
        }
        return new CubeState(f);
    }
}
