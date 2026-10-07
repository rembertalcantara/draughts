package com.draughts.domain.engine;

import lombok.NonNull;

import java.util.List;

/**
 * A complete move for one turn.
 *
 * @param from     starting square (1-32)
 * @param path     squares visited in order; a simple move has one entry, a multi-jump one per jump
 * @param captured squares of the jumped pieces, in order (empty for a simple move)
 */
public record Move(int from, @NonNull List<Integer> path, @NonNull List<Integer> captured) {

    public Move {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        path = List.copyOf(path);
        captured = List.copyOf(captured);
    }

    public static Move simple(int from, int to) {
        return new Move(from, List.of(to), List.of());
    }

    public int to() {
        return path.getLast();
    }

    public boolean isCapture() {
        return !captured.isEmpty();
    }

    /** True when this move starts at {@code from} and visits exactly {@code path}. */
    public boolean matches(int from, List<Integer> path) {
        return this.from == from && this.path.equals(path);
    }

    /** Standard notation: {@code 11-15} for a simple move, {@code 15x24x31} for captures. */
    public String notation() {
        return Notation.format(this);
    }

    @Override
    public String toString() {
        return notation();
    }
}
