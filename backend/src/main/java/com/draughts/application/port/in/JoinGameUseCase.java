package com.draughts.application.port.in;

import com.draughts.domain.game.Game;

import java.util.UUID;

public interface JoinGameUseCase {

    Game join(UUID gameId, UUID playerId);
}
