package com.draughts.ai;

import com.draughts.engine.Board;
import com.draughts.engine.Color;

/** Static evaluation in centipawn-like points, from the point of view of {@code color}. */
public final class Evaluator {

    static final int MAN = 100;
    static final int KING = 160;
    private static final int ADVANCE = 4;
    private static final int BACK_ROW_GUARD = 8;
    private static final int CENTER = 6;

    /** Squares 14,15,18,19 and their neighbours on rows 3-4, columns 2-5. */
    private static final int CENTER_MASK = (1 << 13) | (1 << 14) | (1 << 17) | (1 << 18)
            | (1 << 12) | (1 << 15) | (1 << 16) | (1 << 19);
    private static final int BLACK_BACK_ROW = 0x0000_000F;
    private static final int WHITE_BACK_ROW = 0xF000_0000;

    private Evaluator() {
    }

    public static int evaluate(Board board, Color color) {
        return side(board, color) - side(board, color.opposite());
    }

    private static int side(Board board, Color color) {
        int own = board.pieces(color);
        int kings = own & board.kings();
        int men = own & ~kings;
        int score = Integer.bitCount(men) * MAN + Integer.bitCount(kings) * KING;
        int m = men;
        while (m != 0) {
            int i = Integer.numberOfTrailingZeros(m);
            m &= m - 1;
            int row = i / 4;
            score += ADVANCE * (color == Color.BLACK ? row : 7 - row);
        }
        score += BACK_ROW_GUARD * Integer.bitCount(men & (color == Color.BLACK ? BLACK_BACK_ROW : WHITE_BACK_ROW));
        score += CENTER * Integer.bitCount(own & CENTER_MASK);
        return score;
    }
}
