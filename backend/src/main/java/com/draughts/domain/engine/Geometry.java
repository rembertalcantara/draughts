package com.draughts.domain.engine;

import lombok.experimental.UtilityClass;
/**
 * Precomputed geometry of the 32 playable squares of an 8x8 board.
 *
 * <p>Squares are indexed 0-31 internally (square number minus one). Row 0 is Black's back row,
 * row 7 is White's. On even rows the dark squares are columns 1,3,5,7; on odd rows 0,2,4,6.
 */
@UtilityClass
class Geometry {

    static final int NW = 0;
    static final int NE = 1;
    static final int SW = 2;
    static final int SE = 3;

    /** Directions a black man may move (towards row 7). */
    static final int[] BLACK_FORWARD = {SW, SE};
    /** Directions a white man may move (towards row 0). */
    static final int[] WHITE_FORWARD = {NW, NE};
    static final int[] ALL = {NW, NE, SW, SE};

    private static final int[] DR = {-1, -1, 1, 1};
    private static final int[] DC = {-1, 1, -1, 1};

    /** {@code STEP[i][d]}: neighbour of square i in direction d, or -1. */
    static final int[][] STEP = new int[32][4];
    /** {@code JUMP[i][d]}: landing square two steps from i in direction d, or -1. */
    static final int[][] JUMP = new int[32][4];

    static final int BLACK_PROMOTION_ROW = 0xF000_0000; // squares 29-32
    static final int WHITE_PROMOTION_ROW = 0x0000_000F; // squares 1-4

    static {
        for (int i = 0; i < 32; i++) {
            int r = row(i);
            int c = col(i);
            for (int d = 0; d < 4; d++) {
                STEP[i][d] = index(r + DR[d], c + DC[d]);
                JUMP[i][d] = index(r + 2 * DR[d], c + 2 * DC[d]);
            }
        }
    }

    static int row(int index) {
        return index / 4;
    }

    static int col(int index) {
        int r = index / 4;
        int i = index % 4;
        return r % 2 == 0 ? 2 * i + 1 : 2 * i;
    }

    /** Index of the dark square at (row, col), or -1 when off-board or light. */
    static int index(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7 || (row + col) % 2 == 0) {
            return -1;
        }
        return row * 4 + col / 2;
    }

    static int[] forward(Color color) {
        return color == Color.BLACK ? BLACK_FORWARD : WHITE_FORWARD;
    }

    static int promotionRow(Color color) {
        return color == Color.BLACK ? BLACK_PROMOTION_ROW : WHITE_PROMOTION_ROW;
    }
}
