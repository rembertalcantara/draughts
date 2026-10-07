package com.draughts.domain.game;

import com.draughts.domain.engine.GameStatus;

public enum ResultReason {
    NO_PIECES,
    NO_MOVES,
    MOVE_LIMIT,
    REPETITION,
    RESIGNATION,
    AGREEMENT;

    public static ResultReason from(GameStatus.Reason reason) {
        return ResultReason.valueOf(reason.name());
    }
}
