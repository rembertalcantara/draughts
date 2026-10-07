package com.draughts.web.identity;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/** Signs and verifies the guest identity token {@code <player-id>.<hmac>} stored in a cookie. */
@Component
public class PlayerToken {

    public static final String COOKIE = "DRAUGHTS_PLAYER";

    private final SecretKeySpec key;

    public PlayerToken(IdentityProperties properties) {
        this.key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String sign(UUID playerId) {
        return playerId + "." + mac(playerId.toString());
    }

    public Optional<UUID> verify(String token) {
        if (token == null) {
            return Optional.empty();
        }
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
