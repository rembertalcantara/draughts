package com.draughts.game;

import com.draughts.ai.Difficulty;
import com.draughts.ai.SearchAi;
import com.draughts.engine.Color;
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

class GameServiceTest {

    private final InMemoryGameRepository repository = new InMemoryGameRepository();
    private final List<Object> events = new ArrayList<>();
    private final GameService service = new GameService(repository, new Variants(), events::add,
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private Game humanGame() {
        var game = service.create(alice, new CreateGameCommand(Opponent.HUMAN, ColorChoice.BLACK, null, null));
        return service.join(game.id(), bob);
    }

    @Test
    void humanGameIsOpenUntilSomeoneJoins() {
        var game = service.create(alice, new CreateGameCommand(Opponent.HUMAN, ColorChoice.WHITE, null, null));
        assertThat(game.lifecycle()).isEqualTo(Lifecycle.OPEN);
        assertThat(service.openGames(10)).extracting(GameSummary::id).containsExactly(game.id());

        var joined = service.join(game.id(), bob);

        assertThat(joined.lifecycle()).isEqualTo(Lifecycle.IN_PROGRESS);
        assertThat(joined.colorOf(bob)).contains(Color.BLACK);
        assertThat(joined.colorOf(alice)).contains(Color.WHITE);
        assertThat(service.openGames(10)).isEmpty();
    }

    @Test
    void cannotJoinOwnGame() {
        var game = service.create(alice, new CreateGameCommand(Opponent.HUMAN, ColorChoice.BLACK, null, null));
        assertThatThrownBy(() -> service.join(game.id(), alice)).isInstanceOf(GameException.InvalidState.class);
    }

    @Test
    void playersMoveInTurn() {
        var game = humanGame();

        var after = service.move(game.id(), alice, 11, List.of(15), game.version());

        assertThat(after.moves()).hasSize(1);
        assertThat(after.turn()).isEqualTo(Color.WHITE);
        assertThat(after.version()).isEqualTo(game.version() + 1);
        assertThatThrownBy(() -> service.move(game.id(), alice, 10, List.of(14), null))
                .isInstanceOf(GameException.Forbidden.class);
        assertThat(events).last().isEqualTo(new GameChanged(after));
    }

    @Test
    void rejectsIllegalMovesAndOutsiders() {
        var game = humanGame();
        assertThatThrownBy(() -> service.move(game.id(), alice, 11, List.of(18), null))
                .isInstanceOf(GameException.IllegalMove.class);
        assertThatThrownBy(() -> service.move(game.id(), UUID.randomUUID(), 11, List.of(15), null))
                .isInstanceOf(GameException.Forbidden.class);
    }

    @Test
    void rejectsMovesBasedOnAStaleVersion() {
        var game = humanGame();
        service.move(game.id(), alice, 11, List.of(15), game.version());
        assertThatThrownBy(() -> service.move(game.id(), bob, 22, List.of(18), game.version()))
                .isInstanceOf(GameConflictException.class);
    }

    @Test
    void resignationEndsTheGame() {
        var game = humanGame();
        var after = service.resign(game.id(), bob);
        assertThat(after.lifecycle()).isEqualTo(Lifecycle.FINISHED);
        assertThat(after.result()).isEqualTo(GameResult.BLACK_WIN);
        assertThat(after.resultReason()).isEqualTo(ResultReason.RESIGNATION);
    }

    @Test
    void drawByAgreement() {
        var game = humanGame();
        var offered = service.offerDraw(game.id(), alice);
        assertThat(offered.drawOfferedBy()).isEqualTo(Color.BLACK);

        var accepted = service.offerDraw(game.id(), bob);

        assertThat(accepted.result()).isEqualTo(GameResult.DRAW);
        assertThat(accepted.resultReason()).isEqualTo(ResultReason.AGREEMENT);
    }

    @Test
    void drawOfferLapsesWhenAMoveIsPlayed() {
        var game = humanGame();
        service.offerDraw(game.id(), bob);
        var after = service.move(game.id(), alice, 11, List.of(15), null);
        assertThat(after.drawOfferedBy()).isNull();
    }

    @Test
    void computerRepliesWhenItIsItsTurn() {
        var game = service.create(alice, new CreateGameCommand(Opponent.AI, ColorChoice.WHITE, Difficulty.EASY, null));
        assertThat(game.isAiTurn()).isTrue();

        var after = service.playAiMove(game.id(), new SearchAi(new SplittableRandom(7)));

        assertThat(after.moves()).hasSize(1);
        assertThat(after.moves().getFirst().playedBy()).isNull();
        assertThat(after.isAiTurn()).isFalse();
        assertThat(service.playAiMove(game.id(), new SearchAi())).isEqualTo(after);
    }

    @Test
    void historyListsOnlyThePlayersGames() {
        humanGame();
        service.create(UUID.randomUUID(), new CreateGameCommand(Opponent.AI, ColorChoice.BLACK, Difficulty.EASY, null));
        assertThat(service.gamesOf(alice, 10)).hasSize(1);
    }
}
