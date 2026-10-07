package com.draughts.application.port.in;

import com.draughts.domain.game.Game;

import java.util.UUID;

/** Ways a game ends other than by the rules: resignation and draw by agreement. */
public interface EndGameUseCase {

    Game resign(UUID gameId, UUID playerId);

    /** Offers a draw, or accepts the opponent's pending offer. */
    Game offerDraw(UUID gameId, UUID playerId);

    Game declineDraw(UUID gameId, UUID playerId);
}
