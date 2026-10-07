package com.draughts.game;

import com.draughts.ai.Difficulty;

/**
 * @param difficulty required when {@code opponent} is {@link Opponent#AI}
 * @param variant    rule variant; {@code null} means the default
 */
public record CreateGameCommand(Opponent opponent, ColorChoice color, Difficulty difficulty, String variant) {
}
