package com.draughts.engine;

import java.util.Objects;

public record Piece(Color color, PieceType type) {

    public Piece {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(type, "type");
    }

    public boolean isKing() {
        return type == PieceType.KING;
    }
}
