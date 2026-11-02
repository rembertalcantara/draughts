package com.draughts.application.port.out;

import com.draughts.domain.game.Game;

import java.util.Optional;
import java.util.UUID;

/** Persistence port for the Game aggregate; listings live in {@link GameFinder}. */
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
}
