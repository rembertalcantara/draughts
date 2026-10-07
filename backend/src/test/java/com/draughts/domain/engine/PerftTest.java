package com.draughts.domain.engine;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Move-path enumeration from the start position, compared with published English draughts perft values. */
class PerftTest {

    @ParameterizedTest(name = "perft({0}) = {1}")
    @CsvSource({"1,7", "2,49", "3,302", "4,1469", "5,7361", "6,36768", "7,179740"})
    void matchesKnownCounts(int depth, long expected) {
        assertThat(perft(Board.initial(), Color.BLACK, depth)).isEqualTo(expected);
    }

    private static long perft(Board board, Color toMove, int depth) {
        if (depth == 0) {
            return 1;
        }
        long nodes = 0;
        for (var move : MoveGenerator.legalMoves(board, toMove)) {
            nodes += perft(board.apply(move, toMove), toMove.opposite(), depth - 1);
        }
        return nodes;
    }
}
