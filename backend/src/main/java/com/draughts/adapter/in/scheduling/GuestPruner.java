package com.draughts.adapter.in.scheduling;

import com.draughts.application.port.in.PruneGuestsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Every request without a valid cookie registers a new guest, so bots and health checks would otherwise grow
 * the player table without limit. Guests that never played a game are removed after a retention period.
 */
@Slf4j
@Component
class GuestPruner {

    private final PruneGuestsUseCase pruneGuests;
    private final Duration retention;

    GuestPruner(PruneGuestsUseCase pruneGuests, @Value("${draughts.guests.retention:P7D}") Duration retention) {
        this.pruneGuests = pruneGuests;
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${draughts.guests.prune-interval:PT6H}", initialDelayString = "PT5M")
    void prune() {
        try {
            int removed = pruneGuests.pruneUnusedGuests(retention);
            if (removed > 0) {
                log.info("Removed {} unused guest players older than {}", removed, retention);
            }
        } catch (RuntimeException e) {
            log.error("Pruning unused guests failed", e);
        }
    }
}
