package com.draughts.ai;

import com.draughts.engine.Color;
import com.draughts.engine.EnglishDraughts;
import com.draughts.engine.Fen;
import com.draughts.engine.GameStatus;
import com.draughts.engine.MoveGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Random;
import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;

class SearchAiTest {

    private final EnglishDraughts rules = new EnglishDraughts();
    private final SearchAi ai = new SearchAi(new SplittableRandom(1));

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void returnsALegalMoveWithinBudget(Difficulty difficulty) {
        var state = rules.initial();
        long start = System.nanoTime();
        var move = ai.chooseMove(state.board(), state.turn(), difficulty);
        var elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(MoveGenerator.legalMoves(state.board(), state.turn())).contains(move);
        assertThat(elapsed).isLessThan(difficulty.timeBudget().plusMillis(500));
    }

    @Test
    void playsTheOnlyMove() {
        var state = Fen.read("B:W18:B9,14");
        assertThat(ai.chooseMove(state.board(), state.turn(), Difficulty.HARD).notation()).isEqualTo("14x23");
    }

    @Test
    void prefersTheCaptureThatWinsMoreMaterial() {
        // Black can play 1x10x19 (two pieces) or 13x22 (one piece).
        var state = Fen.read("B:W6,15,17:B1,13");
        assertThat(MoveGenerator.legalMoves(state.board(), state.turn())).hasSize(2);
        var move = ai.chooseMove(state.board(), state.turn(), Difficulty.MEDIUM);
        assertThat(move.captured()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3})
    void beatsARandomPlayer(long seed) {
        var random = new Random(seed);
        var strong = new SearchAi(new SplittableRandom(seed));
        var state = rules.initial();
        while (!rules.status(state).isOver()) {
            var move = state.turn() == Color.BLACK
                    ? strong.chooseMove(state.board(), state.turn(), Difficulty.MEDIUM)
                    : rules.legalMoves(state).get(random.nextInt(rules.legalMoves(state).size()));
            state = rules.apply(state, move);
        }
        assertThat(rules.status(state)).isEqualTo(new GameStatus.Won(Color.BLACK,
                ((GameStatus.Won) rules.status(state)).reason()));
    }
}
