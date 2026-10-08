package com.cubesolve.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Kociemba Two-Phase Solver.
 * Tests solve a selection of known scrambles and verify the solution is correct.
 *
 * NOTE: The solver initialises its move and pruning tables on first call,
 *       which may take a few seconds. BeforeAll is used to pay this cost once.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KociembaSolverTest {

    private KociembaSolver solver;

    @BeforeAll
    void setUp() {
        solver = new KociembaSolver();
    }

    @Test
    @DisplayName("Solved cube returns empty solution")
    void solvedCubeEmptySolution() {
        long t0 = System.currentTimeMillis();
        CubeState cube = new CubeState();
        String solution = solver.solve(cube);
        System.out.println("solvedCubeEmptySolution took " + (System.currentTimeMillis() - t0) + " ms");
        assertTrue(solution.isBlank(), "Solved cube should have empty solution, got: " + solution);
    }

    @Test
    @DisplayName("Single move U produces a valid solution")
    void singleMoveU() {
        long t0 = System.currentTimeMillis();
        CubeState cube = CubeMoves.applyMove(new CubeState(), "U");
        String sol = solver.solve(cube);
        System.out.println("singleMoveU took " + (System.currentTimeMillis() - t0) + " ms, sol: " + sol);
        verifySolution(cube, sol);
    }

    @Test
    @DisplayName("Single move R2 produces a valid solution")
    void singleMoveR2() {
        long t0 = System.currentTimeMillis();
        CubeState cube = CubeMoves.applyMove(new CubeState(), "R2");
        String sol = solver.solve(cube);
        System.out.println("singleMoveR2 took " + (System.currentTimeMillis() - t0) + " ms, sol: " + sol);
        verifySolution(cube, sol);
    }

    @Test
    @DisplayName("Short scramble 'U R F' is solved correctly")
    void shortScramble() {
        long t0 = System.currentTimeMillis();
        String scramble = "U R F";
        CubeState cube = CubeMoves.applyMoves(new CubeState(), scramble);
        String sol = solver.solve(cube);
        System.out.println("shortScramble took " + (System.currentTimeMillis() - t0) + " ms, sol: " + sol);
        verifySolution(cube, sol);
    }

    @Test
    @DisplayName("Medium scramble (10 moves) is solved correctly")
    void mediumScramble() {
        long t0 = System.currentTimeMillis();
        String scramble = "U2 R' F2 D L2 B' U R D2 F";
        CubeState cube = CubeMoves.applyMoves(new CubeState(), scramble);
        String sol = solver.solve(cube);
        System.out.println("mediumScramble took " + (System.currentTimeMillis() - t0) + " ms, sol: " + sol);
        verifySolution(cube, sol);
    }

    @Test
    @DisplayName("Full WCA-length scramble (20 moves) is solved correctly")
    void fullScramble() {
        long t0 = System.currentTimeMillis();
        String scramble = "U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2";
        CubeState cube = CubeMoves.applyMoves(new CubeState(), scramble);
        String sol = solver.solve(cube);
        System.out.println("fullScramble took " + (System.currentTimeMillis() - t0) + " ms, sol: " + sol);
        verifySolution(cube, sol);
    }

    @Test
    @DisplayName("Invalid cube state throws exception")
    void invalidCubeThrows() {
        int[] f = new CubeState().getFacelets();
        f[0] = 1; // break color count
        CubeState badCube = new CubeState(f);
        assertThrows(IllegalArgumentException.class, () -> solver.solve(badCube));
    }

    /**
     * Applies the solution to the scrambled cube and asserts it is solved.
     */
    private void verifySolution(CubeState scrambled, String solution) {
        assertNotNull(solution, "Solution must not be null");
        CubeState result = CubeMoves.applyMoves(scrambled, solution);
        assertTrue(result.isSolved(),
            "Solution '" + solution + "' did not solve the cube. Remaining state: " + result);
    }
}
