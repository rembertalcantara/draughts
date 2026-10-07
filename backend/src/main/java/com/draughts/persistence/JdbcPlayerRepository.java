package com.draughts.persistence;

import com.draughts.player.Player;
import com.draughts.player.PlayerRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcPlayerRepository implements PlayerRepository {

    private final JdbcClient jdbc;

    JdbcPlayerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(Player player) {
        jdbc.sql("insert into player (id, display_name, created_at) values (:id, :name, :createdAt)")
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
        if (ids.isEmpty()) {
            return names;
        }
        jdbc.sql("select id, display_name from player where id in (:ids)")
                .param("ids", ids)
                .query(rs -> {
                    names.put(rs.getObject("id", UUID.class), rs.getString("display_name"));
                });
        return names;
    }

    @Override
    public void rename(UUID id, String displayName) {
        jdbc.sql("update player set display_name = :name where id = :id")
                .param("id", id)
                .param("name", displayName)
                .update();
    }
}
