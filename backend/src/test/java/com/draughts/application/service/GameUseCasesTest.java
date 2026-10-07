package com.draughts.application.service;

import com.draughts.application.port.in.CreateGameCommand;
import com.draughts.application.port.in.PlayMoveCommand;
import com.draughts.application.port.out.ComputerPlayer;
import com.draughts.domain.engine.Color;
import com.draughts.domain.engine.EnglishDraughts;
import com.draughts.domain.engine.GameState;
import com.draughts.domain.engine.Move;
import com.draughts.domain.engine.MoveGenerator;
import com.draughts.domain.game.ColorChoice;
import com.draughts.domain.game.Difficulty;
import com.draughts.domain.game.GameChanged;
import com.draughts.domain.game.GameException;
import com.draughts.domain.game.GameSummary;
import com.draughts.domain.game.Lifecycle;
import com.draughts.domain.game.Opponent;
import com.draughts.domain.game.ResultReason;
import com.draughts.domain.game.Variants;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Orchestration of the use cases over in-memory ports; the game rules themselves are covered by GameTest. */
class GameUseCasesTest {

    /** Plays the first legal move; accepts draws only when told to. */
    private static final class FakeComputer implements ComputerPlayer {
        boolean acceptDraws;

        @Override
        public Move chooseMove(GameState state, Difficulty difficulty) {
            return MoveGenerator.legalMoves(state.board(), state.turn()).getFirst();
        }

        @Override
        public boolean acceptsDraw(GameState state, Color color) {
            return acceptDraws;
        }
    }

    private final InMemoryGameRepository repository = new InMemoryGameRepository();
    private final List<GameChanged> events = new ArrayList<>();
    private final FakeComputer computer = new FakeComputer();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final Variants variants = new Variants(List.of(new EnglishDraughts()));
    private final GameStore store = new GameStore(repository, events::add);

    private final CreateGameService create = new CreateGameService(store, variants, clock, new SplittableRandom(1));
    private final JoinGameService join = new JoinGameService(store, clock);
    private final PlayMoveService play = new PlayMoveService(store, variants, computer, clock);
    private final EndGameService end = new EndGameService(store, computer, clock);
    private final GameQueryService query = new GameQueryService(store, repository, variants);

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private CreateGameCommand.CreateGameCommandBuilder command(Opponent opponent) {
        return CreateGameCommand.builder().playerId(alice).opponent(opponent).color(ColorChoice.BLACK);
    }

    @Test
    void createJoinAndPlayArePersistedAndAnnounced() {
        var game = create.create(command(Opponent.HUMAN).build());
        assertThat(query.openGames(10)).extracting(GameSummary::id).containsExactly(game.id());

        join.join(game.id(), bob);
        var after = play.play(new PlayMoveCommand(game.id(), alice, 11, List.of(15), 1));

        assertThat(query.get(game.id())).isEqualTo(after);
        assertThat(query.openGames(10)).isEmpty();
        assertThat(query.gamesOf(bob, 10)).hasSize(1);
        assertThat(events).extracting(e -> e.game().version()).containsExactly(0, 1, 2);
    }

    @Test
    void staleUpdatesAreRejected() {
        var game = create.create(command(Opponent.HUMAN).build());
        var joined = join.join(game.id(), bob);
        repository.update(joined, joined.play(variants.forVariant(null), alice, 11, List.of(15), null, clock.instant()));

        assertThatThrownBy(() -> store.update(joined, joined.resign(bob, clock.instant())))
                .isInstanceOf(GameException.class)
                .extracting("code").isEqualTo(GameException.Code.VERSION_CONFLICT);
    }

    @Test
    void computerMovesOnlyOnItsTurn() {
        var game = create.create(command(Opponent.AI).color(ColorChoice.WHITE).difficulty(Difficulty.EASY).build());
        assertThat(query.awaitingComputer(10)).containsExactly(game.id());

        var after = play.playComputerMove(game.id());

        assertThat(after.moves()).hasSize(1);
        assertThat(play.playComputerMove(game.id())).isEqualTo(after);
        assertThat(query.awaitingComputer(10)).isEmpty();
        assertThat(query.legalMoves(after)).isNotEmpty();
    }

    @Test
    void drawOffersAskTheComputer() {
        var game = create.create(command(Opponent.AI).build());
        assertThatThrownBy(() -> end.offerDraw(game.id(), alice)).isInstanceOf(GameException.class);

        computer.acceptDraws = true;
        var drawn = end.offerDraw(game.id(), alice);

        assertThat(drawn.lifecycle()).isEqualTo(Lifecycle.FINISHED);
        assertThat(drawn.resultReason()).isEqualTo(ResultReason.AGREEMENT);
        assertThat(query.legalMoves(drawn)).isEmpty();
    }

    @Test
    void unknownGamesAreReported() {
        assertThatThrownBy(() -> query.get(UUID.randomUUID()))
                .isInstanceOf(GameException.class)
                .extracting("code").isEqualTo(GameException.Code.GAME_NOT_FOUND);
    }
}
