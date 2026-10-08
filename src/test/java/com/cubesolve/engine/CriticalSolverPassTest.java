package com.cubesolve.engine;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous Critical Solver Test Pass:
 * Tests the Rubik's Cube engine and Kociemba solver across dozens of scrambles and edge cases.
 *
 * For every scramble:
 *   1. Create solved cube.
 *   2. Apply scramble.
 *   3. Confirm cube is not solved (unless identity).
 *   4. Run Java solver.
 *   5. Apply solution.
 *   6. Confirm cube is solved.
 *   7. Confirm every solution move is legal.
 *   8. Record solving time and move count.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CriticalSolverPassTest {

    private KociembaSolver solver;

    // Execution statistics
    private final List<Long> solveTimes = new ArrayList<>();
    private final List<Integer> moveCounts = new ArrayList<>();
    private int totalScramblesTested = 0;

    @BeforeAll
    void init() {
        solver = new KociembaSolver();
        // Warm up solver tables
        solver.solve(new CubeState());
    }

    @AfterAll
    void printReport() {
        System.out.println("========================================================================");
        System.out.println("CRITICAL SOLVER TEST PASS REPORT");
        System.out.println("========================================================================");
        System.out.printf("Total Scrambles Tested: %d%n", totalScramblesTested);

        if (!solveTimes.isEmpty()) {
            long minTime = solveTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxTime = solveTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            double avgTime = solveTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

            int minMoves = moveCounts.stream().mapToInt(Integer::intValue).min().orElse(0);
            int maxMoves = moveCounts.stream().mapToInt(Integer::intValue).max().orElse(0);
            double avgMoves = moveCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0);

            System.out.printf("Solve Times (ms): Min = %d ms | Max = %d ms | Avg = %.2f ms%n", minTime, maxTime, avgTime);
            System.out.printf("Move Counts:      Min = %d | Max = %d | Avg = %.1f moves%n", minMoves, maxMoves, avgMoves);
        }
        System.out.println("All solutions verified mathematically consistent and legally valid.");
        System.out.println("========================================================================");
    }

    /**
     * Executes the mandatory 8-step verification pipeline for a given scramble.
     */
    private void runFullScramblePipeline(String scramble, String label) {
        totalScramblesTested++;

        // 1. Create solved cube
        CubeState initialCube = new CubeState();
        assertTrue(initialCube.isSolved(), "Initial cube must be solved");

        // 2. Apply scramble
        CubeState scrambledCube = CubeMoves.applyMoves(initialCube, scramble);

        // 3. Confirm cube is not solved (for non-identity scrambles)
        assertFalse(scrambledCube.isSolved(), "[" + label + "] Scrambled cube should NOT be solved before solver runs");

        // 4. Run Java solver & record time
        long startNs = System.nanoTime();
        String solution = solver.solve(scrambledCube);
        long durationMs = (System.nanoTime() - startNs) / 1_000_000;

        assertNotNull(solution, "[" + label + "] Solution must not be null");
        assertFalse(solution.isBlank(), "[" + label + "] Solution must not be blank for scrambled cube");

        // 7. Confirm every solution move is legal
        String[] solutionMoves = solution.trim().split("\\s+");
        for (String move : solutionMoves) {
            assertTrue(CubeMoves.isLegalMove(move),
                "[" + label + "] Illegal move found in solution: '" + move + "'");
        }

        // 5. Apply solution
        CubeState solvedCube = CubeMoves.applyMoves(scrambledCube, solution);

        // 6. Confirm cube is solved
        assertTrue(solvedCube.isSolved(),
            "[" + label + "] Cube state NOT solved after applying solution: " + solution);

        // 8. Record solving time and move count
        solveTimes.add(durationMs);
        moveCounts.add(solutionMoves.length);
    }

    // -------------------------------------------------------------------------
    // TEST 1: Solved Cube
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Solved cube returns empty solution and remains solved")
    void testSolvedCube() {
        CubeState cube = new CubeState();
        assertTrue(cube.isSolved());
        String solution = solver.solve(cube);
        assertTrue(solution.isBlank(), "Solved cube must yield empty solution string");
        CubeState after = CubeMoves.applyMoves(cube, solution);
        assertTrue(after.isSolved());
    }

    // -------------------------------------------------------------------------
    // TEST 2: All 18 One-Move Scrambles
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("All 18 standard one-move scrambles are solved correctly")
    void testAllOneMoveScrambles() {
        for (String move : CubeMoves.ALL_MOVES) {
            runFullScramblePipeline(move, "OneMove:" + move);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 3: Short Scrambles (2 to 5 moves)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Short scrambles (2 to 5 moves) are solved correctly")
    void testShortScrambles() {
        String[] shortScrambles = {
            "R U",
            "F R U",
            "R U R' U'",
            "F B' L R2 D",
            "U D R L F B",
            "L' U2 R D' F",
            "B2 R' D2 F U'"
        };
        for (String sc : shortScrambles) {
            runFullScramblePipeline(sc, "ShortScramble:" + sc);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 4: Medium Scrambles (8 to 15 moves)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Medium scrambles (8 to 15 moves) are solved correctly")
    void testMediumScrambles() {
        String[] mediumScrambles = {
            "U2 R' F2 D L2 B' U R",
            "U2 R' F2 D L2 B' U R D2 F",
            "B2 L2 U F2 R2 D B2 L2 U2 F2",
            "D2 L2 F2 D' B2 D L2 U' R2 B2 R'",
            "R U R' U' R' F R2 U' R' U' R U R' F'"
        };
        for (String sc : mediumScrambles) {
            runFullScramblePipeline(sc, "MediumScramble:" + sc);
        }
    }

    // -------------------------------------------------------------------------
    // TEST 5: Long & Standard WCA Scrambles (20 to 30 moves)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Long and WCA-length scrambles (20 to 30 moves) are solved correctly")
    void testLongScrambles() {
        String[] longScrambles = {
            // Superflip (optimal 20 moves)
            "U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2",
            // Checkerboard
            "U2 D2 F2 B2 L2 R2",
            // Wire pattern
            "R L F B R L F B R L F B",
            // 20-move WCA scrambles
            "F2 D' R2 U' B2 L2 U' F2 D' L2 B2 F' R D' B2 L' D' F2 U B2",
            "L2 B2 D2 F2 U' L2 U' F2 D' R2 U2 B' R2 B' L' F' R2 D' F' R2",
            "D' R2 B2 U2 R2 D B2 D' F2 U B2 R' D B R2 F D' B2 L U'",
            "B2 D2 L2 F2 R2 D' B2 D2 F2 U' R2 B' L' F D' L F' D2 L2 B'"
        };
        for (String sc : longScrambles) {
            runFullScramblePipeline(sc, "LongScramble");
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6: Batch of 30 Randomly Generated Scrambles
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Batch of 30 diverse random scrambles generated and solved")
    void testGeneratedScramblesBatch() {
        for (int i = 1; i <= 30; i++) {
            int length = (i % 20) + 1; // lengths from 1 to 20
            String scramble = ScrambleGenerator.generate(length);
            runFullScramblePipeline(scramble, "Batch#" + i + "(len=" + length + ")");
        }
    }

    // -------------------------------------------------------------------------
    // TEST 7: Inverse Scramble Verification
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Inverse scrambles test")
    void testInverseScrambles() {
        String scramble = "U R2 F B R B2 R U2 L B2 R U' D'";
        String inverse = CubeMoves.invertMoveSequence(scramble);

        // 1. Solve the original scramble
        runFullScramblePipeline(scramble, "InverseTest:Original");

        // 2. Solve the inverted scramble
        runFullScramblePipeline(inverse, "InverseTest:Inverted");

        // 3. Applying scramble followed by inverse returns immediately to solved
        CubeState cube = CubeMoves.applyMoves(new CubeState(), scramble);
        cube = CubeMoves.applyMoves(cube, inverse);
        assertTrue(cube.isSolved(), "Scramble followed by inverse must yield solved cube");
    }

    // -------------------------------------------------------------------------
    // TEST 8: Repeated Moves
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Repeated moves test")
    void testRepeatedMoves() {
        // "R R R" is equivalent to R'
        runFullScramblePipeline("R R R", "Repeated:R3");

        // "U2 U2 U" is equivalent to U
        runFullScramblePipeline("U2 U2 U", "Repeated:U5");

        // "F F F F" is identity -> cube is solved
        CubeState c1 = CubeMoves.applyMoves(new CubeState(), "F F F F");
        assertTrue(c1.isSolved(), "F applied 4 times must return to solved state");

        // "R2 R2" is identity -> cube is solved
        CubeState c2 = CubeMoves.applyMoves(new CubeState(), "R2 R2");
        assertTrue(c2.isSolved(), "R2 applied twice must return to solved state");

        // 6 repetitions of sexy move (R U R' U') returns to solved
        String sixSexy = "R U R' U' R U R' U' R U R' U' R U R' U' R U R' U' R U R' U'";
        CubeState c3 = CubeMoves.applyMoves(new CubeState(), sixSexy);
        assertTrue(c3.isSolved(), "6 sexy moves must return cube to solved state");
    }

    // -------------------------------------------------------------------------
    // TEST 9: Impossible Cube Configurations (Must Throw IllegalArgumentException)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Impossible cubes are rejected by validator and solver")
    void testImpossibleCubes() {
        // Case A: Single twisted corner (URF)
        int[] f1 = new CubeState().getFacelets();
        int tmp1 = f1[8]; f1[8] = f1[9]; f1[9] = f1[18+2]; f1[18+2] = tmp1;
        CubeState twistedCornerCube = new CubeState(f1);
        assertFalse(CubeValidator.validate(twistedCornerCube).valid);
        assertThrows(IllegalArgumentException.class, () -> solver.solve(twistedCornerCube));

        // Case B: Single flipped edge (UR)
        int[] f2 = new CubeState().getFacelets();
        int tmp2 = f2[5]; f2[5] = f2[10]; f2[10] = tmp2;
        CubeState flippedEdgeCube = new CubeState(f2);
        assertFalse(CubeValidator.validate(flippedEdgeCube).valid);
        assertThrows(IllegalArgumentException.class, () -> solver.solve(flippedEdgeCube));

        // Case C: Swapped pair of edges (odd permutation parity)
        int[] f3 = new CubeState().getFacelets();
        int tEdge1 = f3[5]; f3[5] = f3[3]; f3[3] = tEdge1;
        int tEdge2 = f3[10]; f3[10] = f3[37]; f3[37] = tEdge2;
        CubeState parityErrorCube = new CubeState(f3);
        assertFalse(CubeValidator.validate(parityErrorCube).valid);
        assertThrows(IllegalArgumentException.class, () -> solver.solve(parityErrorCube));

        // Case D: Wrong color counts (10 White, 8 Red)
        int[] f4 = new CubeState().getFacelets();
        f4[0] = 1; // Change White sticker to Red
        CubeState wrongColorCube = new CubeState(f4);
        assertFalse(CubeValidator.validate(wrongColorCube).valid);
        assertThrows(IllegalArgumentException.class, () -> solver.solve(wrongColorCube));

        // Case E: Center stickers swapped
        int[] f5 = new CubeState().getFacelets();
        int tCenter = f5[4]; f5[4] = f5[13]; f5[13] = tCenter;
        CubeState centerSwappedCube = new CubeState(f5);
        assertFalse(CubeValidator.validate(centerSwappedCube).valid);
        assertThrows(IllegalArgumentException.class, () -> solver.solve(centerSwappedCube));
    }

    // -------------------------------------------------------------------------
    // TEST 10: Malformed Input Handling
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Malformed inputs are rejected with expected exceptions")
    void testMalformedInput() {
        // Null array
        assertThrows(IllegalArgumentException.class, () -> new CubeState((int[]) null));

        // Short array (53 elements)
        assertThrows(IllegalArgumentException.class, () -> new CubeState(new int[53]));

        // Long array (55 elements)
        assertThrows(IllegalArgumentException.class, () -> new CubeState(new int[55]));

        // Invalid color values (-1, 99)
        int[] badColors = new CubeState().getFacelets();
        badColors[0] = -1;
        badColors[1] = 99;
        CubeState badState = new CubeState(badColors);
        assertFalse(CubeValidator.validate(badState).valid);
        assertThrows(IllegalArgumentException.class, () -> solver.solve(badState));

        // Invalid move strings in engine
        assertThrows(IllegalArgumentException.class, () -> CubeMoves.applyMove(new CubeState(), "X"));
        assertThrows(IllegalArgumentException.class, () -> CubeMoves.applyMove(new CubeState(), "U3"));
        assertThrows(IllegalArgumentException.class, () -> CubeMoves.applyMove(new CubeState(), "R_INVALID"));
        assertThrows(IllegalArgumentException.class, () -> CubeMoves.applyMove(new CubeState(), ""));
        assertThrows(IllegalArgumentException.class, () -> CubeMoves.applyMove(new CubeState(), null));
    }
}
