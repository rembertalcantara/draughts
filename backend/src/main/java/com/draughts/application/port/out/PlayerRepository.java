package com.draughts.application.port.out;

import com.draughts.domain.player.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for players. */
public interface PlayerRepository {

    void save(Player player);

    Optional<Player> findById(UUID id);

    Map<UUID, String> findDisplayNames(Collection<UUID> ids);
}
