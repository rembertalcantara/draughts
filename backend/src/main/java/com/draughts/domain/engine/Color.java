package com.draughts.domain.engine;

/** Side to play. Black starts on squares 1-12 and moves first. */
public enum Color {
    BLACK,
    WHITE;

    public Color opposite() {
        return this == BLACK ? WHITE : BLACK;
    }
}
