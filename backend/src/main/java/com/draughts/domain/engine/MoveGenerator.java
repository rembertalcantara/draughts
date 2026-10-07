package com.draughts.domain.engine;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Legal move generation for English draughts. Captures are mandatory: when any capture exists only
 * capturing moves are returned, and every capture is played to the end of its jump sequence.
 */
@UtilityClass
public class MoveGenerator {

    public static List<Move> legalMoves(Board board, Color color) {
        var captures = captures(board, color);
        return captures.isEmpty() ? simpleMoves(board, color) : captures;
    }

    public static List<Move> captures(Board board, Color color) {
        var moves = new ArrayList<Move>();
        int own = board.pieces(color);
        int opponent = board.pieces(color.opposite());
        for (int i = 0; i < 32; i++) {
            if ((own & (1 << i)) == 0) {
                continue;
            }
            boolean king = (board.kings() & (1 << i)) != 0;
            // The moving piece leaves its square, so a king may jump back through it.
            int occupied = board.occupied() & ~(1 << i);
            jump(i, i, king, color, opponent, occupied, 0, new ArrayList<>(), new ArrayList<>(), moves);
        }
        return moves;
    }

    public static List<Move> simpleMoves(Board board, Color color) {
        var moves = new ArrayList<Move>();
        int own = board.pieces(color);
        int occupied = board.occupied();
        for (int i = 0; i < 32; i++) {
            if ((own & (1 << i)) == 0) {
                continue;
            }
            boolean king = (board.kings() & (1 << i)) != 0;
            for (int d : king ? Geometry.ALL : Geometry.forward(color)) {
                int to = Geometry.STEP[i][d];
                if (to >= 0 && (occupied & (1 << to)) == 0) {
                    moves.add(Move.simple(i + 1, to + 1));
                }
            }
        }
        return moves;
    }

    public static boolean hasMoves(Board board, Color color) {
        return !legalMoves(board, color).isEmpty();
    }

    private static void jump(int start, int current, boolean king, Color color, int opponent, int occupied,
                             int capturedMask, List<Integer> path, List<Integer> captured, List<Move> out) {
        boolean extended = false;
        for (int d : king ? Geometry.ALL : Geometry.forward(color)) {
            int over = Geometry.STEP[current][d];
            int land = Geometry.JUMP[current][d];
            if (land < 0) {
                continue;
            }
            int overBit = 1 << over;
            if ((opponent & overBit) == 0 || (capturedMask & overBit) != 0 || (occupied & (1 << land)) != 0) {
                continue;
            }
            extended = true;
            path.add(land + 1);
            captured.add(over + 1);
            boolean promotes = !king && ((1 << land) & Geometry.promotionRow(color)) != 0;
            if (promotes) {
                // A man crowned during a capture ends its turn on the king row.
                out.add(new Move(start + 1, path, captured));
            } else {
                jump(start, land, king, color, opponent, occupied, capturedMask | overBit, path, captured, out);
            }
            path.removeLast();
            captured.removeLast();
        }
        if (!extended && !path.isEmpty()) {
            out.add(new Move(start + 1, path, captured));
        }
    }
}
