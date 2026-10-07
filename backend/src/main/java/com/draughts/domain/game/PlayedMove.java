package com.draughts.domain.game;

import com.draughts.domain.engine.Color;
import com.draughts.domain.engine.Move;

import java.time.Instant;
import java.util.UUID;

/**
 * One entry of the append-only move log.
 *
 * @param ply      1-based half-move number
 * @param playedBy player who made the move, or {@code null} for the computer
 */
public record PlayedMove(int ply, Color color, Move move, UUID playedBy, Instant playedAt) {
}
