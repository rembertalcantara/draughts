package com.draughts.domain.engine;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/** Reads and writes moves in standard draughts notation ({@code 11-15}, {@code 15x24x31}). */
@UtilityClass
public class Notation {

    public static String format(Move move) {
        var sep = move.isCapture() ? "x" : "-";
        var sb = new StringBuilder().append(move.from());
        for (int square : move.path()) {
            sb.append(sep).append(square);
        }
        return sb.toString();
    }

    /** Parses the squares of a move; captured pieces are not encoded and must be resolved against a position. */
    public static List<Integer> parseSquares(String text) {
        var parts = text.trim().split("[-x]");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Not a move: " + text);
        }
        var squares = new ArrayList<Integer>(parts.length);
        for (var part : parts) {
            int square = Integer.parseInt(part);
            if (square < 1 || square > Board.SQUARES) {
                throw new IllegalArgumentException("Square out of range: " + square);
            }
            squares.add(square);
        }
        return squares;
    }
}
