package com.draughts.domain.player;

import lombok.NonNull;
import lombok.With;

import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.random.RandomGenerator;

public record Player(@NonNull UUID id, @With @NonNull String displayName, @NonNull Instant createdAt) {

    public static final int MAX_NAME_LENGTH = 40;

    public Player {
        displayName = displayName.strip();
        if (displayName.isEmpty() || displayName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Display name must have 1-" + MAX_NAME_LENGTH + " characters");
        }
    }

    /** An anonymous player with a generated name such as {@code Guest-3fa9}. */
    public static Player guest(RandomGenerator random, Instant now) {
        var suffix = HexFormat.of().toHexDigits((short) random.nextInt());
        return new Player(UUID.randomUUID(), "Guest-" + suffix, now);
    }
}
