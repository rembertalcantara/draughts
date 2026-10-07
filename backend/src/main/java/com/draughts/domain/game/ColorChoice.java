package com.draughts.domain.game;

import com.draughts.domain.engine.Color;

import java.util.random.RandomGenerator;

public enum ColorChoice {
    BLACK,
    WHITE,
    RANDOM;

    public Color resolve(RandomGenerator random) {
        return switch (this) {
            case BLACK -> Color.BLACK;
            case WHITE -> Color.WHITE;
            case RANDOM -> random.nextBoolean() ? Color.BLACK : Color.WHITE;
        };
    }
}
