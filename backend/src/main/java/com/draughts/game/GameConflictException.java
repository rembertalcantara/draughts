package com.draughts.game;

/** The game changed since the client last saw it (optimistic concurrency check failed). */
public final class GameConflictException extends GameException {

    public GameConflictException(String message) {
        super(message);
    }
}
