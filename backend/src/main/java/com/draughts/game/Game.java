package com.draughts.game;

import com.draughts.ai.Difficulty;
import com.draughts.engine.Color;
import com.draughts.engine.GameState;
import com.draughts.engine.GameStatus;
import com.draughts.engine.RuleSet;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Game aggregate. Immutable: every change returns a new instance with an incremented version.
 * The position is always derived from the move log, which is the source of truth.
 */
public record Game(
        UUID id,
        String variant,
        Lifecycle lifecycle,
        UUID blackPlayer,
        UUID whitePlayer,
        Color aiColor,
        Difficulty aiDifficulty,
        List<PlayedMove> moves,
        GameState state,
        GameResult result,
        ResultReason resultReason,
        Color drawOfferedBy,
        int version,
        Instant createdAt,
        Instant updatedAt) {

    public Game {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(lifecycle, "lifecycle");
        Objects.requireNonNull(state, "state");
        moves = List.copyOf(moves);
    }

    /** Rebuilds a game from its stored header and move log by replaying every move. */
    public static Game restore(RuleSet rules, UUID id, Lifecycle lifecycle, UUID blackPlayer, UUID whitePlayer,
                               Color aiColor, Difficulty aiDifficulty, List<PlayedMove> moves, GameResult result,
                               ResultReason resultReason, Color drawOfferedBy, int version,
                               Instant createdAt, Instant updatedAt) {
        var state = rules.initial();
        for (var played : moves) {
            state = rules.apply(state, played.move());
        }
        return new Game(id, rules.variant(), lifecycle, blackPlayer, whitePlayer, aiColor, aiDifficulty, moves,
                state, result, resultReason, drawOfferedBy, version, createdAt, updatedAt);
    }

    public Opponent opponent() {
        return aiColor != null ? Opponent.AI : Opponent.HUMAN;
    }

    public Color turn() {
        return state.turn();
    }

    public Optional<Color> colorOf(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        if (playerId.equals(blackPlayer)) {
            return Optional.of(Color.BLACK);
        }
        if (playerId.equals(whitePlayer)) {
            return Optional.of(Color.WHITE);
        }
        return Optional.empty();
    }

    public UUID playerOf(Color color) {
        return color == Color.BLACK ? blackPlayer : whitePlayer;
    }

    public boolean isAiTurn() {
        return lifecycle == Lifecycle.IN_PROGRESS && aiColor == state.turn();
    }

    Game withMove(PlayedMove played, GameState next, GameStatus status, Instant now) {
        var log = new ArrayList<>(moves);
        log.add(played);
        var finished = status.isOver();
        GameResult newResult = null;
        ResultReason reason = null;
        switch (status) {
            case GameStatus.Won won -> {
                newResult = GameResult.winFor(won.winner());
                reason = ResultReason.from(won.reason());
            }
            case GameStatus.Drawn drawn -> {
                newResult = GameResult.DRAW;
                reason = ResultReason.from(drawn.reason());
            }
            case GameStatus.Ongoing ongoing -> {
            }
        }
        return new Game(id, variant, finished ? Lifecycle.FINISHED : lifecycle, blackPlayer, whitePlayer, aiColor,
                aiDifficulty, log, next, newResult, reason, null, version + 1, createdAt, now);
    }

    Game withSecondPlayer(Color color, UUID playerId, Instant now) {
        return new Game(id, variant, Lifecycle.IN_PROGRESS,
                color == Color.BLACK ? playerId : blackPlayer,
                color == Color.WHITE ? playerId : whitePlayer,
                aiColor, aiDifficulty, moves, state, result, resultReason, drawOfferedBy, version + 1, createdAt, now);
    }

    Game finished(GameResult newResult, ResultReason reason, Instant now) {
        return new Game(id, variant, Lifecycle.FINISHED, blackPlayer, whitePlayer, aiColor, aiDifficulty, moves, state,
                newResult, reason, null, version + 1, createdAt, now);
    }

    Game withDrawOffer(Color offeredBy, Instant now) {
        return new Game(id, variant, lifecycle, blackPlayer, whitePlayer, aiColor, aiDifficulty, moves, state, result,
                resultReason, offeredBy, version + 1, createdAt, now);
    }
}
