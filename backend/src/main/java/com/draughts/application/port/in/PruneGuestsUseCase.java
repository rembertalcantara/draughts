package com.draughts.application.port.in;

import java.time.Duration;

/** Housekeeping: removes guest players that never played, so unused identities do not pile up. */
public interface PruneGuestsUseCase {

    /**
     * Deletes players older than {@code minAge} that never took part in a game.
     *
     * @return how many were deleted
     */
    int pruneUnusedGuests(Duration minAge);
}
