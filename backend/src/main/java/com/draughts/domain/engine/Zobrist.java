package com.draughts.domain.engine;

import lombok.experimental.UtilityClass;

import java.util.SplittableRandom;

/** Zobrist hashing of positions, used for repetition detection and AI transposition tables. */
@UtilityClass
public class Zobrist {

    private static final long[][] KEYS = new long[32][4];
    private static final long WHITE_TO_MOVE;

    static {
        var random = new SplittableRandom(0x5EED_D2A6_47L);
        for (int i = 0; i < 32; i++) {
            for (int k = 0; k < 4; k++) {
                KEYS[i][k] = random.nextLong();
            }
        }
        WHITE_TO_MOVE = random.nextLong();
    }

    public static long hash(Board board, Color toMove) {
        long h = toMove == Color.WHITE ? WHITE_TO_MOVE : 0L;
        int occupied = board.occupied();
        while (occupied != 0) {
            int i = Integer.numberOfTrailingZeros(occupied);
            occupied &= occupied - 1;
            int bit = 1 << i;
            int kind = ((board.white() & bit) != 0 ? 2 : 0) + ((board.kings() & bit) != 0 ? 1 : 0);
            h ^= KEYS[i][kind];
        }
        return h;
    }
}
