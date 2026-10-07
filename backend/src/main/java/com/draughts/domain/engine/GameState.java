package com.draughts.domain.engine;

import lombok.NonNull;

import java.util.List;

/**
 * Immutable game position plus the history needed by the draw rules.
 *
 * @param board       current pieces
 * @param turn        side to move
 * @param quietPlies  plies since the last capture or man move
 * @param repetitions position hashes since the last irreversible move, including the current one
 */
public record GameState(@NonNull Board board, @NonNull Color turn, int quietPlies, List<Long> repetitions) {

    public GameState {
        repetitions = List.copyOf(repetitions);
    }

    /** A state with no history, e.g. one set up from a FEN string. */
    public static GameState of(Board board, Color turn) {
        return new GameState(board, turn, 0, List.of(Zobrist.hash(board, turn)));
    }

    public long hash() {
        return repetitions.getLast();
    }

    /** How many times the current position has occurred since the last irreversible move. */
    public int occurrencesOfCurrentPosition() {
        long current = hash();
        return (int) repetitions.stream().filter(h -> h == current).count();
    }
}
