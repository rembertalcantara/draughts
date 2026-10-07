package com.draughts.game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for games. Implemented by the persistence adapter. */
public interface GameRepository {

    void insert(Game game);

    /**
     * Stores {@code game} and appends {@code newMoves} to its log, provided the stored version is still
     * {@code expectedVersion}.
     *
     * @throws GameConflictException when the game was changed concurrently
     */
    void update(Game game, int expectedVersion, List<PlayedMove> newMoves);

    Optional<Game> findById(UUID id);

    List<GameSummary> findOpen(int limit);

    List<GameSummary> findByPlayer(UUID playerId, int limit);

    /** In-progress games where it is the computer's turn, e.g. after a restart. */
    List<UUID> findAwaitingAi(int limit);
}
