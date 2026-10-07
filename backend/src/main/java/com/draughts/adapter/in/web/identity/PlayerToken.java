package com.draughts.adapter.in.web.identity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * The guest identity cookie: {@code <player-id>.<hmac>}, HttpOnly and SameSite=Lax. Signing makes the
 * player id unforgeable without a server-side session.
 */
@Component
public class PlayerToken {

    static final String COOKIE = "DRAUGHTS_PLAYER";
    private static final Duration LIFETIME = Duration.ofDays(365);

    private final SecretKeySpec key;
    private final boolean secureCookie;

    public PlayerToken(IdentityProperties properties) {
        this.key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.secureCookie = properties.secureCookie();
    }

    /** The verified player id carried by the request's cookie, if any. */
    public Optional<UUID> playerId(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies()).stream()
                .flatMap(Arrays::stream)
                .filter(c -> COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .flatMap(this::verify);
    }

    public ResponseCookie cookieFor(UUID playerId) {
        return ResponseCookie.from(COOKIE, sign(playerId))
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(LIFETIME)
                .build();
    }

    String sign(UUID playerId) {
        return playerId + "." + mac(playerId.toString());
    }

    Optional<UUID> verify(String token) {
        int dot = token.indexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        var id = token.substring(0, dot);
        var expected = mac(id).getBytes(StandardCharsets.US_ASCII);
        var actual = token.substring(dot + 1).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String mac(String value) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
