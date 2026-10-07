package com.draughts.domain.game;

import com.draughts.domain.engine.Color;

public enum GameResult {
    BLACK_WIN,
    WHITE_WIN,
    DRAW;

    public static GameResult winFor(Color color) {
        return color == Color.BLACK ? BLACK_WIN : WHITE_WIN;
    }
}
