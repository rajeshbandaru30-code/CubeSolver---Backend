package com.cubesolve.engine;

import java.util.*;

/**
 * Kociemba Two-Phase Algorithm implementation for the 3x3 Rubik's Cube.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ALGORITHM OVERVIEW
 * ─────────────────────────────────────────────────────────────────────────────
 * The Kociemba Two-Phase Algorithm (1992) reduces solving to two IDA* searches:
 *
 *   Phase 1 — Reduce to subgroup G1 = <U,D,R2,L2,F2,B2>
 *     Goal: Achieve EO=0 (edge orientation), CO=0 (corner orientation),
 *           Slice=494 (slice edges in their slice)
 *     Coordinates: EO (0–2047), CO (0–2186), Slice (0–494)
 *     Pruning table: EO × CO  (2048 × 2187 = ~4.5M entries)
 *                    EO × Slice (2048 × 495 = ~1M entries)
 *
 *   Phase 2 — Solve within G1 using only <U,D,R2,L2,F2,B2>
 *     Goal: Achieve CP=0, SP=0, EP=0
 *     Coordinates: CP (0–40319), SP (0–23)
 *     Pruning table: CP × SP only (40320 × 24 = ~1M entries)
 *     EP searched without precomputed table (Phase 2 depth is ≤ 18)
 *
 * Memory usage: ~6MB total for all tables.
 *
 * References:
 *   Kociemba, H. (1992). "Close to God's Algorithm." Cubism For Fun, 28, 10–13.
 *   https://kociemba.org/cube.htm
 */
public class KociembaSolver {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final int N_MOVES_P1  = 18;  // all 18 moves in Phase 1
    private static final int N_MOVES_P2  = 10;  // G1 moves in Phase 2
    // G1 move indices into CubeMoves.ALL_MOVES:
    // U=0,U2=1,U'=2,D=3,D2=4,D'=5,R2=7,L2=10,F2=13,B2=16
    private static final int[] P2_MOVE_INDICES = {0, 1, 2, 3, 4, 5, 7, 10, 13, 16};

    private static final int N_EO    = 2048;
    private static final int N_CO    = 2187;
    private static final int N_SLICE = 495;
    private static final int N_CP    = 40320;
    private static final int N_SP    = 24;
    private static final int N_EP    = 40320;

    // All 18 move names (same order as CubeMoves.ALL_MOVES)
    private static final String[] MOVES = CubeMoves.ALL_MOVES;

    // -----------------------------------------------------------------------
    // Move tables (phase1 coordinates) - static so shared across instances
    // -----------------------------------------------------------------------

    private static int[][] eoMove;     // [N_EO][N_MOVES_P1]
    private static int[][] coMove;     // [N_CO][N_MOVES_P1]
    private static int[][] sliceMove;  // [N_SLICE][N_MOVES_P1]

    // Move tables (phase2 coordinates, all 18 moves for incremental tracking in P1)
    private static int[][] cpMove;     // [N_CP][N_MOVES_P1]
    private static int[][] spMove;     // [N_SP][N_MOVES_P1]
    private static int[][] epMove;     // [N_EP][N_MOVES_P1]

    // Pruning tables
    private static byte[] pruneP1_EO_CO;    // [N_EO * N_CO]
    private static byte[] pruneP1_EO_Slice; // [N_EO * N_SLICE]
    private static byte[] pruneP1_CO_Slice; // [N_CO * N_SLICE]
    private static byte[] pruneP2_CP_SP;    // [N_CP * N_SP]
    private static byte[] pruneP2_EP_SP;    // [N_EP * N_SP]

    private static volatile boolean initialised = false;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Solves the given cube. Returns the solution as a space-separated move string.
     * Returns empty string if already solved.
     *
     * @param cube the scrambled cube
     * @return solution move string (e.g. "U R2 F' B R")
     * @throws IllegalArgumentException if the cube is invalid
     */
    public String solve(CubeState cube) {
        CubeValidator.validateOrThrow(cube);
        if (cube.isSolved()) return "";

        if (!initialised) {
            synchronized (KociembaSolver.class) {
                if (!initialised) {
                    initTables();
                    initialised = true;
                }
            }
        }

        return twoPhaseSearch(cube);
    }

    // -----------------------------------------------------------------------
    // Table Initialisation
    // -----------------------------------------------------------------------

