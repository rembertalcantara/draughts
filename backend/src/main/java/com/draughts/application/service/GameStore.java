package com.draughts.application.service;

import com.draughts.application.port.out.GameEventPublisher;
import com.draughts.application.port.out.GameRepository;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.GameChanged;
import com.draughts.domain.game.GameException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Loads and stores games for the use cases, announcing every change. */
@Component
@RequiredArgsConstructor
class GameStore {

    private final GameRepository games;
    private final GameEventPublisher events;

    Game load(UUID gameId) {
        return games.findById(gameId).orElseThrow(() -> GameException.notFound(gameId));
    }

    Game insert(Game game) {
        games.insert(game);
        events.publish(new GameChanged(game));
        return game;
    }

    Game update(Game previous, Game current) {
        if (current == previous) {
            return current;
        }
        games.update(previous, current);
        events.publish(new GameChanged(current));
        return current;
    }
}
