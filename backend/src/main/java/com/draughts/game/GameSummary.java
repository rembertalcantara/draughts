package com.draughts.game;

import com.draughts.ai.Difficulty;
import com.draughts.engine.Color;

import java.time.Instant;
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
}
