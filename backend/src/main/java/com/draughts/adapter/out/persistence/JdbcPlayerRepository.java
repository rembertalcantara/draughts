package com.draughts.adapter.out.persistence;

import com.draughts.application.port.out.PlayerRepository;
import com.draughts.domain.player.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JdbcPlayerRepository implements PlayerRepository {

    private final JdbcClient jdbc;

    @Override
    public void save(Player player) {
        jdbc.sql("""
                        insert into player (id, display_name, created_at) values (:id, :name, :createdAt)
                        on conflict (id) do update set display_name = excluded.display_name
                        """)
                .param("id", player.id())
                .param("name", player.displayName())
                .param("createdAt", Timestamp.from(player.createdAt()))
                .update();
    }

    @Override
    public Optional<Player> findById(UUID id) {
        return jdbc.sql("select id, display_name, created_at from player where id = :id")
                .param("id", id)
                .query((rs, n) -> new Player(rs.getObject("id", UUID.class), rs.getString("display_name"),
                        rs.getTimestamp("created_at").toInstant()))
                .optional();
    }

    @Override
    public Map<UUID, String> findDisplayNames(Collection<UUID> ids) {
        var names = new HashMap<UUID, String>();
        if (!ids.isEmpty()) {
            jdbc.sql("select id, display_name from player where id in (:ids)")
                    .param("ids", ids)
                    .query(rs -> {
                        names.put(rs.getObject("id", UUID.class), rs.getString("display_name"));
                    });
        }
        return names;
    }

    @Override
    public int deleteWithoutGamesCreatedBefore(Instant cutoff) {
        return jdbc.sql("""
                        delete from player p
                        where p.created_at < :cutoff
                          and not exists (select 1 from game g where g.black_player_id = p.id or g.white_player_id = p.id)
                          and not exists (select 1 from game_move m where m.played_by = p.id)
                        """)
                .param("cutoff", Timestamp.from(cutoff))
                .update();
    }
}
