package com.draughts.domain.engine;

import java.util.List;

/** A draughts variant. Implementations must be stateless and thread-safe. */
public interface RuleSet {

    /** Identifier stored with each game, e.g. {@code ENGLISH}. */
    String variant();

    GameState initial();

    /** Legal moves for the side to move; only captures when any capture is available. */
    List<Move> legalMoves(GameState state);

    /**
     * Plays the legal move that starts at {@code from} and visits {@code path}.
     *
     * @throws IllegalMoveException when no such legal move exists
     */
    GameState apply(GameState state, int from, List<Integer> path);

    default GameState apply(GameState state, Move move) {
        return apply(state, move.from(), move.path());
    }

    /** Finds the legal move matching {@code from} and {@code path}, resolving its captured squares. */
    Move resolve(GameState state, int from, List<Integer> path);

    GameStatus status(GameState state);

    /** Rebuilds a position by playing {@code moves} from the initial position. */
    default GameState replay(List<Move> moves) {
        var state = initial();
        for (var move : moves) {
            state = apply(state, move);
        }
        return state;
    }
}
