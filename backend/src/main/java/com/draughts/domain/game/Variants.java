package com.draughts.domain.game;

import com.draughts.domain.engine.EnglishDraughts;
import com.draughts.domain.engine.RuleSet;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Registry of the supported rule variants. New variants are added by registering another {@link RuleSet}. */
public final class Variants {

    public static final String DEFAULT = EnglishDraughts.VARIANT;

    private final Map<String, RuleSet> rules;

    public Variants(Collection<RuleSet> ruleSets) {
        this.rules = ruleSets.stream().collect(Collectors.toUnmodifiableMap(RuleSet::variant, Function.identity()));
    }

    /** @param variant a variant id, or {@code null} for the default */
    public RuleSet forVariant(String variant) {
        var ruleSet = rules.get(variant == null ? DEFAULT : variant);
        if (ruleSet == null) {
            throw GameException.invalidState("Unsupported variant: " + variant);
        }
        return ruleSet;
    }
}
