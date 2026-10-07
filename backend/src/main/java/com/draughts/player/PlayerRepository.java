package com.draughts.player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for players. */
public interface PlayerRepository {

    void insert(Player player);

    Optional<Player> findById(UUID id);

    Map<UUID, String> findDisplayNames(Collection<UUID> ids);

    void rename(UUID id, String displayName);
}
