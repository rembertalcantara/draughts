package com.draughts.game;

import com.draughts.engine.Color;

import java.util.random.RandomGenerator;

public enum ColorChoice {
    BLACK,
    WHITE,
    RANDOM;

    Color resolve(RandomGenerator random) {
        return switch (this) {
            case BLACK -> Color.BLACK;
            case WHITE -> Color.WHITE;
            case RANDOM -> random.nextBoolean() ? Color.BLACK : Color.WHITE;
        };
    }
}
