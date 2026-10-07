package com.draughts.adapter.out.persistence;

import com.draughts.application.port.out.GameRepository;
import com.draughts.domain.game.Game;
import com.draughts.domain.game.GameException;
import com.draughts.domain.game.GameSummary;
import com.draughts.domain.game.PlayedMove;
import com.draughts.domain.game.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JdbcGameRepository implements GameRepository {

    private static final String SUMMARY_SELECT = """
            select g.id, g.variant, g.status, g.black_player_id, g.white_player_id, g.ai_color, g.ai_difficulty,
                   g.current_turn, g.result, g.result_reason, g.created_at, g.updated_at,
                   (select count(*) from game_move m where m.game_id = g.id) as move_count
            from game g
            """;

    private final JdbcClient jdbc;
    private final Variants variants;

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
                .paramSource(GameRows.params(game))
                .update();
        insertMoves(game.id(), game.moves());
    }

    @Override
    public void update(Game previous, Game current) {
        int updated = jdbc.sql("""
                        update game set status = :status, black_player_id = :black, white_player_id = :white,
                                        current_turn = :turn, board_snapshot = cast(:snapshot as jsonb),
                                        result = :result, result_reason = :reason, draw_offered_by = :drawOfferedBy,
                                        version = :version, updated_at = :updatedAt
                        where id = :id and version = :expectedVersion
                        """)
                .paramSource(GameRows.params(current).addValue("expectedVersion", previous.version()))
                .update();
        if (updated == 0) {
            throw GameException.conflict("Game " + current.id() + " was modified concurrently");
        }
        insertMoves(current.id(), current.moves().subList(previous.moves().size(), current.moves().size()));
    }

    @Override
    public Optional<Game> findById(UUID id) {
        return jdbc.sql("select * from game where id = :id")
                .param("id", id)
                .query((rs, n) -> new Header(rs.getString("variant"), GameRows.header(rs)))
                .optional()
                .map(header -> {
                    var moves = jdbc.sql("select * from game_move where game_id = :id order by ply")
                            .param("id", id)
                            .query(GameRows::move)
                            .list();
                    var state = variants.forVariant(header.variant()).replay(moves.stream().map(PlayedMove::move).toList());
                    return header.game().moves(moves).state(state).build();
                });
    }

    @Override
    public List<GameSummary> findOpen(int limit) {
        return jdbc.sql(SUMMARY_SELECT + " where g.status = 'OPEN' order by g.created_at desc limit :limit")
                .param("limit", limit)
                .query(GameRows::summary)
                .list();
    }

    @Override
    public List<GameSummary> findByPlayer(UUID playerId, int limit) {
        return jdbc.sql(SUMMARY_SELECT + """
                         where g.black_player_id = :player or g.white_player_id = :player
                        order by g.updated_at desc limit :limit
                        """)
                .param("player", playerId)
                .param("limit", limit)
                .query(GameRows::summary)
                .list();
    }

    @Override
    public List<UUID> findAwaitingComputer(int limit) {
        return jdbc.sql("""
                        select id from game where status = 'IN_PROGRESS' and ai_color = current_turn
                        order by updated_at limit :limit
                        """)
                .param("limit", limit)
                .query(UUID.class)
                .list();
    }

    private record Header(String variant, Game.GameBuilder game) {
    }

    private void insertMoves(UUID gameId, List<PlayedMove> moves) {
        for (var played : moves) {
            jdbc.sql("""
                            insert into game_move (game_id, ply, from_sq, path, captured, played_by, played_at)
                            values (:gameId, :ply, :from, cast(:path as smallint[]), cast(:captured as smallint[]),
                                    :playedBy, :playedAt)
                            """)
                    .paramSource(GameRows.params(gameId, played))
                    .update();
        }
    }
}
