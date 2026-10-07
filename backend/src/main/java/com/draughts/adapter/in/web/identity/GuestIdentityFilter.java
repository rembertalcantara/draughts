package com.draughts.adapter.in.web.identity;

import com.draughts.application.port.in.PlayerUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** Gives every API caller a guest identity; requests without a valid cookie get a freshly registered guest. */
@Component
@RequiredArgsConstructor
public class GuestIdentityFilter extends OncePerRequestFilter {

    static final String PLAYER_ATTRIBUTE = GuestIdentityFilter.class.getName() + ".player";

    private final PlayerToken tokens;
    private final PlayerUseCase players;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var uri = request.getRequestURI();
        return !uri.startsWith("/api/") || uri.startsWith("/api/docs") || uri.startsWith("/api/openapi");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var playerId = tokens.playerId(request)
                .filter(id -> players.find(id).isPresent())
                .orElseGet(() -> registerGuest(response));
        request.setAttribute(PLAYER_ATTRIBUTE, playerId);
        chain.doFilter(request, response);
    }

    private UUID registerGuest(HttpServletResponse response) {
        var player = players.registerGuest();
        response.addHeader(HttpHeaders.SET_COOKIE, tokens.cookieFor(player.id()).toString());
        return player.id();
    }
}
