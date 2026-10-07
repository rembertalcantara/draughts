package com.draughts.engine;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnglishDraughtsTest {

    private final EnglishDraughts rules = new EnglishDraughts();

    private static List<String> notations(List<Move> moves) {
        return moves.stream().map(Move::notation).sorted().toList();
    }

    @Test
    void initialPositionHasTwelvePiecesEachAndBlackToMove() {
        var state = rules.initial();
        assertThat(state.turn()).isEqualTo(Color.BLACK);
        assertThat(state.board().count(Color.BLACK)).isEqualTo(12);
        assertThat(state.board().count(Color.WHITE)).isEqualTo(12);
        assertThat(state.board().pieceAt(1)).contains(new Piece(Color.BLACK, PieceType.MAN));
        assertThat(state.board().pieceAt(32)).contains(new Piece(Color.WHITE, PieceType.MAN));
        assertThat(state.board().pieceAt(16)).isEmpty();
    }

    @Test
    void blackHasSevenOpeningMoves() {
        assertThat(notations(rules.legalMoves(rules.initial())))
                .containsExactly("10-14", "10-15", "11-15", "11-16", "12-16", "9-13", "9-14");
    }

    @Nested
    class Men {

        @Test
        void moveForwardOnly() {
            var state = Fen.read("B:W:B14");
            assertThat(notations(rules.legalMoves(state))).containsExactly("14-17", "14-18");
            var white = Fen.read("W:W19:B");
            assertThat(notations(rules.legalMoves(white))).containsExactly("19-15", "19-16");
        }

        @Test
        void cannotCaptureBackwards() {
            var state = Fen.read("B:W14:B18");
            assertThat(rules.legalMoves(state)).noneMatch(Move::isCapture);
        }

        @Test
        void promoteOnReachingTheFarRow() {
            var state = rules.apply(Fen.read("B:W1:B27"), 27, List.of(31));
            assertThat(state.board().pieceAt(31)).contains(new Piece(Color.BLACK, PieceType.KING));
        }
    }

    @Nested
    class Captures {

        @Test
        void captureIsMandatory() {
            // Black on 14 can jump 18; the quiet move 9-13 is then illegal.
            var state = Fen.read("B:W18:B9,14");
            assertThat(notations(rules.legalMoves(state))).containsExactly("14x23");
            assertThatThrownBy(() -> rules.apply(state, 9, List.of(13)))
                    .isInstanceOf(IllegalMoveException.class)
                    .hasMessage("Capture is mandatory");
        }

        @Test
        void multiJumpMustBeCompleted() {
            // 1x10x19 over 6 and 15.
            var state = Fen.read("B:W6,15:B1");
            assertThat(notations(rules.legalMoves(state))).containsExactly("1x10x19");
            assertThatThrownBy(() -> rules.apply(state, 1, List.of(10)))
                    .isInstanceOf(IllegalMoveException.class)
                    .hasMessage("The capture sequence must be completed");

            var after = rules.apply(state, 1, List.of(10, 19));
            assertThat(after.board().count(Color.WHITE)).isZero();
            assertThat(after.board().pieceAt(19)).isPresent();
        }

        @Test
        void playerMayChooseBetweenCaptureSequences() {
            // Black 14 can take 17 (landing 21) or 18 (landing 23).
            var state = Fen.read("B:W17,18:B14");
            assertThat(notations(rules.legalMoves(state))).containsExactly("14x21", "14x23");
        }

        @Test
        void manCrownedDuringCaptureStopsThere() {
            // Black 22 jumps 26 to 31 and is crowned; the white piece on 27 must not be taken in the same turn.
            var state = Fen.read("B:W26,27:B22");
            assertThat(notations(rules.legalMoves(state))).containsExactly("22x31");
            var after = rules.apply(state, 22, List.of(31));
            assertThat(after.board().pieceAt(31)).contains(new Piece(Color.BLACK, PieceType.KING));
            assertThat(after.board().pieceAt(27)).isPresent();
        }

        @Test
        void kingCapturesInEveryDirection() {
            var state = Fen.read("B:W10,11,18,19:BK15");
            assertThat(notations(rules.legalMoves(state)))
                    .containsExactlyInAnyOrder("15x6", "15x8", "15x22", "15x24");
        }

        @Test
        void aPieceCannotBeJumpedTwice() {
            // A king circling 10 -> 6 -> ... could revisit; every sequence must capture distinct pieces.
            var state = Fen.read("B:W6,7,14,15:BK10");
            assertThat(rules.legalMoves(state)).allSatisfy(m ->
                    assertThat(m.captured()).doesNotHaveDuplicates());
        }
    }

    @Nested
    class Kings {

        @Test
        void moveOneSquareInAnyDirection() {
            var state = Fen.read("B:W:BK14");
            assertThat(notations(rules.legalMoves(state)))
                    .containsExactlyInAnyOrder("14-17", "14-18", "14-10", "14-9");
        }
    }

    @Nested
    class EndOfGame {

        @Test
        void sideWithoutPiecesLoses() {
            var state = rules.apply(Fen.read("B:W18:B14"), 14, List.of(23));
            assertThat(rules.status(state)).isEqualTo(new GameStatus.Won(Color.BLACK, GameStatus.Reason.NO_PIECES));
            assertThat(rules.legalMoves(state)).isEmpty();
        }

        @Test
        void sideWithoutMovesLoses() {
            // White man on 29 is blocked by black men on 25 and 22 (which protects 25).
            var state = Fen.read("W:W29:B25,22");
            assertThat(rules.status(state)).isEqualTo(new GameStatus.Won(Color.BLACK, GameStatus.Reason.NO_MOVES));
        }

        @Test
        void drawAfterMoveLimitWithoutCaptureOrManMove() {
            var start = Fen.read("B:WK32:BK1");
            var state = new GameState(start.board(), start.turn(), EnglishDraughts.MOVE_LIMIT_PLIES - 1,
                    start.repetitions());
            assertThat(rules.status(state)).isEqualTo(GameStatus.ONGOING);

            state = rules.apply(state, 1, List.of(5));

            assertThat(rules.status(state)).isEqualTo(new GameStatus.Drawn(GameStatus.Reason.MOVE_LIMIT));
        }

        @Test
        void drawOnThreefoldRepetition() {
            var state = Fen.read("B:WK32:BK1");
            int[][] shuffle = {{1, 5}, {32, 28}, {5, 1}, {28, 32}};
            for (int round = 0; round < 2; round++) {
                for (int[] m : shuffle) {
                    state = rules.apply(state, m[0], List.of(m[1]));
                }
            }
            assertThat(rules.status(state)).isEqualTo(new GameStatus.Drawn(GameStatus.Reason.REPETITION));
        }

        @Test
        void manMoveResetsQuietCounter() {
            var state = rules.apply(rules.initial(), 11, List.of(15));
            assertThat(state.quietPlies()).isZero();
            assertThat(state.repetitions()).hasSize(1);
        }
    }

    @Test
    void fenRoundTrips() {
        var fen = "W:W18,K30:B1,K14";
        var state = Fen.read(fen);
        assertThat(Fen.write(state.board(), state.turn())).isEqualTo(fen);
    }

    @Test
    void notationParsesBothSeparators() {
        assertThat(Notation.parseSquares("11-15")).containsExactly(11, 15);
        assertThat(Notation.parseSquares("1x10x19")).containsExactly(1, 10, 19);
    }
}
