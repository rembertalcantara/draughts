package com.draughts.engine;

/** Outcome of a position according to the rules alone (resignation and agreement are handled by the game). */
public sealed interface GameStatus {

    record Ongoing() implements GameStatus {
    }

    record Won(Color winner, Reason reason) implements GameStatus {
    }

    record Drawn(Reason reason) implements GameStatus {
    }

    enum Reason {
        NO_PIECES,
        NO_MOVES,
        MOVE_LIMIT,
        REPETITION
    }

    GameStatus ONGOING = new Ongoing();

    default boolean isOver() {
        return !(this instanceof Ongoing);
    }
}
