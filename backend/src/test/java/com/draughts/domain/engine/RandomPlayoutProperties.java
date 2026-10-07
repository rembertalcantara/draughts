package com.draughts.domain.engine;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class RandomPlayoutProperties {

    private final EnglishDraughts rules = new EnglishDraughts();

    @Property(tries = 200)
    void randomGamesStayLegalAndTerminate(@ForAll long seed) {
        var random = new Random(seed);
        var state = rules.initial();
        int plies = 0;
        while (!rules.status(state).isOver()) {
            var moves = rules.legalMoves(state);
            assertThat(moves).isNotEmpty();
            var move = moves.get(random.nextInt(moves.size()));
            int before = state.board().count(Color.BLACK) + state.board().count(Color.WHITE);

            var next = rules.apply(state, move);

            int after = next.board().count(Color.BLACK) + next.board().count(Color.WHITE);
            assertThat(after).isEqualTo(before - move.captured().size());
            assertThat(next.board().count(state.turn())).isEqualTo(state.board().count(state.turn()));
            assertThat(next.board().kingCount(state.turn())).isGreaterThanOrEqualTo(state.board().kingCount(state.turn()));
            assertThat(next.turn()).isEqualTo(state.turn().opposite());
            state = next;
            assertThat(++plies).isLessThan(2_000);
        }
    }

    @Property(tries = 100)
    void replayingTheSameMovesIsDeterministic(@ForAll long seed) {
        var random = new Random(seed);
        var a = rules.initial();
        var b = rules.initial();
        for (int i = 0; i < 60 && !rules.status(a).isOver(); i++) {
            var moves = rules.legalMoves(a);
            var move = moves.get(random.nextInt(moves.size()));
            a = rules.apply(a, move);
            b = rules.apply(b, move.from(), move.path());
        }
        assertThat(a).isEqualTo(b);
    }
}
