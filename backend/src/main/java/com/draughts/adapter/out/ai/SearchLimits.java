package com.draughts.adapter.out.ai;

import com.draughts.domain.game.Difficulty;

import java.time.Duration;

/**
 * Search limits for one move.
 *
 * @param maxDepth   maximum iterative-deepening depth in plies
 * @param timeBudget wall-clock budget
 * @param margin     root moves scoring within this many points of the best are picked at random
 */
public record SearchLimits(int maxDepth, Duration timeBudget, int margin) {

    public static SearchLimits of(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new SearchLimits(2, Duration.ofMillis(300), 60);
            case MEDIUM -> new SearchLimits(5, Duration.ofMillis(1000), 10);
            case HARD -> new SearchLimits(9, Duration.ofMillis(2000), 0);
        };
    }
}
