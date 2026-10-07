package com.draughts.player;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

@Service
public class PlayerService {

    private final PlayerRepository players;
    private final Clock clock;

    public PlayerService(PlayerRepository players, Clock clock) {
        this.players = players;
        this.clock = clock;
    }

    /** Creates an anonymous guest with a generated name such as {@code Guest-3fa9}. */
    @Transactional
    public Player createGuest() {
        var suffix = HexFormat.of().toHexDigits((short) RandomGenerator.getDefault().nextInt());
        var player = new Player(UUID.randomUUID(), "Guest-" + suffix, clock.instant());
        players.insert(player);
        return player;
    }

    @Transactional(readOnly = true)
    public Optional<Player> find(UUID id) {
        return players.findById(id);
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> displayNames(Collection<UUID> ids) {
        return players.findDisplayNames(ids);
    }

    @Transactional
    public Player rename(UUID id, String displayName) {
        players.rename(id, displayName.strip());
        return players.findById(id).orElseThrow();
    }
}
