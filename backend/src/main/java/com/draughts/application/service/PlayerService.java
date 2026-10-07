package com.draughts.application.service;

import com.draughts.application.port.in.PlayerUseCase;
import com.draughts.application.port.out.PlayerRepository;
import com.draughts.domain.player.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

@Service
@Transactional
@RequiredArgsConstructor
class PlayerService implements PlayerUseCase {

    private final PlayerRepository players;
    private final Clock clock;
    private final RandomGenerator random;

    @Override
    public Player registerGuest() {
        var player = Player.guest(random, clock.instant());
        players.save(player);
        return player;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Player> find(UUID playerId) {
        return players.findById(playerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> displayNames(Collection<UUID> playerIds) {
        return players.findDisplayNames(playerIds);
    }

    @Override
    public Player rename(UUID playerId, String displayName) {
        var player = players.findById(playerId)
                .orElseThrow(() -> new NoSuchElementException("Player " + playerId + " not found"))
                .withDisplayName(displayName);
        players.save(player);
        return player;
    }
}
