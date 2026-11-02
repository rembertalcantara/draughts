package com.draughts.application.service;

import com.draughts.application.port.in.PlayComputerMoveUseCase;
import com.draughts.application.port.out.ComputerPlayer;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** The computer plays its move, when it is its turn. */
@Service
@Transactional
@RequiredArgsConstructor
class PlayComputerMoveService implements PlayComputerMoveUseCase {

    private final GameStore store;
    private final Variants variants;
    private final ComputerPlayer computer;
    private final Clock clock;

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
