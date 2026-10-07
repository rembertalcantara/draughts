package com.draughts.player;

import java.time.Instant;
import java.util.UUID;

public record Player(UUID id, String displayName, Instant createdAt) {
}
