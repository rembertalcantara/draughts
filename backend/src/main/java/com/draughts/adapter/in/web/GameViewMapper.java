package com.draughts.adapter.in.web;

import com.draughts.adapter.in.web.dto.ApiDtos.GameSummaryView;
import com.draughts.adapter.in.web.dto.ApiDtos.GameView;
import com.draughts.adapter.in.web.dto.ApiDtos.MoveView;
import com.draughts.adapter.in.web.dto.ApiDtos.PieceView;
import com.draughts.adapter.in.web.dto.ApiDtos.PlayedMoveView;
import com.draughts.adapter.in.web.dto.ApiDtos.PlayerView;
import com.draughts.application.port.in.GameQuery;
import com.draughts.application.port.in.PlayerUseCase;
import com.draughts.domain.engine.Color;
import com.draughts.domain.engine.Fen;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.GameSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/** Maps domain games to API views. */
@Component
@RequiredArgsConstructor
class GameViewMapper {

    private final GameQuery games;
    private final PlayerUseCase players;

    /** @param viewer the requesting player, or {@code null} for a broadcast */
    GameView toView(Game game, UUID viewer) {
        var names = names(Stream.of(game.blackPlayer(), game.whitePlayer()));
        var state = game.state();
        return new GameView(game.id(), game.variant(), game.lifecycle(), game.opponent(), game.aiDifficulty(),
                game.turn(), game.version(), Fen.write(state.board(), game.turn()),
                state.board().pieces().entrySet().stream()
                        .map(e -> new PieceView(e.getKey(), e.getValue().color(), e.getValue().isKing()))
                        .toList(),
                games.legalMoves(game).stream()
                        .map(m -> new MoveView(m.from(), m.path(), m.captured(), m.notation()))
                        .toList(),
                game.moves().stream()
                        .map(p -> new PlayedMoveView(p.ply(), p.color(), p.move().from(), p.move().path(),
                                p.move().captured(), p.move().notation()))
                        .toList(),
                player(game.blackPlayer(), Color.BLACK, game.aiColor(), names),
                player(game.whitePlayer(), Color.WHITE, game.aiColor(), names),
                game.colorOf(viewer).orElse(null), game.drawOfferedBy(), game.result(), game.resultReason(),
                game.createdAt(), game.updatedAt());
    }

    List<GameSummaryView> toSummaries(Collection<GameSummary> summaries, UUID viewer) {
        var names = names(summaries.stream().flatMap(g -> Stream.of(g.blackPlayer(), g.whitePlayer())));
        return summaries.stream().map(g -> new GameSummaryView(
                g.id(), g.variant(), g.lifecycle(), g.opponent(), g.aiDifficulty(),
                player(g.blackPlayer(), Color.BLACK, g.aiColor(), names),
                player(g.whitePlayer(), Color.WHITE, g.aiColor(), names),
                g.colorOf(viewer).orElse(null), g.turn(), g.result(), g.resultReason(), g.moveCount(),
                g.createdAt(), g.updatedAt())).toList();
    }

    private Map<UUID, String> names(Stream<UUID> playerIds) {
        return players.displayNames(playerIds.filter(Objects::nonNull).distinct().toList());
    }

    private static PlayerView player(UUID id, Color side, Color aiColor, Map<UUID, String> names) {
        if (side == aiColor) {
            return new PlayerView("Computer", true);
        }
        return id == null ? null : new PlayerView(names.getOrDefault(id, "Unknown"), false);
    }
}
