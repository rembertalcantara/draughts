package com.draughts.application.service;

import com.draughts.application.port.in.PlayMoveCommand;
import com.draughts.application.port.in.PlayMoveUseCase;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** A person plays a move. */
@Service
@Transactional
@RequiredArgsConstructor
class PlayMoveService implements PlayMoveUseCase {

    private final GameStore store;
    private final Variants variants;
    private final Clock clock;

    @Override
    public Game play(PlayMoveCommand command) {
        var game = store.load(command.gameId());
        var next = game.play(variants.forVariant(game.variant()), command.playerId(), command.from(), command.path(),
                command.expectedVersion(), clock.instant());
        return store.update(game, next);
    }
}
