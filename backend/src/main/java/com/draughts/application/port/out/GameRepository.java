package com.draughts.application.port.out;

import com.draughts.domain.game.Game;
import com.draughts.domain.game.GameSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for games. */
public interface GameRepository {

    void insert(Game game);

    /**
     * Stores {@code current}, appending the moves it has beyond {@code previous}, provided the stored game
     * is still at {@code previous.version()}.
     *
     * @throws com.draughts.domain.game.GameException with code VERSION_CONFLICT on a concurrent change
     */
    void update(Game previous, Game current);

    Optional<Game> findById(UUID id);

    List<GameSummary> findOpen(int limit);

    List<GameSummary> findByPlayer(UUID playerId, int limit);

    /** In-progress games where it is the computer's turn, e.g. after a restart. */
    List<UUID> findAwaitingComputer(int limit);
}
