package com.draughts.application.port.in;

import lombok.NonNull;

import java.util.List;
import java.util.UUID;

/** @param expectedVersion version the client based the move on, or {@code null} to skip the check */
public record PlayMoveCommand(@NonNull UUID gameId, @NonNull UUID playerId, int from, @NonNull List<Integer> path,
                              Integer expectedVersion) {
}
