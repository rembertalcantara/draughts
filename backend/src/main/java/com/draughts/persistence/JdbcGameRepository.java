package com.draughts.persistence;

import com.draughts.ai.Difficulty;
import com.draughts.engine.Color;
import com.draughts.engine.Fen;
import com.draughts.engine.Move;
import com.draughts.game.Game;
import com.draughts.game.GameConflictException;
import com.draughts.game.GameRepository;
import com.draughts.game.GameResult;
import com.draughts.game.GameSummary;
import com.draughts.game.Lifecycle;
import com.draughts.game.PlayedMove;
import com.draughts.game.ResultReason;
import com.draughts.game.Variants;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
class JdbcGameRepository implements GameRepository {

    private static final String SUMMARY_COLUMNS = """
            g.id, g.variant, g.status, g.black_player_id, g.white_player_id, g.ai_color, g.ai_difficulty,
            g.current_turn, g.result, g.result_reason, g.created_at, g.updated_at,
            (select count(*) from game_move m where m.game_id = g.id) as move_count
            """;

    private final JdbcClient jdbc;
    private final Variants variants;

    JdbcGameRepository(JdbcClient jdbc, Variants variants) {
        this.jdbc = jdbc;
        this.variants = variants;
    }

    @Override
    public void insert(Game game) {
        jdbc.sql("""
                        insert into game (id, variant, status, black_player_id, white_player_id, ai_color, ai_difficulty,
                                          current_turn, board_snapshot, result, result_reason, draw_offered_by,
                                          version, created_at, updated_at)
                        values (:id, :variant, :status, :black, :white, :aiColor, :aiDifficulty,
                                :turn, cast(:snapshot as jsonb), :result, :reason, :drawOfferedBy,
                                :version, :createdAt, :updatedAt)
                        """)
                .paramSource(params(game))
                .update();
        insertMoves(game.id(), game.moves());
    }

    @Override
    public void update(Game game, int expectedVersion, List<PlayedMove> newMoves) {
        var params = params(game);
        params.addValue("expectedVersion", expectedVersion);
        int updated = jdbc.sql("""
                        update game set status = :status, black_player_id = :black, white_player_id = :white,
                                        current_turn = :turn, board_snapshot = cast(:snapshot as jsonb),
                                        result = :result, result_reason = :reason, draw_offered_by = :drawOfferedBy,
                                        version = :version, updated_at = :updatedAt
                        where id = :id and version = :expectedVersion
                        """)
                .paramSource(params)
                .update();
        if (updated == 0) {
            throw new GameConflictException("Game " + game.id() + " was modified concurrently");
        }
        insertMoves(game.id(), newMoves);
    }

    @Override
    public Optional<Game> findById(UUID id) {
        var header = jdbc.sql("select * from game where id = :id").param("id", id)
                .query((rs, n) -> new Header(rs))
                .optional();
        return header.map(h -> {
            var moves = jdbc.sql("""
                            select ply, from_sq, path, captured, played_by, played_at
                            from game_move where game_id = :id order by ply
                            """)
                    .param("id", id)
                    .query((rs, n) -> new PlayedMove(
                            rs.getInt("ply"),
                            n % 2 == 0 ? Color.BLACK : Color.WHITE,
                            new Move(rs.getInt("from_sq"), ints(rs.getArray("path")), ints(rs.getArray("captured"))),
                            rs.getObject("played_by", UUID.class),
                            rs.getTimestamp("played_at").toInstant()))
                    .list();
            return Game.restore(variants.forVariant(h.variant), h.id, h.status, h.black, h.white, h.aiColor,
                    h.aiDifficulty, moves, h.result, h.reason, h.drawOfferedBy, h.version, h.createdAt, h.updatedAt);
        });
    }

    @Override
    public List<GameSummary> findOpen(int limit) {
        return jdbc.sql("select " + SUMMARY_COLUMNS + " from game g where g.status = 'OPEN' order by g.created_at desc limit :limit")
                .param("limit", limit)
                .query((rs, n) -> summary(rs))
                .list();
    }

    @Override
    public List<GameSummary> findByPlayer(UUID playerId, int limit) {
        return jdbc.sql("select " + SUMMARY_COLUMNS + """
                         from game g where g.black_player_id = :player or g.white_player_id = :player
                        order by g.updated_at desc limit :limit
                        """)
                .param("player", playerId)
                .param("limit", limit)
                .query((rs, n) -> summary(rs))
                .list();
    }

