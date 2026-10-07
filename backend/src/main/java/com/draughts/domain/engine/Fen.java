package com.draughts.domain.engine;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * PDN-style FEN for draughts, e.g. {@code B:W21,22,K30:B1,2,3}: side to move, then the white and
 * black pieces, kings prefixed with {@code K}.
 */
@UtilityClass
public class Fen {

    public static String write(Board board, Color turn) {
        return (turn == Color.BLACK ? "B" : "W") + ":W" + squares(board, Color.WHITE) + ":B" + squares(board, Color.BLACK);
    }

    public static GameState read(String fen) {
        var parts = fen.trim().split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid FEN: " + fen);
        }
        var turn = switch (parts[0]) {
            case "B" -> Color.BLACK;
            case "W" -> Color.WHITE;
            default -> throw new IllegalArgumentException("Invalid side to move: " + parts[0]);
        };
        var pieces = new HashMap<Integer, Piece>();
        for (int i = 1; i < 3; i++) {
            var color = switch (parts[i].charAt(0)) {
                case 'B' -> Color.BLACK;
                case 'W' -> Color.WHITE;
                default -> throw new IllegalArgumentException("Invalid colour in FEN: " + parts[i]);
            };
            var list = parts[i].substring(1);
            if (list.isBlank()) {
                continue;
            }
            for (var token : list.split(",")) {
                boolean king = token.startsWith("K");
                int square = Integer.parseInt(king ? token.substring(1) : token);
                pieces.put(square, new Piece(color, king ? PieceType.KING : PieceType.MAN));
            }
        }
        return GameState.of(Board.of(pieces), turn);
    }

    private static String squares(Board board, Color color) {
        var tokens = new ArrayList<String>();
        board.pieces().forEach((square, piece) -> {
            if (piece.color() == color) {
                tokens.add((piece.isKing() ? "K" : "") + square);
            }
        });
        return String.join(",", tokens);
    }
}
