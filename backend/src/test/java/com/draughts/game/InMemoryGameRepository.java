package com.draughts.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Test double honouring the optimistic-locking contract of {@link GameRepository}. */
class InMemoryGameRepository implements GameRepository {

    private final Map<UUID, Game> games = new ConcurrentHashMap<>();

    @Override
    public void insert(Game game) {
        games.put(game.id(), game);
    }

    @Override
    public synchronized void update(Game game, int expectedVersion, List<PlayedMove> newMoves) {
        var stored = games.get(game.id());
        if (stored == null || stored.version() != expectedVersion) {
            throw new GameConflictException("conflict");
        }
        games.put(game.id(), game);
    }

    @Override
    public Optional<Game> findById(UUID id) {
        return Optional.ofNullable(games.get(id));
    }

    @Override
    public List<GameSummary> findOpen(int limit) {
        return games.values().stream().filter(g -> g.lifecycle() == Lifecycle.OPEN).map(this::summary).limit(limit).toList();
    }

    @Override
    public List<GameSummary> findByPlayer(UUID playerId, int limit) {
        return games.values().stream().filter(g -> g.colorOf(playerId).isPresent())
                .sorted(Comparator.comparing(Game::updatedAt).reversed()).map(this::summary).limit(limit).toList();
    }

    @Override
    public List<UUID> findAwaitingAi(int limit) {
        return new ArrayList<>(games.values().stream().filter(Game::isAiTurn).map(Game::id).limit(limit).toList());
    }

    private GameSummary summary(Game g) {
        return new GameSummary(g.id(), g.variant(), g.lifecycle(), g.blackPlayer(), g.whitePlayer(), g.aiColor(),
                g.aiDifficulty(), g.turn(), g.result(), g.resultReason(), g.moves().size(), g.createdAt(), g.updatedAt());
    }
}
