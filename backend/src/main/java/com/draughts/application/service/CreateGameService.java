package com.draughts.application.service;

import com.draughts.application.port.in.CreateGameCommand;
import com.draughts.application.port.in.CreateGameUseCase;
import com.draughts.domain.game.ColorChoice;
import com.draughts.domain.game.Difficulty;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.Opponent;
import com.draughts.domain.game.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.random.RandomGenerator;

@Service
@Transactional
@RequiredArgsConstructor
class CreateGameService implements CreateGameUseCase {

    private final GameStore store;
    private final Variants variants;
    private final Clock clock;
    private final RandomGenerator random;

    @Override
    public Game create(CreateGameCommand command) {
        var rules = variants.forVariant(command.variant());
        var color = Objects.requireNonNullElse(command.color(), ColorChoice.RANDOM).resolve(random);
        var now = clock.instant();
        var game = command.opponent() == Opponent.AI
                ? Game.againstComputer(UUID.randomUUID(), rules, command.playerId(), color,
                Objects.requireNonNullElse(command.difficulty(), Difficulty.MEDIUM), now)
                : Game.open(UUID.randomUUID(), rules, command.playerId(), color, now);
        return store.insert(game);
    }
}
