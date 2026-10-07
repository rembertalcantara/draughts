package com.draughts.application.service;

import com.draughts.application.port.in.PlayComputerMoveUseCase;
import com.draughts.application.port.in.PlayMoveCommand;
import com.draughts.application.port.in.PlayMoveUseCase;
import com.draughts.application.port.out.ComputerPlayer;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
class PlayMoveService implements PlayMoveUseCase, PlayComputerMoveUseCase {

    private final GameStore store;
    private final Variants variants;
    private final ComputerPlayer computer;
    private final Clock clock;

    @Override
    public Game play(PlayMoveCommand command) {
        var game = store.load(command.gameId());
        var next = game.play(variants.forVariant(game.variant()), command.playerId(), command.from(), command.path(),
                command.expectedVersion(), clock.instant());
        return store.update(game, next);
    }

    @Override
    public Game playComputerMove(UUID gameId) {
        var game = store.load(gameId);
        if (!game.isComputerTurn()) {
            return game;
        }
        var move = computer.chooseMove(game.state(), game.aiDifficulty());
        return store.update(game, game.playComputerMove(variants.forVariant(game.variant()), move, clock.instant()));
    }
}
