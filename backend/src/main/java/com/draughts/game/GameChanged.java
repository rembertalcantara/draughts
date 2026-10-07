package com.draughts.game;

/** Published after a game changes; listeners run after the transaction commits. */
public record GameChanged(Game game) {
}
