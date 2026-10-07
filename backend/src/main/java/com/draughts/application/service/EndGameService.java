package com.draughts.application.service;

import com.draughts.application.port.in.EndGameUseCase;
import com.draughts.application.port.out.ComputerPlayer;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.Opponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
class EndGameService implements EndGameUseCase {

    private final GameStore store;
    private final ComputerPlayer computer;
    private final Clock clock;

    @Override
    public Game resign(UUID gameId, UUID playerId) {
        var game = store.load(gameId);
        return store.update(game, game.resign(playerId, clock.instant()));
    }

    @Override
    public Game offerDraw(UUID gameId, UUID playerId) {
        var game = store.load(gameId);
        boolean computerAccepts = game.opponent() == Opponent.AI && computer.acceptsDraw(game.state(), game.aiColor());
        return store.update(game, game.offerDraw(playerId, computerAccepts, clock.instant()));
    }

    @Override
    public Game declineDraw(UUID gameId, UUID playerId) {
        var game = store.load(gameId);
        return store.update(game, game.declineDraw(playerId, clock.instant()));
    }
}
