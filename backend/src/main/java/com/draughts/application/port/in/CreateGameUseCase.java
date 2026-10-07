package com.draughts.application.port.in;

import com.draughts.domain.game.Game;

public interface CreateGameUseCase {

    Game create(CreateGameCommand command);
}
