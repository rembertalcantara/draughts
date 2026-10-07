package com.draughts.web.identity;

import com.draughts.player.PlayerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Gives every API caller a guest identity. The player id lives in a signed, HttpOnly, SameSite=Lax
 * cookie; requests without a valid cookie get a freshly created guest.
 */
@Component
public class GuestIdentityFilter extends OncePerRequestFilter {

    public static final String PLAYER_ATTRIBUTE = GuestIdentityFilter.class.getName() + ".player";

    private final PlayerToken tokens;
    private final PlayerService players;
    private final IdentityProperties properties;

    public GuestIdentityFilter(PlayerToken tokens, PlayerService players, IdentityProperties properties) {
        this.tokens = tokens;
        this.players = players;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/api/docs")
                || request.getRequestURI().startsWith("/api/openapi");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var playerId = cookie(request)
                .flatMap(tokens::verify)
                .filter(id -> players.find(id).isPresent())
                .orElseGet(() -> issue(response));
        request.setAttribute(PLAYER_ATTRIBUTE, playerId);
        chain.doFilter(request, response);
    }

    public static Optional<String> cookie(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies()).stream()
                .flatMap(Arrays::stream)
                .filter(c -> PlayerToken.COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private UUID issue(HttpServletResponse response) {
        var player = players.createGuest();
        var cookie = ResponseCookie.from(PlayerToken.COOKIE, tokens.sign(player.id()))
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(365))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return player.id();
    }
}
