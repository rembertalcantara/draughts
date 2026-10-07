package com.draughts.game;

import com.draughts.engine.GameStatus;

public enum ResultReason {
    NO_PIECES,
    NO_MOVES,
    MOVE_LIMIT,
    REPETITION,
    RESIGNATION,
    AGREEMENT;

    static ResultReason from(GameStatus.Reason reason) {
        return ResultReason.valueOf(reason.name());
    }
}
