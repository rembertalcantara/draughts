package com.draughts.game;

import com.draughts.ai.Difficulty;
import com.draughts.ai.Evaluator;
import com.draughts.ai.SearchAi;
import com.draughts.engine.Color;
import com.draughts.engine.IllegalMoveException;
import com.draughts.engine.Move;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** Application service: every state change of a game goes through here. */
@Service
public class GameService {

    /** The computer accepts a draw offer when its evaluation is at least this far behind. */
    private static final int AI_ACCEPTS_DRAW_BELOW = -100;

    private final GameRepository games;
    private final Variants variants;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final RandomGenerator random;

    public GameService(GameRepository games, Variants variants, ApplicationEventPublisher events, Clock clock) {
        this.games = games;
        this.variants = variants;
        this.events = events;
        this.clock = clock;
        this.random = RandomGenerator.getDefault();
    }

    @Transactional
    public Game create(UUID playerId, CreateGameCommand command) {
        Objects.requireNonNull(playerId, "playerId");
        var variant = command.variant() == null ? Variants.DEFAULT : command.variant();
        if (!variants.supports(variant)) {
            throw new GameException.InvalidState("Unsupported variant: " + variant);
        }
        var rules = variants.forVariant(variant);
        var color = (command.color() == null ? ColorChoice.RANDOM : command.color()).resolve(random);
        var now = clock.instant();
        Game game;
        if (command.opponent() == Opponent.AI) {
            var difficulty = command.difficulty() == null ? Difficulty.MEDIUM : command.difficulty();
            game = new Game(UUID.randomUUID(), variant, Lifecycle.IN_PROGRESS,
                    color == Color.BLACK ? playerId : null,
                    color == Color.WHITE ? playerId : null,
                    color.opposite(), difficulty, List.of(), rules.initial(), null, null, null, 0, now, now);
        } else {
            game = new Game(UUID.randomUUID(), variant, Lifecycle.OPEN,
                    color == Color.BLACK ? playerId : null,
                    color == Color.WHITE ? playerId : null,
                    null, null, List.of(), rules.initial(), null, null, null, 0, now, now);
        }
        games.insert(game);
        events.publishEvent(new GameChanged(game));
        return game;
    }

    @Transactional(readOnly = true)
    public Game get(UUID gameId) {
        return games.findById(gameId).orElseThrow(() -> new GameException.NotFound(gameId));
    }

    @Transactional(readOnly = true)
    public List<GameSummary> openGames(int limit) {
        return games.findOpen(limit);
    }

    @Transactional(readOnly = true)
    public List<GameSummary> gamesOf(UUID playerId, int limit) {
        return games.findByPlayer(playerId, limit);
    }

    @Transactional
    public Game join(UUID gameId, UUID playerId) {
        var game = get(gameId);
        if (game.lifecycle() != Lifecycle.OPEN) {
            throw new GameException.InvalidState("Game is not open for joining");
        }
        if (game.colorOf(playerId).isPresent()) {
            throw new GameException.InvalidState("You cannot join your own game");
        }
        var free = game.blackPlayer() == null ? Color.BLACK : Color.WHITE;
        return save(game, game.withSecondPlayer(free, playerId, clock.instant()), List.of());
    }

    /**
     * Plays a human move.
     *
     * @param expectedVersion the version the client based the move on, or {@code null} to skip the check
     */
    @Transactional
    public Game move(UUID gameId, UUID playerId, int from, List<Integer> path, Integer expectedVersion) {
        var game = get(gameId);
        if (expectedVersion != null && expectedVersion != game.version()) {
            throw new GameConflictException("Game has changed; expected version " + expectedVersion
                    + " but is " + game.version());
        }
        requireInProgress(game);
        var color = requireParticipant(game, playerId);
        if (color != game.turn()) {
            throw new GameException.Forbidden("It is not your turn");
        }
        return play(game, from, path, playerId);
    }

    /** Plays the computer's move if it is still the computer's turn; returns the game unchanged otherwise. */
    @Transactional
    public Game playAiMove(UUID gameId, SearchAi ai) {
        var game = get(gameId);
        if (!game.isAiTurn()) {
            return game;
        }
        var move = ai.chooseMove(game.state().board(), game.turn(), game.aiDifficulty());
        return play(game, move.from(), move.path(), null);
    }

    @Transactional
    public Game resign(UUID gameId, UUID playerId) {
        var game = get(gameId);
        requireInProgress(game);
        var color = requireParticipant(game, playerId);
        return save(game, game.finished(GameResult.winFor(color.opposite()), ResultReason.RESIGNATION,
                clock.instant()), List.of());
    }

    /** Offers a draw, or accepts the opponent's pending offer. */
    @Transactional
    public Game offerDraw(UUID gameId, UUID playerId) {
        var game = get(gameId);
        requireInProgress(game);
        var color = requireParticipant(game, playerId);
        var now = clock.instant();
        if (game.drawOfferedBy() == color.opposite()) {
            return save(game, game.finished(GameResult.DRAW, ResultReason.AGREEMENT, now), List.of());
        }
        if (game.opponent() == Opponent.AI) {
            int aiView = Evaluator.evaluate(game.state().board(), game.aiColor());
            if (aiView <= AI_ACCEPTS_DRAW_BELOW) {
                return save(game, game.finished(GameResult.DRAW, ResultReason.AGREEMENT, now), List.of());
            }
            throw new GameException.InvalidState("The computer declines the draw");
        }
        if (game.drawOfferedBy() == color) {
            return game;
        }
        return save(game, game.withDrawOffer(color, now), List.of());
    }

    @Transactional
    public Game declineDraw(UUID gameId, UUID playerId) {
        var game = get(gameId);
        requireInProgress(game);
        var color = requireParticipant(game, playerId);
        if (game.drawOfferedBy() != color.opposite()) {
            throw new GameException.InvalidState("There is no draw offer to decline");
        }
        return save(game, game.withDrawOffer(null, clock.instant()), List.of());
    }

    private Game play(Game game, int from, List<Integer> path, UUID playerId) {
        var rules = variants.forVariant(game.variant());
        Move move;
        try {
            move = rules.resolve(game.state(), from, path);
        } catch (IllegalMoveException e) {
            throw new GameException.IllegalMove(e.getMessage());
        }
        var next = rules.apply(game.state(), move);
        var now = clock.instant();
        var played = new PlayedMove(game.moves().size() + 1, game.turn(), move, playerId, now);
        return save(game, game.withMove(played, next, rules.status(next), now), List.of(played));
    }

    private Game save(Game before, Game after, List<PlayedMove> newMoves) {
        games.update(after, before.version(), newMoves);
        events.publishEvent(new GameChanged(after));
        return after;
    }

    private static void requireInProgress(Game game) {
        if (game.lifecycle() != Lifecycle.IN_PROGRESS) {
            throw new GameException.InvalidState("Game is not in progress");
        }
    }

    private static Color requireParticipant(Game game, UUID playerId) {
        return game.colorOf(playerId)
                .orElseThrow(() -> new GameException.Forbidden("You are not playing in this game"));
    }
}
