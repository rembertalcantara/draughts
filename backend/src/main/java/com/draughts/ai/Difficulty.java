package com.draughts.ai;

import java.time.Duration;

/**
 * Search limits for the computer opponent.
 *
 * @param maxDepth   maximum iterative-deepening depth in plies
 * @param timeBudget wall-clock budget for one move
 * @param margin     root moves scoring within this many points of the best are picked at random
 */
public enum Difficulty {
    EASY(2, Duration.ofMillis(300), 60),
    MEDIUM(5, Duration.ofMillis(1000), 10),
    HARD(9, Duration.ofMillis(2000), 0);

    private final int maxDepth;
    private final Duration timeBudget;
    private final int margin;

    Difficulty(int maxDepth, Duration timeBudget, int margin) {
        this.maxDepth = maxDepth;
        this.timeBudget = timeBudget;
        this.margin = margin;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public Duration timeBudget() {
        return timeBudget;
    }

    public int margin() {
        return margin;
    }
}
