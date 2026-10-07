package com.draughts.domain.game;

import com.draughts.domain.engine.Color;
import com.draughts.domain.engine.GameState;
import com.draughts.domain.engine.GameStatus;
import com.draughts.domain.engine.IllegalMoveException;
import com.draughts.domain.engine.Move;
import com.draughts.domain.engine.RuleSet;
import lombok.Builder;
import lombok.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Game aggregate root. Immutable: every behaviour method checks the game's invariants and returns a new
 * instance with an incremented version. The position is derived from the move log, the source of truth.
 *
 * @param aiColor      side played by the computer, or {@code null} in a game between two people
 * @param drawOfferedBy side with a pending draw offer, if any
 */
@Builder(toBuilder = true)
public record Game(
        @NonNull UUID id,
        @NonNull String variant,
        @NonNull Lifecycle lifecycle,
        UUID blackPlayer,
        UUID whitePlayer,
        Color aiColor,
        Difficulty aiDifficulty,
        @NonNull List<PlayedMove> moves,
        @NonNull GameState state,
        GameResult result,
        ResultReason resultReason,
        Color drawOfferedBy,
        int version,
        @NonNull Instant createdAt,
        @NonNull Instant updatedAt) {

    public Game {
        moves = List.copyOf(moves);
    }

    /** A game against the computer starts immediately. */
    public static Game againstComputer(UUID id, RuleSet rules, UUID player, Color playerColor,
                                       Difficulty difficulty, Instant now) {
        return start(id, rules, player, playerColor, now)
                .lifecycle(Lifecycle.IN_PROGRESS)
                .aiColor(playerColor.opposite())
                .aiDifficulty(difficulty)
                .build();
    }

    /** A game between two people stays open until someone joins. */
    public static Game open(UUID id, RuleSet rules, UUID creator, Color creatorColor, Instant now) {
        return start(id, rules, creator, creatorColor, now).lifecycle(Lifecycle.OPEN).build();
    }

    private static GameBuilder start(UUID id, RuleSet rules, UUID player, Color color, Instant now) {
        return builder()
                .id(id)
                .variant(rules.variant())
                .blackPlayer(color == Color.BLACK ? player : null)
                .whitePlayer(color == Color.WHITE ? player : null)
                .moves(List.of())
                .state(rules.initial())
                .createdAt(now)
                .updatedAt(now);
    }

    // ---- queries -------------------------------------------------------------------------------

    public Opponent opponent() {
        return aiColor != null ? Opponent.AI : Opponent.HUMAN;
    }

    public Color turn() {
        return state.turn();
    }

    public Optional<Color> colorOf(UUID playerId) {
        if (playerId != null && playerId.equals(blackPlayer)) {
            return Optional.of(Color.BLACK);
        }
        if (playerId != null && playerId.equals(whitePlayer)) {
            return Optional.of(Color.WHITE);
        }
        return Optional.empty();
    }

    public boolean isComputerTurn() {
        return lifecycle == Lifecycle.IN_PROGRESS && aiColor == state.turn();
    }

    // ---- behaviour -----------------------------------------------------------------------------

    public Game join(UUID playerId, Instant now) {
        if (lifecycle != Lifecycle.OPEN) {
            throw GameException.invalidState("Game is not open for joining");
        }
        if (colorOf(playerId).isPresent()) {
            throw GameException.invalidState("You cannot join your own game");
        }
        var next = next(now).lifecycle(Lifecycle.IN_PROGRESS);
        return (blackPlayer == null ? next.blackPlayer(playerId) : next.whitePlayer(playerId)).build();
    }

    /** A person's move; {@code expectedVersion} may be {@code null} to skip the staleness check. */
    public Game play(RuleSet rules, UUID playerId, int from, List<Integer> path, Integer expectedVersion,
                     Instant now) {
        if (expectedVersion != null && expectedVersion != version) {
            throw GameException.conflict("Game has changed; expected version " + expectedVersion + " but is " + version);
        }
        requireInProgress();
        if (participantColor(playerId) != turn()) {
            throw GameException.forbidden("It is not your turn");
        }
        return applyMove(rules, from, path, playerId, now);
    }

    public Game playComputerMove(RuleSet rules, Move move, Instant now) {
        if (!isComputerTurn()) {
            throw GameException.invalidState("It is not the computer's turn");
        }
        return applyMove(rules, move.from(), move.path(), null, now);
    }

    public Game resign(UUID playerId, Instant now) {
        requireInProgress();
        return finish(GameResult.winFor(participantColor(playerId).opposite()), ResultReason.RESIGNATION, now);
    }

    /**
     * Offers a draw, or accepts the opponent's pending offer.
     *
     * @param computerAccepts whether the computer opponent (if any) accepts the offer
     */
    public Game offerDraw(UUID playerId, boolean computerAccepts, Instant now) {
        requireInProgress();
        var color = participantColor(playerId);
        if (drawOfferedBy == color.opposite() || computerAccepts) {
            return finish(GameResult.DRAW, ResultReason.AGREEMENT, now);
        }
        if (opponent() == Opponent.AI) {
            throw GameException.invalidState("The computer declines the draw");
        }
        return drawOfferedBy == color ? this : next(now).drawOfferedBy(color).build();
    }

    public Game declineDraw(UUID playerId, Instant now) {
        requireInProgress();
        if (drawOfferedBy != participantColor(playerId).opposite()) {
            throw GameException.invalidState("There is no draw offer to decline");
        }
        return next(now).drawOfferedBy(null).build();
    }

    // ---- internals -----------------------------------------------------------------------------

    private Game applyMove(RuleSet rules, int from, List<Integer> path, UUID playerId, Instant now) {
        Move move;
        try {
            move = rules.resolve(state, from, path);
        } catch (IllegalMoveException e) {
            throw GameException.illegalMove(e.getMessage());
        }
        var nextState = rules.apply(state, move);
        var log = new ArrayList<>(moves);
        log.add(new PlayedMove(moves.size() + 1, turn(), move, playerId, now));
        var next = next(now).moves(log).state(nextState).drawOfferedBy(null);
        switch (rules.status(nextState)) {
            case GameStatus.Won won -> next.lifecycle(Lifecycle.FINISHED)
                    .result(GameResult.winFor(won.winner()))
                    .resultReason(ResultReason.from(won.reason()));
            case GameStatus.Drawn drawn -> next.lifecycle(Lifecycle.FINISHED)
                    .result(GameResult.DRAW)
                    .resultReason(ResultReason.from(drawn.reason()));
            case GameStatus.Ongoing ongoing -> {
            }
        }
        return next.build();
    }

    private Game finish(GameResult newResult, ResultReason reason, Instant now) {
        return next(now).lifecycle(Lifecycle.FINISHED).result(newResult).resultReason(reason).drawOfferedBy(null).build();
    }

    /** Every change bumps the version used for optimistic locking. */
    private GameBuilder next(Instant now) {
        return toBuilder().version(version + 1).updatedAt(now);
    }

    private void requireInProgress() {
        if (lifecycle != Lifecycle.IN_PROGRESS) {
            throw GameException.invalidState("Game is not in progress");
        }
    }

    private Color participantColor(UUID playerId) {
        return colorOf(playerId).orElseThrow(() -> GameException.forbidden("You are not playing in this game"));
    }
}
