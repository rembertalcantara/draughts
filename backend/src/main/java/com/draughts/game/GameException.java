package com.draughts.game;

/** Base type for errors the API maps to HTTP problem responses. */
public sealed class GameException extends RuntimeException
        permits GameException.NotFound, GameException.IllegalMove, GameException.Forbidden,
        GameException.InvalidState, GameConflictException {

    GameException(String message) {
        super(message);
    }

    public static final class NotFound extends GameException {
        public NotFound(Object id) {
            super("Game " + id + " not found");
        }
    }

    /** The move is not legal in the current position. */
    public static final class IllegalMove extends GameException {
        public IllegalMove(String message) {
            super(message);
        }
    }

    /** The player may not perform this action, e.g. moving out of turn or acting in someone else's game. */
    public static final class Forbidden extends GameException {
        public Forbidden(String message) {
            super(message);
        }
    }

    /** The game is not in a state that allows the action, e.g. joining a game that already started. */
    public static final class InvalidState extends GameException {
        public InvalidState(String message) {
            super(message);
        }
    }
}
