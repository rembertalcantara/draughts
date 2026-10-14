package com.draughts.application.port.in;

import com.draughts.domain.game.Game;
import com.draughts.domain.game.GameSummary;

import java.util.List;
import java.util.UUID;

/** Read-only access to games for people: a single game and the lists they browse. */
public interface GameQuery {

    Game get(UUID gameId);

    List<GameSummary> openGames(int limit);

    List<GameSummary> gamesOf(UUID playerId, int limit);
}