    private void initTables() {
        long t0 = System.currentTimeMillis();
        // --- Phase 1 move tables ---
        eoMove    = new int[N_EO][N_MOVES_P1];
        coMove    = new int[N_CO][N_MOVES_P1];
        sliceMove = new int[N_SLICE][N_MOVES_P1];

        for (int eo = 0; eo < N_EO; eo++) {
            CubeState c = buildForEO(eo);
            for (int m = 0; m < N_MOVES_P1; m++) {
                eoMove[eo][m] = computeEO(CubeMoves.applyMove(c, MOVES[m]));
            }
        }

        for (int co = 0; co < N_CO; co++) {
            CubeState c = buildForCO(co);
            for (int m = 0; m < N_MOVES_P1; m++) {
                coMove[co][m] = computeCO(CubeMoves.applyMove(c, MOVES[m]));
            }
        }

        for (int sl = 0; sl < N_SLICE; sl++) {
            CubeState c = buildForSlice(sl);
            for (int m = 0; m < N_MOVES_P1; m++) {
                sliceMove[sl][m] = computeSlice(CubeMoves.applyMove(c, MOVES[m]));
            }
        }
        System.out.println("P1 move tables built in " + (System.currentTimeMillis() - t0) + " ms");
        long t1 = System.currentTimeMillis();

        // --- Phase 2 / Full move tables for CP, SP, EP ---
        cpMove = new int[N_CP][N_MOVES_P1];
        spMove = new int[N_SP][N_MOVES_P1];
        epMove = new int[N_EP][N_MOVES_P1];

        for (int cp = 0; cp < N_CP; cp++) {
            CubeState c = buildForCP(cp);
            for (int m = 0; m < N_MOVES_P1; m++) {
                cpMove[cp][m] = computeCP(CubeMoves.applyMove(c, MOVES[m]));
            }
        }

        for (int sp = 0; sp < N_SP; sp++) {
            CubeState c = buildForSP(sp);
            for (int m = 0; m < N_MOVES_P1; m++) {
                spMove[sp][m] = computeSP(CubeMoves.applyMove(c, MOVES[m]));
            }
        }

        for (int ep = 0; ep < N_EP; ep++) {
            CubeState c = buildForEP(ep);
            for (int m = 0; m < N_MOVES_P1; m++) {
                epMove[ep][m] = computeEP(CubeMoves.applyMove(c, MOVES[m]));
            }
        }
        System.out.println("CP/SP/EP move tables built in " + (System.currentTimeMillis() - t1) + " ms");
        long t2 = System.currentTimeMillis();

        // --- Phase 1 Pruning tables (BFS from goal) ---
        int goalSlice = computeSlice(new CubeState());

        pruneP1_EO_CO = new byte[N_EO * N_CO];
        Arrays.fill(pruneP1_EO_CO, (byte) -1);
        pruneP1_EO_CO[0 * N_CO + 0] = 0;
        bfsFill2D(pruneP1_EO_CO, N_EO, N_CO, eoMove, coMove, N_MOVES_P1);
        System.out.println("Prune P1_EO_CO built in " + (System.currentTimeMillis() - t2) + " ms");
        long t3 = System.currentTimeMillis();

        pruneP1_EO_Slice = new byte[N_EO * N_SLICE];
        Arrays.fill(pruneP1_EO_Slice, (byte) -1);
        pruneP1_EO_Slice[0 * N_SLICE + goalSlice] = 0;
        bfsFill2D(pruneP1_EO_Slice, N_EO, N_SLICE, eoMove, sliceMove, N_MOVES_P1);
        System.out.println("Prune P1_EO_Slice built in " + (System.currentTimeMillis() - t3) + " ms");
        long t4 = System.currentTimeMillis();

        pruneP1_CO_Slice = new byte[N_CO * N_SLICE];
        Arrays.fill(pruneP1_CO_Slice, (byte) -1);
        pruneP1_CO_Slice[0 * N_SLICE + goalSlice] = 0;
        bfsFill2D(pruneP1_CO_Slice, N_CO, N_SLICE, coMove, sliceMove, N_MOVES_P1);
        System.out.println("Prune P1_CO_Slice built in " + (System.currentTimeMillis() - t4) + " ms");
        long t5 = System.currentTimeMillis();

        // --- Phase 2 Pruning tables (using P2 moves) ---
        int goalSP = computeSP(new CubeState());

        // Extract P2 sub-tables for BFS building
        int[][] cpMoveP2 = extractP2MoveTable(cpMove);
        int[][] spMoveP2 = extractP2MoveTable(spMove);
        int[][] epMoveP2 = extractP2MoveTable(epMove);

        pruneP2_CP_SP = new byte[N_CP * N_SP];
        Arrays.fill(pruneP2_CP_SP, (byte) -1);
        pruneP2_CP_SP[0 * N_SP + goalSP] = 0;
        bfsFill2D(pruneP2_CP_SP, N_CP, N_SP, cpMoveP2, spMoveP2, N_MOVES_P2);
        System.out.println("Prune P2_CP_SP built in " + (System.currentTimeMillis() - t5) + " ms");
        long t6 = System.currentTimeMillis();

        pruneP2_EP_SP = new byte[N_EP * N_SP];
        Arrays.fill(pruneP2_EP_SP, (byte) -1);
        pruneP2_EP_SP[0 * N_SP + goalSP] = 0;
        bfsFill2D(pruneP2_EP_SP, N_EP, N_SP, epMoveP2, spMoveP2, N_MOVES_P2);
        System.out.println("Prune P2_EP_SP built in " + (System.currentTimeMillis() - t6) + " ms");

        fixup(pruneP1_EO_CO);
        fixup(pruneP1_EO_Slice);
        fixup(pruneP1_CO_Slice);
        fixup(pruneP2_CP_SP);
        fixup(pruneP2_EP_SP);
        System.out.println("Total initTables finished in " + (System.currentTimeMillis() - t0) + " ms");
    }

