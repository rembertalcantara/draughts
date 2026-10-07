package com.draughts.adapter.in.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
class SchedulingConfig {

    /** @param threads size of the pool that runs computer searches */
    @ConfigurationProperties("draughts.ai")
    record ComputerProperties(@DefaultValue("2") int threads) {
    }

    @Bean(destroyMethod = "shutdownNow")
    ExecutorService computerMoveExecutor(ComputerProperties properties) {
        return Executors.newFixedThreadPool(properties.threads(), Thread.ofPlatform().name("computer-", 0).factory());
    }
}
