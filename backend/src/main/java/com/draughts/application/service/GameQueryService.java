package com.draughts.application.service;

import com.draughts.application.port.in.GameQuery;
import com.draughts.application.port.out.GameRepository;
import com.draughts.domain.engine.Move;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.GameSummary;
import com.draughts.domain.game.Lifecycle;
import com.draughts.domain.game.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class GameQueryService implements GameQuery {

    private final GameStore store;
    private final GameRepository games;
    private final Variants variants;

    @Override
    public Game get(UUID gameId) {
        return store.load(gameId);
    }

    @Override
    public List<Move> legalMoves(Game game) {
        return game.lifecycle() == Lifecycle.IN_PROGRESS
                ? variants.forVariant(game.variant()).legalMoves(game.state())
                : List.of();
    }

    @Override
    public List<GameSummary> openGames(int limit) {
        return games.findOpen(limit);
    }

    @Override
    public List<GameSummary> gamesOf(UUID playerId, int limit) {
        return games.findByPlayer(playerId, limit);
    }

    @Override
    public List<UUID> awaitingComputer(int limit) {
        return games.findAwaitingComputer(limit);
    }
}
