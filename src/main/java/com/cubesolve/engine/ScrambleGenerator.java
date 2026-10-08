package com.cubesolve.engine;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates random WCA-compliant scramble sequences for a 3x3 Rubik's Cube.
 *
 * A WCA scramble has these properties:
 *   - Default length: 20 moves
 *   - No two consecutive moves on the same face (e.g. U followed by U')
 *   - No two consecutive moves on opposite faces in a fixed order (e.g. U then D is allowed
 *     but D then U is disallowed to avoid redundant sequences)
 *   - Each move is chosen from the 18 standard moves
 */
public class ScrambleGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_SCRAMBLE_LENGTH = 20;

    // Face letters in order: U=0, D=1, R=2, L=3, F=4, B=5
    // Opposite pairs: U-D, R-L, F-B
    private static final int[] OPPOSITE = {1, 0, 3, 2, 5, 4};

    private ScrambleGenerator() { }

    /**
     * Generates a random scramble sequence of the default length (20 moves).
     *
     * @return space-separated move string
     */
    public static String generate() {
        return generate(DEFAULT_SCRAMBLE_LENGTH);
    }

    /**
     * Generates a random scramble of a given length and returns both
     * the move string and the resulting cube state.
     *
     * @param length number of moves
     * @return ScrambleResult containing the move string and final CubeState
     */
    public static ScrambleResult generateWithState(int length) {
        String moves = generate(length);
        CubeState state = CubeMoves.applyMoves(new CubeState(), moves);
        return new ScrambleResult(moves, state);
    }

    /**
     * Generates a random scramble sequence of a given length.
     *
     * @param length number of moves
     * @return space-separated move string
     */
    public static String generate(int length) {
        if (length < 1 || length > 100) {
            throw new IllegalArgumentException("Scramble length must be between 1 and 100.");
        }

        String[] suffixes = {"", "2", "'"};
        String[] faces = {"U", "D", "R", "L", "F", "B"};

        List<String> moves = new ArrayList<>();
        int lastFace = -1;
        int secondLastFace = -1;

        for (int i = 0; i < length; i++) {
            int face;
            int attempts = 0;
            do {
                face = RANDOM.nextInt(6);
                attempts++;
                if (attempts > 1000) break; // safety guard
            } while (
                face == lastFace ||
                (lastFace >= 0 && face == OPPOSITE[lastFace] && secondLastFace == face)
            );

            String suffix = suffixes[RANDOM.nextInt(3)];
            moves.add(faces[face] + suffix);

            secondLastFace = lastFace;
            lastFace = face;
        }

        return String.join(" ", moves);
    }

    /**
     * Holds the result of a scramble generation.
     */
    public record ScrambleResult(String moveSequence, CubeState cubeState) { }
}
