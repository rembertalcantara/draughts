package com.draughts.domain.game;

import com.draughts.domain.engine.Color;
import com.draughts.domain.engine.EnglishDraughts;
import com.draughts.domain.engine.Fen;
import com.draughts.domain.engine.Move;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final EnglishDraughts rules = new EnglishDraughts();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private Game humanGame() {
        return Game.open(UUID.randomUUID(), rules, alice, Color.BLACK, NOW).join(bob, NOW);
    }

    private static void assertCode(Runnable action, GameException.Code code) {
        assertThatThrownBy(action::run).isInstanceOf(GameException.class)
                .extracting("code").isEqualTo(code);
    }

    @Test
    void openGameStartsWhenSomeoneJoins() {
        var open = Game.open(UUID.randomUUID(), rules, alice, Color.WHITE, NOW);
        assertThat(open.lifecycle()).isEqualTo(Lifecycle.OPEN);

        var joined = open.join(bob, NOW);

        assertThat(joined.lifecycle()).isEqualTo(Lifecycle.IN_PROGRESS);
        assertThat(joined.colorOf(bob)).contains(Color.BLACK);
        assertThat(joined.colorOf(alice)).contains(Color.WHITE);
        assertThat(joined.version()).isEqualTo(open.version() + 1);
    }

    @Test
    void cannotJoinOwnOrStartedGame() {
        var open = Game.open(UUID.randomUUID(), rules, alice, Color.BLACK, NOW);
        assertCode(() -> open.join(alice, NOW), GameException.Code.INVALID_STATE);
        assertCode(() -> humanGame().join(UUID.randomUUID(), NOW), GameException.Code.INVALID_STATE);
    }

    @Test
    void playersMoveInTurn() {
        var game = humanGame();

        var after = game.play(rules, alice, 11, List.of(15), game.version(), NOW);

        assertThat(after.moves()).singleElement().satisfies(m -> {
            assertThat(m.move().notation()).isEqualTo("11-15");
            assertThat(m.playedBy()).isEqualTo(alice);
        });
        assertThat(after.turn()).isEqualTo(Color.WHITE);
        assertCode(() -> after.play(rules, alice, 10, List.of(14), null, NOW), GameException.Code.FORBIDDEN);
    }

    @Test
    void rejectsIllegalMovesOutsidersAndStaleVersions() {
        var game = humanGame();
        assertCode(() -> game.play(rules, alice, 11, List.of(18), null, NOW), GameException.Code.ILLEGAL_MOVE);
        assertCode(() -> game.play(rules, UUID.randomUUID(), 11, List.of(15), null, NOW), GameException.Code.FORBIDDEN);
        assertCode(() -> game.play(rules, alice, 11, List.of(15), game.version() - 1, NOW),
                GameException.Code.VERSION_CONFLICT);
    }

    @Test
    void gameEndsWhenTheRulesSaySo() {
        var game = humanGame().toBuilder().state(Fen.read("B:W18:B14")).build();

        var after = game.play(rules, alice, 14, List.of(23), null, NOW);

        assertThat(after.lifecycle()).isEqualTo(Lifecycle.FINISHED);
        assertThat(after.result()).isEqualTo(GameResult.BLACK_WIN);
        assertThat(after.resultReason()).isEqualTo(ResultReason.NO_PIECES);
    }

    @Test
    void resignationEndsTheGame() {
        var after = humanGame().resign(bob, NOW);
        assertThat(after.lifecycle()).isEqualTo(Lifecycle.FINISHED);
        assertThat(after.result()).isEqualTo(GameResult.BLACK_WIN);
        assertThat(after.resultReason()).isEqualTo(ResultReason.RESIGNATION);
        assertCode(() -> after.resign(alice, NOW), GameException.Code.INVALID_STATE);
    }

    @Test
    void drawByAgreementOrDecline() {
        var offered = humanGame().offerDraw(alice, false, NOW);
        assertThat(offered.drawOfferedBy()).isEqualTo(Color.BLACK);
        assertThat(offered.offerDraw(alice, false, NOW)).isSameAs(offered);

        assertThat(offered.offerDraw(bob, false, NOW).resultReason()).isEqualTo(ResultReason.AGREEMENT);
        assertThat(offered.declineDraw(bob, NOW).drawOfferedBy()).isNull();
        assertCode(() -> offered.declineDraw(alice, NOW), GameException.Code.INVALID_STATE);
    }

    @Test
    void drawOfferLapsesWhenAMoveIsPlayed() {
        var offered = humanGame().offerDraw(bob, false, NOW);
        assertThat(offered.play(rules, alice, 11, List.of(15), null, NOW).drawOfferedBy()).isNull();
    }

    @Test
    void computerGameStartsImmediatelyAndTheComputerDecidesOnDraws() {
        var game = Game.againstComputer(UUID.randomUUID(), rules, alice, Color.WHITE, Difficulty.EASY, NOW);
        assertThat(game.isComputerTurn()).isTrue();
        assertCode(() -> game.play(rules, alice, 22, List.of(18), null, NOW), GameException.Code.FORBIDDEN);

        var afterComputer = game.playComputerMove(rules, Move.simple(11, 15), NOW);

        assertThat(afterComputer.moves().getFirst().playedBy()).isNull();
        assertThat(afterComputer.isComputerTurn()).isFalse();
        assertCode(() -> afterComputer.playComputerMove(rules, Move.simple(22, 18), NOW), GameException.Code.INVALID_STATE);
        assertCode(() -> afterComputer.offerDraw(alice, false, NOW), GameException.Code.INVALID_STATE);
        assertThat(afterComputer.offerDraw(alice, true, NOW).result()).isEqualTo(GameResult.DRAW);
    }
}
