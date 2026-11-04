package com.draughts.application.service;

import com.draughts.application.port.out.PlayerRepository;
import com.draughts.domain.player.Player;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PruneGuestsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");

    /** Records the cutoff it is asked to delete before. */
    private static final class RecordingPlayers implements PlayerRepository {
        Instant cutoff;

        @Override
        public void save(Player player) {
        }

        @Override
        public Optional<Player> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Map<UUID, String> findDisplayNames(Collection<UUID> ids) {
            return Map.of();
        }

        @Override
        public int deleteWithoutGamesCreatedBefore(Instant cutoff) {
            this.cutoff = cutoff;
            return 3;
        }
    }

    @Test
    void prunesGuestsOlderThanTheRetentionPeriod() {
        var players = new RecordingPlayers();
        var service = new PruneGuestsService(players, Clock.fixed(NOW, ZoneOffset.UTC));

        int removed = service.pruneUnusedGuests(Duration.ofDays(7));

        assertThat(removed).isEqualTo(3);
        assertThat(players.cutoff).isEqualTo(Instant.parse("2026-10-28T12:00:00Z"));
    }
}
