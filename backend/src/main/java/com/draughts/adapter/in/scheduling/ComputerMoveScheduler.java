package com.draughts.adapter.in.scheduling;

import com.draughts.application.port.in.GameQuery;
import com.draughts.application.port.in.PlayComputerMoveUseCase;
import com.draughts.domain.game.GameChanged;
import com.draughts.domain.game.GameException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Drives the computer opponent: after each committed change where it is the computer's turn, and in a
 * periodic sweep that recovers games after a restart. Searches run on a dedicated pool so they never
 * starve request threads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ComputerMoveScheduler {

    private final PlayComputerMoveUseCase playComputerMove;
    private final GameQuery games;
    private final ExecutorService computerMoveExecutor;
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    @TransactionalEventListener(fallbackExecution = true)
    void onGameChanged(GameChanged event) {
        if (event.game().isComputerTurn()) {
            schedule(event.game().id());
        }
    }

    @Scheduled(fixedDelayString = "${draughts.ai.sweep-interval:PT15S}")
    void sweep() {
        games.awaitingComputer(50).forEach(this::schedule);
    }

    private void schedule(UUID gameId) {
        if (!pending.add(gameId)) {
            return;
        }
        computerMoveExecutor.execute(() -> {
            try {
                playComputerMove.playComputerMove(gameId);
            } catch (GameException e) {
                log.debug("Computer move skipped for game {}: {}", gameId, e.getMessage());
            } catch (RuntimeException e) {
                log.error("Computer move failed for game {}", gameId, e);
            } finally {
                pending.remove(gameId);
            }
        });
    }
}
