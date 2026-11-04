package com.draughts.adapter.in.web.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param secret       HMAC key that signs the guest identity cookie; at least 32 characters
 * @param secureCookie mark the cookie {@code Secure} (enable behind HTTPS)
 */
@ConfigurationProperties("draughts.identity")
public record IdentityProperties(String secret, @DefaultValue("false") boolean secureCookie) {

    public IdentityProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("draughts.identity.secret must be set to at least 32 characters "
                    + "(environment variable DRAUGHTS_IDENTITY_SECRET; use the \"dev\" profile for local development)");
        }
    }
}
