package com.draughts.application.port.in;

import com.draughts.domain.game.ColorChoice;
import com.draughts.domain.game.Difficulty;
import com.draughts.domain.game.Opponent;
import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

/**
 * @param color      {@code null} means random
 * @param difficulty used when {@code opponent} is {@link Opponent#AI}; {@code null} means medium
 * @param variant    {@code null} means the default variant
 */
@Builder
public record CreateGameCommand(@NonNull UUID playerId, @NonNull Opponent opponent, ColorChoice color,
                                Difficulty difficulty, String variant) {
}
