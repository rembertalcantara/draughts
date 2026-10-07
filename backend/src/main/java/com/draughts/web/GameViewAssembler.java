package com.draughts.web;

import com.draughts.engine.Color;
import com.draughts.game.Game;
import com.draughts.game.GameSummary;
import com.draughts.game.Lifecycle;
import com.draughts.game.Opponent;
import com.draughts.game.Variants;
import com.draughts.player.PlayerService;
import com.draughts.web.dto.ApiDtos.GameSummaryView;
import com.draughts.web.dto.ApiDtos.GameView;
import com.draughts.web.dto.ApiDtos.MoveView;
import com.draughts.web.dto.ApiDtos.PieceView;
import com.draughts.web.dto.ApiDtos.PlayedMoveView;
import com.draughts.web.dto.ApiDtos.PlayerView;
import com.draughts.engine.Fen;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class GameViewAssembler {

    private final PlayerService players;
    private final Variants variants;

    public GameViewAssembler(PlayerService players, Variants variants) {
        this.players = players;
        this.variants = variants;
    }

    /** @param viewer the requesting player, or {@code null} for a broadcast */
    public GameView toView(Game game, UUID viewer) {
        var names = players.displayNames(Stream.of(game.blackPlayer(), game.whitePlayer())
                .filter(Objects::nonNull).toList());
        var board = game.state().board().pieces().entrySet().stream()
                .map(e -> new PieceView(e.getKey(), e.getValue().color(), e.getValue().isKing()))
                .toList();
        List<MoveView> legal = game.lifecycle() == Lifecycle.IN_PROGRESS
                ? variants.forVariant(game.variant()).legalMoves(game.state()).stream()
                .map(m -> new MoveView(m.from(), m.path(), m.captured(), m.notation()))
                .toList()
                : List.of();
        var moves = game.moves().stream()
                .map(p -> new PlayedMoveView(p.ply(), p.color(), p.move().from(), p.move().path(),
                        p.move().captured(), p.move().notation()))
                .toList();
        return new GameView(game.id(), game.variant(), game.lifecycle(), game.opponent(), game.aiDifficulty(),
                game.turn(), game.version(), Fen.write(game.state().board(), game.turn()), board, legal, moves,
                player(game.blackPlayer(), game.aiColor() == Color.BLACK, names),
                player(game.whitePlayer(), game.aiColor() == Color.WHITE, names),
                game.colorOf(viewer).orElse(null), game.drawOfferedBy(), game.result(), game.resultReason(),
                game.createdAt(), game.updatedAt());
    }

    public List<GameSummaryView> toSummaries(List<GameSummary> games, UUID viewer) {
        var ids = new HashSet<UUID>();
        games.forEach(g -> {
            if (g.blackPlayer() != null) {
                ids.add(g.blackPlayer());
            }
            if (g.whitePlayer() != null) {
                ids.add(g.whitePlayer());
            }
        });
        var names = players.displayNames(ids);
        return games.stream().map(g -> new GameSummaryView(
                g.id(), g.variant(), g.lifecycle(), g.aiColor() != null ? Opponent.AI : Opponent.HUMAN,
                g.aiDifficulty(),
                player(g.blackPlayer(), g.aiColor() == Color.BLACK, names),
                player(g.whitePlayer(), g.aiColor() == Color.WHITE, names),
                viewer == null ? null : viewer.equals(g.blackPlayer()) ? Color.BLACK
                        : viewer.equals(g.whitePlayer()) ? Color.WHITE : null,
                g.turn(), g.result(), g.resultReason(), g.moveCount(), g.createdAt(), g.updatedAt())).toList();
    }

    private static PlayerView player(UUID id, boolean computer, Map<UUID, String> names) {
        if (computer) {
            return new PlayerView("Computer", true);
        }
        return id == null ? null : new PlayerView(names.getOrDefault(id, "Unknown"), false);
    }
}
