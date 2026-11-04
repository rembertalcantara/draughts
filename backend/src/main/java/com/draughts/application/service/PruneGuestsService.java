package com.draughts.application.service;

import com.draughts.application.port.in.PruneGuestsUseCase;
import com.draughts.application.port.out.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
@Transactional
@RequiredArgsConstructor
class PruneGuestsService implements PruneGuestsUseCase {

    private final PlayerRepository players;
    private final Clock clock;

    @Override
    public int pruneUnusedGuests(Duration minAge) {
        return players.deleteWithoutGamesCreatedBefore(clock.instant().minus(minAge));
    }
}
