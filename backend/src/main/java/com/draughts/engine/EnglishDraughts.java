package com.draughts.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * English draughts / American checkers: 8x8 board, men move and capture forward only, kings move one
 * square in any direction, captures are mandatory and multi-jumps must be completed.
 */
public final class EnglishDraughts implements RuleSet {

    public static final String VARIANT = "ENGLISH";

    /** 40 moves per side (80 plies) without a capture or a man move is a draw. */
    public static final int MOVE_LIMIT_PLIES = 80;

    @Override
    public String variant() {
        return VARIANT;
    }

    @Override
    public GameState initial() {
        return GameState.of(Board.initial(), Color.BLACK);
    }

    @Override
    public List<Move> legalMoves(GameState state) {
        if (status(state).isOver()) {
            return List.of();
        }
        return MoveGenerator.legalMoves(state.board(), state.turn());
    }

    @Override
    public Move resolve(GameState state, int from, List<Integer> path) {
        return legalMoves(state).stream()
                .filter(m -> m.matches(from, path))
                .findFirst()
                .orElseThrow(() -> new IllegalMoveException(describeIllegal(state, from, path)));
    }

    @Override
    public GameState apply(GameState state, int from, List<Integer> path) {
        var move = resolve(state, from, path);
        boolean manMoved = state.board().pieceAt(move.from()).map(p -> !p.isKing()).orElse(false);
        var board = state.board().apply(move, state.turn());
        var next = state.turn().opposite();
        long hash = Zobrist.hash(board, next);
        boolean irreversible = move.isCapture() || manMoved;
        if (irreversible) {
            return new GameState(board, next, 0, List.of(hash));
        }
        var repetitions = new ArrayList<>(state.repetitions());
        repetitions.add(hash);
        return new GameState(board, next, state.quietPlies() + 1, repetitions);
    }

    @Override
    public GameStatus status(GameState state) {
        var board = state.board();
        var turn = state.turn();
        if (board.count(turn) == 0) {
            return new GameStatus.Won(turn.opposite(), GameStatus.Reason.NO_PIECES);
        }
        if (!MoveGenerator.hasMoves(board, turn)) {
            return new GameStatus.Won(turn.opposite(), GameStatus.Reason.NO_MOVES);
        }
        if (state.quietPlies() >= MOVE_LIMIT_PLIES) {
            return new GameStatus.Drawn(GameStatus.Reason.MOVE_LIMIT);
        }
        if (state.occurrencesOfCurrentPosition() >= 3) {
            return new GameStatus.Drawn(GameStatus.Reason.REPETITION);
        }
        return GameStatus.ONGOING;
    }

    private static String describeIllegal(GameState state, int from, List<Integer> path) {
        var legal = MoveGenerator.legalMoves(state.board(), state.turn());
        boolean isPrefix = legal.stream().anyMatch(m -> m.from() == from
                && m.path().size() > path.size()
                && m.path().subList(0, path.size()).equals(path));
        if (isPrefix) {
            return "The capture sequence must be completed";
        }
        if (!legal.isEmpty() && legal.getFirst().isCapture()) {
            return "Capture is mandatory";
        }
        return "Illegal move";
    }
}
