package com.draughts.engine;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable board stored as three bitboards. Bit {@code i} is square {@code i + 1}.
 *
 * @param black squares occupied by black pieces
 * @param white squares occupied by white pieces
 * @param kings squares occupied by kings of either colour
 */
public record Board(int black, int white, int kings) {

    public static final int SQUARES = 32;

    private static final Board INITIAL = new Board(0x0000_0FFF, 0xFFF0_0000, 0);

    public Board {
        if ((black & white) != 0) {
            throw new IllegalArgumentException("A square cannot hold two pieces");
        }
        if ((kings & ~(black | white)) != 0) {
            throw new IllegalArgumentException("Kings must be on occupied squares");
        }
    }

    /** Standard starting position: Black on 1-12, White on 21-32. */
    public static Board initial() {
        return INITIAL;
    }

    public static Board empty() {
        return new Board(0, 0, 0);
    }

    public static Board of(Map<Integer, Piece> pieces) {
        int b = 0;
        int w = 0;
        int k = 0;
        for (var entry : pieces.entrySet()) {
            int bit = bit(entry.getKey());
            if (entry.getValue().color() == Color.BLACK) {
                b |= bit;
            } else {
                w |= bit;
            }
            if (entry.getValue().isKing()) {
                k |= bit;
            }
        }
        return new Board(b, w, k);
    }

    public Optional<Piece> pieceAt(int square) {
        int bit = bit(square);
        if ((black & bit) != 0) {
            return Optional.of(new Piece(Color.BLACK, (kings & bit) != 0 ? PieceType.KING : PieceType.MAN));
        }
        if ((white & bit) != 0) {
            return Optional.of(new Piece(Color.WHITE, (kings & bit) != 0 ? PieceType.KING : PieceType.MAN));
        }
        return Optional.empty();
    }

    /** All pieces keyed by square, in square order. */
    public Map<Integer, Piece> pieces() {
        var result = new TreeMap<Integer, Piece>();
        for (int sq = 1; sq <= SQUARES; sq++) {
            int s = sq;
            pieceAt(sq).ifPresent(p -> result.put(s, p));
        }
        return result;
    }

    public int occupied() {
        return black | white;
    }

    public int pieces(Color color) {
        return color == Color.BLACK ? black : white;
    }

    public int count(Color color) {
        return Integer.bitCount(pieces(color));
    }

    public int kingCount(Color color) {
        return Integer.bitCount(pieces(color) & kings);
    }

    /**
     * Plays a move for {@code color} without checking legality. Captured pieces are removed and a
     * man that ends on the far row is promoted.
     */
    public Board apply(Move move, Color color) {
        int fromBit = bit(move.from());
        int toBit = bit(move.to());
        int own = pieces(color);
        if ((own & fromBit) == 0) {
            throw new IllegalArgumentException("No " + color + " piece on " + move.from());
        }
        boolean king = (kings & fromBit) != 0;
        int capturedMask = 0;
        for (int sq : move.captured()) {
            capturedMask |= bit(sq);
        }
        own = (own & ~fromBit) | toBit;
        int opponent = pieces(color.opposite()) & ~capturedMask;
        int newKings = kings & ~fromBit & ~capturedMask;
        if (king || (toBit & Geometry.promotionRow(color)) != 0) {
            newKings |= toBit;
        }
        return color == Color.BLACK ? new Board(own, opponent, newKings) : new Board(opponent, own, newKings);
    }

    static int bit(int square) {
        if (square < 1 || square > SQUARES) {
            throw new IllegalArgumentException("Square out of range: " + square);
        }
        return 1 << (square - 1);
    }
}
