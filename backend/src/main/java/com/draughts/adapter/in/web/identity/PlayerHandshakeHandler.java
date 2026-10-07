package com.draughts.adapter.in.web.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/** Resolves the WebSocket principal from the identity cookie; anonymous connections may only listen. */
@RequiredArgsConstructor
public class PlayerHandshakeHandler extends DefaultHandshakeHandler {

    private final PlayerToken tokens;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servlet)) {
            return null;
        }
        return tokens.playerId(servlet.getServletRequest())
                .map(id -> (Principal) id::toString)
                .orElse(null);
    }
}
