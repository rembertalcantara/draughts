package com.draughts.adapter.in.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** @param allowedOrigins origins allowed to open WebSocket connections (the SPA's origin) */
@ConfigurationProperties("draughts.web")
public record WebProperties(List<String> allowedOrigins) {

    public WebProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
