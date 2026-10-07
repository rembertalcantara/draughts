package com.draughts.domain.engine;

import lombok.NonNull;

public record Piece(@NonNull Color color, @NonNull PieceType type) {

    public boolean isKing() {
        return type == PieceType.KING;
    }
}
