package com.draughts.application.port.in;

import com.draughts.domain.game.Game;

import java.util.UUID;

public interface PlayComputerMoveUseCase {

    /** Plays the computer's move if it is still its turn; otherwise returns the game unchanged. */
    Game playComputerMove(UUID gameId);
}
