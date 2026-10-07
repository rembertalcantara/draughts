package com.draughts.application.port.in;

import com.draughts.domain.game.Game;

public interface PlayMoveUseCase {

    Game play(PlayMoveCommand command);
}
