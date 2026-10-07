package com.draughts.domain.game;

import com.draughts.domain.engine.Color;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Lightweight projection for lobby and history listings. */
public record GameSummary(
        UUID id,
        String variant,
        Lifecycle lifecycle,
        UUID blackPlayer,
        UUID whitePlayer,
        Color aiColor,
        Difficulty aiDifficulty,
        Color turn,
        GameResult result,
        ResultReason resultReason,
        int moveCount,
        Instant createdAt,
        Instant updatedAt) {

    public Opponent opponent() {
        return aiColor != null ? Opponent.AI : Opponent.HUMAN;
    }

    public Optional<Color> colorOf(UUID playerId) {
        if (playerId != null && playerId.equals(blackPlayer)) {
            return Optional.of(Color.BLACK);
        }
        if (playerId != null && playerId.equals(whitePlayer)) {
            return Optional.of(Color.WHITE);
        }
        return Optional.empty();
    }
}
