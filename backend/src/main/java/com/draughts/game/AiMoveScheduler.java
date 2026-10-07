package com.draughts.game;

import com.draughts.ai.SearchAi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs computer moves on a small dedicated pool so a deep search can never starve request threads.
 * Triggered after each committed change, plus a periodic sweep that recovers games after a restart.
 */
@Component
public class AiMoveScheduler implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AiMoveScheduler.class);

    private final GameService games;
    private final GameRepository repository;
    private final SearchAi ai = new SearchAi();
    private final ExecutorService executor;
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public AiMoveScheduler(GameService games, GameRepository repository, AiProperties properties) {
        this.games = games;
        this.repository = repository;
        this.executor = Executors.newFixedThreadPool(properties.threads(), Thread.ofPlatform().name("ai-", 0).factory());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onGameChanged(GameChanged event) {
        if (event.game().isAiTurn()) {
            schedule(event.game().id());
        }
    }

    @Scheduled(fixedDelayString = "${draughts.ai.sweep-interval:PT15S}")
    public void sweep() {
        repository.findAwaitingAi(50).forEach(this::schedule);
    }

    void schedule(UUID gameId) {
        if (!pending.add(gameId)) {
            return;
        }
        executor.execute(() -> {
            try {
                games.playAiMove(gameId, ai);
            } catch (GameConflictException e) {
                log.debug("Game {} changed while the computer was thinking", gameId);
            } catch (RuntimeException e) {
                log.error("Computer move failed for game {}", gameId, e);
            } finally {
                pending.remove(gameId);
            }
        });
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
