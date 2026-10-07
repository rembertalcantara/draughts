package com.draughts.domain.game;

import lombok.Getter;

/** Any rule or state violation of a game. {@link #code()} tells adapters how to report it. */
@Getter
public class GameException extends RuntimeException {

    public enum Code {
        GAME_NOT_FOUND,
        ILLEGAL_MOVE,
        /** The player may not do this, e.g. moving out of turn or acting in someone else's game. */
        FORBIDDEN,
        /** The game is not in a state that allows the action. */
        INVALID_STATE,
        /** The game changed since the caller last saw it. */
        VERSION_CONFLICT
    }

    private final Code code;

    public GameException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public static GameException notFound(Object gameId) {
        return new GameException(Code.GAME_NOT_FOUND, "Game " + gameId + " not found");
    }

    public static GameException illegalMove(String message) {
        return new GameException(Code.ILLEGAL_MOVE, message);
    }

    public static GameException forbidden(String message) {
        return new GameException(Code.FORBIDDEN, message);
    }

    public static GameException invalidState(String message) {
        return new GameException(Code.INVALID_STATE, message);
    }

    public static GameException conflict(String message) {
        return new GameException(Code.VERSION_CONFLICT, message);
    }
}
