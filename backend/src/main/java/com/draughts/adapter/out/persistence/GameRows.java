package com.draughts.adapter.out.persistence;

import com.draughts.domain.engine.Color;
import com.draughts.domain.engine.Fen;
import com.draughts.domain.engine.Move;
import com.draughts.domain.game.Difficulty;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.GameResult;
import com.draughts.domain.game.GameSummary;
import com.draughts.domain.game.Lifecycle;
import com.draughts.domain.game.PlayedMove;
import com.draughts.domain.game.ResultReason;
import lombok.experimental.UtilityClass;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Mapping between the {@code game} / {@code game_move} tables and domain objects. */
@UtilityClass
class GameRows {

    MapSqlParameterSource params(Game game) {
        return new MapSqlParameterSource()
                .addValue("id", game.id())
                .addValue("variant", game.variant())
                .addValue("status", game.lifecycle().name())
                .addValue("black", game.blackPlayer())
                .addValue("white", game.whitePlayer())
                .addValue("aiColor", name(game.aiColor()))
                .addValue("aiDifficulty", name(game.aiDifficulty()))
                .addValue("turn", game.turn().name())
                .addValue("snapshot", snapshot(game))
                .addValue("result", name(game.result()))
                .addValue("reason", name(game.resultReason()))
                .addValue("drawOfferedBy", name(game.drawOfferedBy()))
                .addValue("version", game.version())
                .addValue("createdAt", Timestamp.from(game.createdAt()))
                .addValue("updatedAt", Timestamp.from(game.updatedAt()));
    }

    MapSqlParameterSource params(UUID gameId, PlayedMove played) {
        return new MapSqlParameterSource()
                .addValue("gameId", gameId)
                .addValue("ply", played.ply())
                .addValue("from", played.move().from())
                .addValue("path", arrayLiteral(played.move().path()))
                .addValue("captured", arrayLiteral(played.move().captured()))
                .addValue("playedBy", played.playedBy())
                .addValue("playedAt", Timestamp.from(played.playedAt()));
    }

    /** Game header without its move log and position, which the repository adds. */
    Game.GameBuilder header(ResultSet rs) throws SQLException {
        return Game.builder()
                .id(rs.getObject("id", UUID.class))
                .variant(rs.getString("variant"))
                .lifecycle(Lifecycle.valueOf(rs.getString("status")))
                .blackPlayer(rs.getObject("black_player_id", UUID.class))
                .whitePlayer(rs.getObject("white_player_id", UUID.class))
                .aiColor(enumOrNull(Color.class, rs.getString("ai_color")))
                .aiDifficulty(enumOrNull(Difficulty.class, rs.getString("ai_difficulty")))
                .result(enumOrNull(GameResult.class, rs.getString("result")))
                .resultReason(enumOrNull(ResultReason.class, rs.getString("result_reason")))
                .drawOfferedBy(enumOrNull(Color.class, rs.getString("draw_offered_by")))
                .version(rs.getInt("version"))
                .createdAt(instant(rs, "created_at"))
                .updatedAt(instant(rs, "updated_at"));
    }

    /** Black always moves first, so even row numbers are Black's moves. */
    PlayedMove move(ResultSet rs, int rowNum) throws SQLException {
        return new PlayedMove(
                rs.getInt("ply"),
                rowNum % 2 == 0 ? Color.BLACK : Color.WHITE,
                new Move(rs.getInt("from_sq"), ints(rs.getArray("path")), ints(rs.getArray("captured"))),
                rs.getObject("played_by", UUID.class),
                instant(rs, "played_at"));
    }

    GameSummary summary(ResultSet rs, int rowNum) throws SQLException {
        return new GameSummary(
                rs.getObject("id", UUID.class),
                rs.getString("variant"),
                Lifecycle.valueOf(rs.getString("status")),
                rs.getObject("black_player_id", UUID.class),
                rs.getObject("white_player_id", UUID.class),
                enumOrNull(Color.class, rs.getString("ai_color")),
                enumOrNull(Difficulty.class, rs.getString("ai_difficulty")),
                Color.valueOf(rs.getString("current_turn")),
                enumOrNull(GameResult.class, rs.getString("result")),
                enumOrNull(ResultReason.class, rs.getString("result_reason")),
                rs.getInt("move_count"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    /** Derived board snapshot for quick reads and ad-hoc queries; always rebuildable from the move log. */
    private String snapshot(Game game) {
        var board = game.state().board();
        return "{\"fen\":\"%s\",\"black\":%d,\"white\":%d,\"blackKings\":%d,\"whiteKings\":%d}".formatted(
                Fen.write(board, game.turn()),
                board.count(Color.BLACK), board.count(Color.WHITE),
                board.kingCount(Color.BLACK), board.kingCount(Color.WHITE));
    }

    private String arrayLiteral(List<Integer> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(",", "{", "}"));
    }

    private List<Integer> ints(Array array) throws SQLException {
        var values = new ArrayList<Integer>();
        for (Object o : (Object[]) array.getArray()) {
            values.add(((Number) o).intValue());
        }
        return values;
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