    @Override
    public List<UUID> findAwaitingAi(int limit) {
        return jdbc.sql("""
                        select id from game where status = 'IN_PROGRESS' and ai_color = current_turn
                        order by updated_at limit :limit
                        """)
                .param("limit", limit)
                .query(UUID.class)
                .list();
    }

    private void insertMoves(UUID gameId, List<PlayedMove> moves) {
        for (var played : moves) {
            jdbc.sql("""
                            insert into game_move (game_id, ply, from_sq, path, captured, played_by, played_at)
                            values (:gameId, :ply, :from, cast(:path as smallint[]), cast(:captured as smallint[]),
                                    :playedBy, :playedAt)
                            """)
                    .param("gameId", gameId)
                    .param("ply", played.ply())
                    .param("from", played.move().from())
                    .param("path", arrayLiteral(played.move().path()))
                    .param("captured", arrayLiteral(played.move().captured()))
                    .param("playedBy", played.playedBy())
                    .param("playedAt", Timestamp.from(played.playedAt()))
                    .update();
        }
    }

    private static MapSqlParameterSource params(Game game) {
        var p = new MapSqlParameterSource();
        p.addValue("id", game.id());
        p.addValue("variant", game.variant());
        p.addValue("status", game.lifecycle().name());
        p.addValue("black", game.blackPlayer());
        p.addValue("white", game.whitePlayer());
        p.addValue("aiColor", name(game.aiColor()));
        p.addValue("aiDifficulty", name(game.aiDifficulty()));
        p.addValue("turn", game.turn().name());
        p.addValue("snapshot", snapshot(game));
        p.addValue("result", name(game.result()));
        p.addValue("reason", name(game.resultReason()));
        p.addValue("drawOfferedBy", name(game.drawOfferedBy()));
        p.addValue("version", game.version());
        p.addValue("createdAt", Timestamp.from(game.createdAt()));
        p.addValue("updatedAt", Timestamp.from(game.updatedAt()));
        return p;
    }

    /** Derived board snapshot for fast reads and ad-hoc queries; always rebuildable from the move log. */
    private static String snapshot(Game game) {
        var board = game.state().board();
        return "{\"fen\":\"%s\",\"black\":%d,\"white\":%d,\"blackKings\":%d,\"whiteKings\":%d}".formatted(
                Fen.write(board, game.turn()),
                board.count(Color.BLACK), board.count(Color.WHITE),
                board.kingCount(Color.BLACK), board.kingCount(Color.WHITE));
    }

    private static GameSummary summary(ResultSet rs) throws SQLException {
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
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private static String arrayLiteral(List<Integer> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(",", "{", "}"));
    }

    private static List<Integer> ints(Array array) throws SQLException {
        var values = new ArrayList<Integer>();
        for (Object o : (Object[]) array.getArray()) {
            values.add(((Number) o).intValue());
        }
        return values;
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    /** Row of the {@code game} table. */
    private static final class Header {
        final UUID id;
        final String variant;
        final Lifecycle status;
        final UUID black;
        final UUID white;
        final Color aiColor;
        final Difficulty aiDifficulty;
        final GameResult result;
        final ResultReason reason;
        final Color drawOfferedBy;
        final int version;
        final Instant createdAt;
        final Instant updatedAt;

        Header(ResultSet rs) throws SQLException {
            id = rs.getObject("id", UUID.class);
            variant = rs.getString("variant");
            status = Lifecycle.valueOf(rs.getString("status"));
            black = rs.getObject("black_player_id", UUID.class);
            white = rs.getObject("white_player_id", UUID.class);
            aiColor = enumOrNull(Color.class, rs.getString("ai_color"));
            aiDifficulty = enumOrNull(Difficulty.class, rs.getString("ai_difficulty"));
            result = enumOrNull(GameResult.class, rs.getString("result"));
            reason = enumOrNull(ResultReason.class, rs.getString("result_reason"));
            drawOfferedBy = enumOrNull(Color.class, rs.getString("draw_offered_by"));
            version = rs.getInt("version");
            createdAt = rs.getTimestamp("created_at").toInstant();
            updatedAt = rs.getTimestamp("updated_at").toInstant();
        }
    }
}
