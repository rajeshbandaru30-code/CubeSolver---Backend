package com.cubesolve.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CubeState and CubeMoves.
 * These tests verify that:
 *   1. A solved cube is recognised as solved.
 *   2. A move followed by its inverse returns to the solved state.
 *   3. A move applied 4 times returns to the solved state.
 *   4. A known scramble produces the expected cube state (regression test).
 */
class CubeMovesTest {

    @Test
    @DisplayName("Solved cube reports isSolved() = true")
    void solvedCubeIsSolved() {
        assertTrue(new CubeState().isSolved());
    }

    @Test
    @DisplayName("Each move followed by its inverse returns to solved state")
    void moveAndInverse() {
        String[][] pairs = {
            {"U", "U'"}, {"U'", "U"}, {"U2", "U2"},
            {"D", "D'"}, {"D'", "D"}, {"D2", "D2"},
            {"R", "R'"}, {"R'", "R"}, {"R2", "R2"},
            {"L", "L'"}, {"L'", "L"}, {"L2", "L2"},
            {"F", "F'"}, {"F'", "F"}, {"F2", "F2"},
            {"B", "B'"}, {"B'", "B"}, {"B2", "B2"},
        };
        for (String[] pair : pairs) {
            CubeState cube = new CubeState();
            cube = CubeMoves.applyMove(cube, pair[0]);
            cube = CubeMoves.applyMove(cube, pair[1]);
            assertTrue(cube.isSolved(),
                "Move " + pair[0] + " followed by " + pair[1] + " should return to solved state");
        }
    }

    @Test
    @DisplayName("Each move applied 4 times returns to solved state")
    void moveFourTimes() {
        for (String move : new String[]{"U", "D", "R", "L", "F", "B"}) {
            CubeState cube = new CubeState();
            for (int i = 0; i < 4; i++) {
                cube = CubeMoves.applyMove(cube, move);
            }
            assertTrue(cube.isSolved(), "Move " + move + " x4 should return to solved state");
        }
    }

    @Test
    @DisplayName("U2 applied twice returns to solved state")
    void doubleMoveAppliedTwice() {
        for (String move : new String[]{"U2", "D2", "R2", "L2", "F2", "B2"}) {
            CubeState cube = new CubeState();
            cube = CubeMoves.applyMove(cube, move);
            cube = CubeMoves.applyMove(cube, move);
            assertTrue(cube.isSolved(), move + " applied twice should return to solved state");
        }
    }

    @Test
    @DisplayName("Known scramble 'U R F' does not leave the cube solved")
    void simpleScrambleNotSolved() {
        CubeState cube = CubeMoves.applyMoves(new CubeState(), "U R F");
        assertFalse(cube.isSolved());
    }

    @Test
    @DisplayName("Scramble then inverse scramble returns to solved state")
    void scrambleAndInverse() {
        String scramble = "U R2 F B R B2 R U2 L B2 R U' D' R2 F R'";
        String inverse = CubeMoves.invertMoveSequence(scramble);
        CubeState cube = CubeMoves.applyMoves(new CubeState(), scramble);
        assertFalse(cube.isSolved(), "After scramble, cube should not be solved");
        cube = CubeMoves.applyMoves(cube, inverse);
        assertTrue(cube.isSolved(), "After inverse scramble, cube should be solved");
    }

    @Test
    @DisplayName("Superflip (all edges flipped) is not solved")
    void superflipNotSolved() {
        // The superflip is a famous 20-move-optimal position
        String superflip = "U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2";
        CubeState cube = CubeMoves.applyMoves(new CubeState(), superflip);
        assertFalse(cube.isSolved(), "Superflip should not be solved");
    }

    @Test
    @DisplayName("toStorageString and fromStorageString round-trip")
    void storageStringRoundTrip() {
        CubeState cube = CubeMoves.applyMoves(new CubeState(), "U R2 F' D L B2");
        String stored = cube.toStorageString();
        CubeState restored = CubeState.fromStorageString(stored);
        assertArrayEquals(cube.getFacelets(), restored.getFacelets());
    }
}
