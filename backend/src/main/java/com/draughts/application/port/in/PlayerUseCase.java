package com.draughts.application.port.in;

import com.draughts.domain.player.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlayerUseCase {

    Player registerGuest();

    Optional<Player> find(UUID playerId);

    Map<UUID, String> displayNames(Collection<UUID> playerIds);

    Player rename(UUID playerId, String displayName);
}
