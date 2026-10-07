package com.draughts.application.service;

import com.draughts.application.port.in.JoinGameUseCase;
import com.draughts.domain.game.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
class JoinGameService implements JoinGameUseCase {

    private final GameStore store;
    private final Clock clock;

    @Override
    public Game join(UUID gameId, UUID playerId) {
        var game = store.load(gameId);
        return store.update(game, game.join(playerId, clock.instant()));
    }
}
