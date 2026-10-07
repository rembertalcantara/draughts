package com.draughts.application.port.in;

import com.draughts.domain.engine.Move;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.GameSummary;

import java.util.List;
import java.util.UUID;

public interface GameQuery {

    Game get(UUID gameId);

    /** Legal moves for the side to move; empty unless the game is in progress. */
    List<Move> legalMoves(Game game);

    List<GameSummary> openGames(int limit);

    List<GameSummary> gamesOf(UUID playerId, int limit);

    List<UUID> awaitingComputer(int limit);
}
