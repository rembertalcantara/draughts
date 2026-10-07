package com.draughts.game;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param threads size of the pool that runs computer searches */
@ConfigurationProperties("draughts.ai")
public record AiProperties(@DefaultValue("2") int threads) {
}
