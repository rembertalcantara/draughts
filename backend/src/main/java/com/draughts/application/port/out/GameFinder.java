package com.draughts.application.port.out;

import com.draughts.domain.game.GameSummary;

import java.util.List;
import java.util.UUID;

/** Read side of game persistence: listings that never load or modify a whole aggregate. */
public interface GameFinder {

    List<GameSummary> findOpen(int limit);

    List<GameSummary> findByPlayer(UUID playerId, int limit);

    /** In-progress games where it is the computer's turn, e.g. after a restart. */
    List<UUID> findAwaitingComputer(int limit);
}
