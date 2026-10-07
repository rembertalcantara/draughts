package com.draughts.config;

import com.draughts.domain.engine.EnglishDraughts;
import com.draughts.domain.engine.RuleSet;
import com.draughts.domain.game.Variants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;
import java.util.random.RandomGenerator;

/** Exposes the framework-free domain objects (and the JDK services they need) as beans. */
@Configuration
class DomainConfig {

    /** Supporting another variant means adding another {@link RuleSet} bean. */
    @Bean
    RuleSet englishDraughts() {
        return new EnglishDraughts();
    }

    @Bean
    Variants variants(List<RuleSet> ruleSets) {
        return new Variants(ruleSets);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RandomGenerator randomGenerator() {
        return RandomGenerator.getDefault();
    }
}
