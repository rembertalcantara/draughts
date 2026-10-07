package com.draughts.web.identity;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/** Resolves the WebSocket principal from the signed identity cookie; anonymous connections may only listen. */
public class PlayerHandshakeHandler extends DefaultHandshakeHandler {

    private final PlayerToken tokens;

    public PlayerHandshakeHandler(PlayerToken tokens) {
        this.tokens = tokens;
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servlet) {
            var playerId = GuestIdentityFilter.cookie(servlet.getServletRequest()).flatMap(tokens::verify);
            if (playerId.isPresent()) {
                var name = playerId.get().toString();
                return () -> name;
            }
        }
        return null;
    }
}