    private static int[][] extractP2MoveTable(int[][] fullTable) {
        int n = fullTable.length;
        int[][] p2Table = new int[n][N_MOVES_P2];
        for (int i = 0; i < n; i++) {
            for (int m = 0; m < N_MOVES_P2; m++) {
                p2Table[i][m] = fullTable[i][P2_MOVE_INDICES[m]];
            }
        }
        return p2Table;
    }

    /** BFS to fill a 2D pruning table indexed [A * nB + B]. */
    private static void bfsFill2D(byte[] table, int nA, int nB,
                                  int[][] moveA, int[][] moveB, int nMoves) {
        int[] q = new int[table.length];
        int head = 0, tail = 0;
        for (int i = 0; i < table.length; i++) {
            if (table[i] == 0) {
                q[tail++] = i;
            }
        }
        while (head < tail) {
            int idx = q[head++];
            int a = idx / nB;
            int b = idx % nB;
            byte d = table[idx];
            for (int m = 0; m < nMoves; m++) {
                int na = moveA[a][m];
                int nb = moveB[b][m];
                int nidx = na * nB + nb;
                if (table[nidx] == -1) {
                    table[nidx] = (byte) (d + 1);
                    q[tail++] = nidx;
                }
            }
        }
    }

    private void fixup(byte[] table) {
        for (int i = 0; i < table.length; i++) {
            if (table[i] == -1) table[i] = 20;
        }
    }

    // -----------------------------------------------------------------------
    // Two-Phase IDA* Search
    // -----------------------------------------------------------------------

