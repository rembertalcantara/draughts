package com.draughts.game;

import com.draughts.engine.EnglishDraughts;
import com.draughts.engine.RuleSet;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Registry of supported rule variants. */
@Component
public class Variants {

    public static final String DEFAULT = EnglishDraughts.VARIANT;

    private final Map<String, RuleSet> rules = Map.of(EnglishDraughts.VARIANT, new EnglishDraughts());

    public RuleSet forVariant(String variant) {
        var ruleSet = rules.get(variant);
        if (ruleSet == null) {
            throw new IllegalArgumentException("Unsupported variant: " + variant);
        }
        return ruleSet;
    }

    public boolean supports(String variant) {
        return rules.containsKey(variant);
    }
}