    /**
     * Main search routine. Incremental coordinate tracking in Phase 1.
     */
    private String twoPhaseSearch(CubeState cube) {
        CubeState solved = new CubeState();
        int goalSlice = computeSlice(solved);   // = 494 for solved cube
        int goalCP    = computeCP(solved);      // = 0
        int goalSP    = computeSP(solved);      // = 0
        int goalEP    = computeEP(solved);      // = 0

        int[] moves   = new int[25]; // enough for any solution
        int[] p2Moves = new int[20];

        int eo = computeEO(cube);
        int co = computeCO(cube);
        int sl = computeSlice(cube);

        int lb1 = pruneP1_EO_CO[eo * N_CO + co];
        int lb2 = pruneP1_EO_Slice[eo * N_SLICE + sl];
        int lb3 = pruneP1_CO_Slice[co * N_SLICE + sl];
        int minD1 = Math.max(lb1, Math.max(lb2, lb3));

        for (int maxD1 = minD1; maxD1 <= 20; maxD1++) {
            int total = idaP1(cube, eo, co, sl, 0, maxD1, moves, -1,
                              goalSlice, goalCP, goalSP, goalEP, p2Moves);
            if (total >= 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < total; i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(MOVES[moves[i]]);
                }
                return sb.toString().trim();
            }
        }
        throw new RuntimeException("No solution found within search depth limit.");
    }

    /**
     * Phase 1 IDA* — reduces cube to G1 subgroup.
     * EO, CO, and Slice coordinates are updated incrementally using move tables.
     * When G1 goal is reached, the exact CubeState is reconstructed to compute CP, SP, EP.
     */
    private int idaP1(CubeState initialCube,
                      int eo, int co, int sl,
                      int depth, int maxDepth, int[] moves, int lastFace,
                      int goalSlice, int goalCP, int goalSP, int goalEP,
                      int[] p2Moves) {

        // Phase 1 goal: EO=0, CO=0, Slice=goalSlice
        if (eo == 0 && co == 0 && sl == goalSlice) {
            CubeState currentCube = initialCube;
            for (int i = 0; i < depth; i++) {
                currentCube = CubeMoves.applyMove(currentCube, MOVES[moves[i]]);
            }
            int cp = computeCP(currentCube);
            int sp = computeSP(currentCube);
            int ep = computeEP(currentCube);

            int lbCP = pruneP2_CP_SP[cp * N_SP + sp];
            int lbEP = pruneP2_EP_SP[ep * N_SP + sp];
            int minP2 = Math.max(lbCP, lbEP);
            int maxP2 = 12;

            if (minP2 <= maxP2) {
                for (int maxD2 = minP2; maxD2 <= maxP2; maxD2++) {
                    int p2Len = idaP2(cp, sp, ep, 0, maxD2, p2Moves, -1,
                                      goalCP, goalSP, goalEP);
                    if (p2Len >= 0) {
                        for (int i = 0; i < p2Len; i++) {
                            moves[depth + i] = P2_MOVE_INDICES[p2Moves[i]];
                        }
                        return depth + p2Len;
                    }
                }
            }
            return -1;
        }

        if (depth == maxDepth) return -1;

        int lb1 = pruneP1_EO_CO[eo * N_CO + co];
        int lb2 = pruneP1_EO_Slice[eo * N_SLICE + sl];
        int lb3 = pruneP1_CO_Slice[co * N_SLICE + sl];
        if (Math.max(lb1, Math.max(lb2, lb3)) > maxDepth - depth) return -1;

        for (int m = 0; m < N_MOVES_P1; m++) {
            int face = m / 3;
            if (face == lastFace) continue;
            if (lastFace >= 0 && face == (lastFace ^ 1) && face > lastFace) continue;

            int nEO = eoMove[eo][m];
            int nCO = coMove[co][m];
            int nSl = sliceMove[sl][m];

            moves[depth] = m;
            int result = idaP1(initialCube, nEO, nCO, nSl, depth + 1, maxDepth, moves, face,
                               goalSlice, goalCP, goalSP, goalEP, p2Moves);
            if (result >= 0) return result;
        }
        return -1;
    }

    /**
     * Phase 2 IDA* — solves within G1 using only {U,U2,U',D,D2,D',R2,L2,F2,B2}.
     * Uses precomputed CP and SP move/pruning tables plus EP move table.
     */
    private int idaP2(int cp, int sp, int ep,
                      int depth, int maxDepth, int[] p2Moves, int lastFace,
                      int goalCP, int goalSP, int goalEP) {
        if (cp == goalCP && sp == goalSP && ep == goalEP) return depth;
        if (depth == maxDepth) return -1;

        int lb1 = pruneP2_CP_SP[cp * N_SP + sp];
        int lb2 = pruneP2_EP_SP[ep * N_SP + sp];
        if (Math.max(lb1, lb2) > maxDepth - depth) return -1;

        for (int m = 0; m < N_MOVES_P2; m++) {
            int globalIdx = P2_MOVE_INDICES[m];
            int face = globalIdx / 3;
            if (face == lastFace) continue;
            if (lastFace >= 0 && face == (lastFace ^ 1) && face > lastFace) continue;

            int nCP = cpMove[cp][globalIdx];
            int nSP = spMove[sp][globalIdx];
            int nEP = epMove[ep][globalIdx];

            p2Moves[depth] = m;
            int result = idaP2(nCP, nSP, nEP, depth + 1, maxDepth, p2Moves, face,
                               goalCP, goalSP, goalEP);
            if (result >= 0) return result;
        }
        return -1;
    }

    // -----------------------------------------------------------------------
    // Coordinate Computation (from CubeState)
    // -----------------------------------------------------------------------

    // Edge facelet positions [edge_index][0=first sticker, 1=second sticker]
    private static final int[][] EDGE_FACELETS = {
        {CubeState.U*9+5, CubeState.R*9+1},  // 0=UR
        {CubeState.U*9+7, CubeState.F*9+1},  // 1=UF
        {CubeState.U*9+3, CubeState.L*9+1},  // 2=UL
        {CubeState.U*9+1, CubeState.B*9+1},  // 3=UB
        {CubeState.D*9+5, CubeState.R*9+7},  // 4=DR
        {CubeState.D*9+1, CubeState.F*9+7},  // 5=DF
        {CubeState.D*9+3, CubeState.L*9+7},  // 6=DL
        {CubeState.D*9+7, CubeState.B*9+7},  // 7=DB
        {CubeState.F*9+5, CubeState.R*9+3},  // 8=FR (slice)
        {CubeState.F*9+3, CubeState.L*9+5},  // 9=FL (slice)
        {CubeState.B*9+5, CubeState.L*9+3},  // 10=BL (slice)
        {CubeState.B*9+3, CubeState.R*9+5},  // 11=BR (slice)
    };

    // Home colors for each edge [edge_index][0,1]
    private static final int[][] EDGE_COLORS = {
        {CubeState.U, CubeState.R}, {CubeState.U, CubeState.F},
        {CubeState.U, CubeState.L}, {CubeState.U, CubeState.B},
        {CubeState.D, CubeState.R}, {CubeState.D, CubeState.F},
        {CubeState.D, CubeState.L}, {CubeState.D, CubeState.B},
        {CubeState.F, CubeState.R}, {CubeState.F, CubeState.L},
        {CubeState.B, CubeState.L}, {CubeState.B, CubeState.R},
    };

    private static final int[][] CORNER_FACELETS = {
        {CubeState.U*9+8, CubeState.R*9+0, CubeState.F*9+2},  // 0=URF
        {CubeState.U*9+6, CubeState.F*9+0, CubeState.L*9+2},  // 1=UFL
        {CubeState.U*9+0, CubeState.L*9+0, CubeState.B*9+2},  // 2=ULB
        {CubeState.U*9+2, CubeState.B*9+0, CubeState.R*9+2},  // 3=UBR
        {CubeState.D*9+2, CubeState.F*9+8, CubeState.R*9+6},  // 4=DFR
        {CubeState.D*9+0, CubeState.L*9+8, CubeState.F*9+6},  // 5=DLF
        {CubeState.D*9+6, CubeState.B*9+8, CubeState.L*9+6},  // 6=DBL
        {CubeState.D*9+8, CubeState.R*9+8, CubeState.B*9+6},  // 7=DRB
    };

    private static final int[][] CORNER_COLORS = {
        {CubeState.U, CubeState.R, CubeState.F},
        {CubeState.U, CubeState.F, CubeState.L},
        {CubeState.U, CubeState.L, CubeState.B},
        {CubeState.U, CubeState.B, CubeState.R},
        {CubeState.D, CubeState.F, CubeState.R},
        {CubeState.D, CubeState.L, CubeState.F},
        {CubeState.D, CubeState.B, CubeState.L},
        {CubeState.D, CubeState.R, CubeState.B},
    };

    /**
     * Edge Orientation coordinate (0–2047).
     * Edge is "bad" (ori=1) if a U/D-coloured sticker is on an F/B face slot,
     * or an F/B-coloured sticker is on a U/D face slot.
     */
    static int computeEO(CubeState cube) {
        int[] f = cube.getFacelets();
        int eo = 0;
        for (int i = 0; i < 11; i++) {
            int c0 = f[EDGE_FACELETS[i][0]];
            int c1 = f[EDGE_FACELETS[i][1]];
            int ori = isEdgeBad(c0, c1, i) ? 1 : 0;
            eo = eo * 2 + ori;
        }
        return eo;
    }

    /**
     * Determines if an edge is "bad" (orientation = 1) in the Kociemba sense.
     *
     * Kociemba edge orientation rule (the simplest correct formulation):
     * An edge is bad if its stickers violate the "UD symmetry axis" rule:
     *   - A U/D colored sticker is on a F/B face slot, OR
     *   - A F/B colored sticker is on a U/D face slot.
     *
     * In other words, an edge is GOOD if:
     *   - U/D colored sticker is on U/D or R/L face slot (never on F/B), AND
     *   - F/B colored sticker is on F/B or R/L face slot (never on U/D).
     *
     * @param c0 color of sticker on the first facelet position (U/D/F/B principal face)
     * @param c1 color of sticker on the second facelet position (R/L or U/D secondary face)
     * @param edgeIndex 0–11, used to determine if the edge is UD-layer or equatorial
     */
    private static boolean isEdgeBad(int c0, int c1, int edgeIndex) {
        // Check if either sticker violates the UD-symmetry rule:
        // U/D color appearing on F or B face slot → bad
        // F/B color appearing on U or D face slot → bad
        boolean c0isUD = c0 == CubeState.U || c0 == CubeState.D;
        boolean c0isFB = c0 == CubeState.F || c0 == CubeState.B;
        boolean c1isUD = c1 == CubeState.U || c1 == CubeState.D;
        boolean c1isFB = c1 == CubeState.F || c1 == CubeState.B;

        if (edgeIndex < 8) {
            // UD-layer edges: first slot is on U/D face.
            // Edge is bad if the sticker on the U/D slot is F or B colored.
            return c0isFB || c1isUD;
        } else {
            // Equatorial (slice) edges: first slot is on F/B face.
            // Edge is bad if the sticker on the F/B slot is U or D colored.
            return c0isUD || c1isFB;
        }
    }

    /** Corner Orientation coordinate (0–2186). */
    static int computeCO(CubeState cube) {
        int[] f = cube.getFacelets();
        int co = 0;
        for (int i = 0; i < 7; i++) {
            int c0 = f[CORNER_FACELETS[i][0]];
            int c1 = f[CORNER_FACELETS[i][1]];
            int ori;
            if (c0 == CubeState.U || c0 == CubeState.D) ori = 0;
            else if (c1 == CubeState.U || c1 == CubeState.D) ori = 1;
            else ori = 2;
            co = co * 3 + ori;
        }
        return co;
    }

    /**
     * UD-Slice coordinate (0–494).
     * Encodes which of the 12 edge slots contain the 4 UD-slice edges (FR,FL,BL,BR).
     * Uses the combinatorial number system: C(p3,4)+C(p2,3)+C(p1,2)+C(p0,1).
     * Solved state (slice edges in positions 8,9,10,11) = C(11,4)+C(10,3)+C(9,2)+C(8,1) = 494.
     */
    static int computeSlice(CubeState cube) {
        int[] f = cube.getFacelets();
        boolean[] isSlice = new boolean[12];
        for (int i = 0; i < 12; i++) {
            int c0 = f[EDGE_FACELETS[i][0]];
            int c1 = f[EDGE_FACELETS[i][1]];
            isSlice[i] = (c0 != CubeState.U && c0 != CubeState.D
                       && c1 != CubeState.U && c1 != CubeState.D);
        }
        int[] pos = new int[4];
        int found = 0;
        for (int i = 0; i < 12 && found < 4; i++) {
            if (isSlice[i]) pos[found++] = i;
        }
        if (found < 4) return 0;
        return C[pos[3]][4] + C[pos[2]][3] + C[pos[1]][2] + C[pos[0]][1];
    }

    /** Corner Permutation coordinate (0–40319) — Lehmer code of 8 corners. */
    static int computeCP(CubeState cube) {
        int[] f = cube.getFacelets();
        int[] perm = new int[8];
        for (int i = 0; i < 8; i++) {
            int c0 = f[CORNER_FACELETS[i][0]];
            int c1 = f[CORNER_FACELETS[i][1]];
            int c2 = f[CORNER_FACELETS[i][2]];
            outer:
            for (int j = 0; j < 8; j++) {
                for (int o = 0; o < 3; o++) {
                    if (c0 == CORNER_COLORS[j][o % 3]
                     && c1 == CORNER_COLORS[j][(o+1) % 3]
                     && c2 == CORNER_COLORS[j][(o+2) % 3]) {
                        perm[i] = j;
                        break outer;
                    }
                }
            }
        }
        return lehmer(perm, 8);
    }

    /**
     * UD-Slice edge permutation (0–23).
     * Permutation of the 4 slice edges {FR,FL,BL,BR} among their 4 home slots.
     */
    static int computeSP(CubeState cube) {
        int[] f = cube.getFacelets();
        // Home colors: FR(0)=F+R, FL(1)=F+L, BL(2)=B+L, BR(3)=B+R
        int[][] homeColors = {
            {CubeState.F, CubeState.R}, {CubeState.F, CubeState.L},
            {CubeState.B, CubeState.L}, {CubeState.B, CubeState.R},
        };
        int[] perm = new int[4];
        for (int i = 0; i < 4; i++) {
            int c0 = f[EDGE_FACELETS[8 + i][0]];
            int c1 = f[EDGE_FACELETS[8 + i][1]];
            for (int j = 0; j < 4; j++) {
                if ((c0 == homeColors[j][0] && c1 == homeColors[j][1])
                 || (c0 == homeColors[j][1] && c1 == homeColors[j][0])) {
                    perm[i] = j;
                    break;
                }
            }
        }
        return lehmer(perm, 4);
    }

    /**
     * Non-slice (UD) edge permutation (0–40319).
     * Lehmer code of the 8 non-slice edges (UR,UF,UL,UB,DR,DF,DL,DB).
     */
    static int computeEP(CubeState cube) {
        int[] f = cube.getFacelets();
        int[] perm = new int[8];
        for (int i = 0; i < 8; i++) {
            int c0 = f[EDGE_FACELETS[i][0]];
            int c1 = f[EDGE_FACELETS[i][1]];
            for (int j = 0; j < 8; j++) {
                if ((c0 == EDGE_COLORS[j][0] && c1 == EDGE_COLORS[j][1])
                 || (c0 == EDGE_COLORS[j][1] && c1 == EDGE_COLORS[j][0])) {
                    perm[i] = j;
                    break;
                }
            }
        }
        return lehmer(perm, 8);
    }

    // -----------------------------------------------------------------------
    // Cube builders — build a representative CubeState for a given coordinate
    // (Used for move table generation only)
    // -----------------------------------------------------------------------

    private static CubeState buildForEO(int eo) {
        // Start from solved; apply edge orientations
        int[] f = solvedFacelets();
        int[] oris = new int[12];
        int rem = eo, sum = 0;
        for (int i = 10; i >= 0; i--) {
            oris[i] = rem % 2; sum += oris[i]; rem /= 2;
        }
        oris[11] = sum % 2;
        for (int i = 0; i < 12; i++) {
            f[EDGE_FACELETS[i][0]] = EDGE_COLORS[i][oris[i] % 2];
            f[EDGE_FACELETS[i][1]] = EDGE_COLORS[i][(oris[i] + 1) % 2];
        }
        return new CubeState(f);
    }

    private static CubeState buildForCO(int co) {
        int[] f = solvedFacelets();
        int[] oris = new int[8];
        int rem = co, sum = 0;
        for (int i = 6; i >= 0; i--) {
            oris[i] = rem % 3; sum += oris[i]; rem /= 3;
        }
        oris[7] = (3 - sum % 3) % 3;
        for (int i = 0; i < 8; i++) {
            int o = oris[i];
            f[CORNER_FACELETS[i][0]] = CORNER_COLORS[i][(0 - o + 3) % 3];
            f[CORNER_FACELETS[i][1]] = CORNER_COLORS[i][(1 - o + 3) % 3];
            f[CORNER_FACELETS[i][2]] = CORNER_COLORS[i][(2 - o + 3) % 3];
        }
        return new CubeState(f);
    }

    private static CubeState buildForSlice(int sl) {
        // Decode sl to 4 positions of slice edges using combinatorial number system
        int[] pos = decodeComb(sl, 4, 12);
        int[] f = solvedFacelets();
        // Clear all edge positions first to a dummy non-slice color
        for (int i = 0; i < 8; i++) {
            f[EDGE_FACELETS[i][0]] = EDGE_COLORS[i][0];
            f[EDGE_FACELETS[i][1]] = EDGE_COLORS[i][1];
        }
        // Place slice edges at the decoded positions (identity: FR at pos[0], FL at pos[1], etc.)
        for (int k = 0; k < 4; k++) {
            int p = pos[k];
            f[EDGE_FACELETS[p][0]] = EDGE_COLORS[8 + k][0]; // FR,FL,BL,BR colors
            f[EDGE_FACELETS[p][1]] = EDGE_COLORS[8 + k][1];
        }
        // Remaining (non-slice) edges get their UD colors
        int udIdx = 0;
        for (int i = 0; i < 12; i++) {
            boolean isSlicePos = false;
            for (int k = 0; k < 4; k++) if (pos[k] == i) { isSlicePos = true; break; }
            if (!isSlicePos && udIdx < 8) {
                f[EDGE_FACELETS[i][0]] = EDGE_COLORS[udIdx][0];
                f[EDGE_FACELETS[i][1]] = EDGE_COLORS[udIdx][1];
                udIdx++;
            }
        }
        return new CubeState(f);
    }

    private static CubeState buildForCP(int cp) {
        int[] f = solvedFacelets();
        int[] perm = decodeLehmer(cp, 8);
        // Place corner cubie perm[i] into position i
        int[][] tmp = new int[8][3];
        for (int i = 0; i < 8; i++) {
            tmp[i][0] = CORNER_COLORS[perm[i]][0];
            tmp[i][1] = CORNER_COLORS[perm[i]][1];
            tmp[i][2] = CORNER_COLORS[perm[i]][2];
        }
        for (int i = 0; i < 8; i++) {
            f[CORNER_FACELETS[i][0]] = tmp[i][0];
            f[CORNER_FACELETS[i][1]] = tmp[i][1];
            f[CORNER_FACELETS[i][2]] = tmp[i][2];
        }
        return new CubeState(f);
    }

    private static CubeState buildForSP(int sp) {
        int[] f = solvedFacelets();
        int[] perm = decodeLehmer(sp, 4);
        for (int i = 0; i < 4; i++) {
            // Slice edge home colors: FR=F+R, FL=F+L, BL=B+L, BR=B+R
            int[][] homeColors = {
                {CubeState.F, CubeState.R}, {CubeState.F, CubeState.L},
                {CubeState.B, CubeState.L}, {CubeState.B, CubeState.R},
            };
            f[EDGE_FACELETS[8 + i][0]] = homeColors[perm[i]][0];
            f[EDGE_FACELETS[8 + i][1]] = homeColors[perm[i]][1];
        }
        return new CubeState(f);
    }

    private static CubeState buildForEP(int ep) {
        int[] f = solvedFacelets();
        int[] perm = decodeLehmer(ep, 8);
        for (int i = 0; i < 8; i++) {
            f[EDGE_FACELETS[i][0]] = EDGE_COLORS[perm[i]][0];
            f[EDGE_FACELETS[i][1]] = EDGE_COLORS[perm[i]][1];
        }
        return new CubeState(f);
    }

    private static int[] solvedFacelets() {
        int[] f = new int[54];
        for (int face = 0; face < 6; face++) {
            for (int pos = 0; pos < 9; pos++) {
                f[face * 9 + pos] = face;
            }
        }
        return f;
    }

    /** Rebuild a CubeState from all 6 coordinates for mid-search use. */
    private CubeState rebuildCubeState(int eo, int co, int sl, int cp, int sp, int ep) {
        // Apply coordinates progressively onto a solved cube
        // This is an approximation for getting P2 coords during P1 search;
        // the full rebuild is expensive but only called for non-P2 moves.
        CubeState c = buildForCP(cp);
        // Apply EP on top
        int[] f = c.getFacelets();
        int[] epPerm = decodeLehmer(ep, 8);
        int[][] tmp = new int[8][2];
        for (int i = 0; i < 8; i++) {
            tmp[i][0] = EDGE_COLORS[epPerm[i]][0];
            tmp[i][1] = EDGE_COLORS[epPerm[i]][1];
        }
        for (int i = 0; i < 8; i++) {
            f[EDGE_FACELETS[i][0]] = tmp[i][0];
            f[EDGE_FACELETS[i][1]] = tmp[i][1];
        }
        // Apply SP on top (slice edges)
        int[][] homeColors = {
            {CubeState.F, CubeState.R}, {CubeState.F, CubeState.L},
            {CubeState.B, CubeState.L}, {CubeState.B, CubeState.R},
        };
        int[] spPerm = decodeLehmer(sp, 4);
        for (int i = 0; i < 4; i++) {
            f[EDGE_FACELETS[8 + i][0]] = homeColors[spPerm[i]][0];
            f[EDGE_FACELETS[8 + i][1]] = homeColors[spPerm[i]][1];
        }
        return new CubeState(f);
    }

    // -----------------------------------------------------------------------
    // Combinatorics
    // -----------------------------------------------------------------------

    private static final int[][] C = new int[13][13];
    static {
        for (int n = 0; n <= 12; n++) {
            C[n][0] = 1;
            for (int k = 1; k <= n; k++) {
                C[n][k] = C[n-1][k-1] + C[n-1][k];
            }
        }
    }

    private static int lehmer(int[] perm, int n) {
        int code = 0;
        for (int i = 0; i < n - 1; i++) {
            int count = 0;
            for (int j = i + 1; j < n; j++) {
                if (perm[j] < perm[i]) count++;
            }
            code = code * (n - i) + count;
        }
        return code;
    }

    private static int[] decodeLehmer(int code, int n) {
        int[] perm = new int[n];
        int[] avail = new int[n];
        for (int i = 0; i < n; i++) avail[i] = i;
        int[] digits = new int[n];
        for (int i = n - 1; i >= 0; i--) {
            digits[i] = code % (n - i);
            code /= (n - i);
        }
        for (int i = 0; i < n; i++) {
            perm[i] = avail[digits[i]];
            System.arraycopy(avail, digits[i] + 1, avail, digits[i], n - i - 1 - digits[i]);
        }
        return perm;
    }

    /**
     * Decode a combinatorial number system value into sorted positions.
     * Given coord = C(p[r-1],r) + C(p[r-2],r-1) + ... + C(p[0],1),
     * recover p[0] < p[1] < ... < p[r-1] with p[i] < n.
     */
    private static int[] decodeComb(int coord, int r, int n) {
        int[] pos = new int[r];
        for (int k = r; k > 0; k--) {
            // Find largest p such that C(p,k) <= coord
            int p = k - 1;
            while (p < n && C[p][k] <= coord) p++;
            p--;
            pos[k - 1] = p;
            coord -= C[p][k];
        }
        return pos;
    }
}
