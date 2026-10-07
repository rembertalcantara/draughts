package com.draughts.domain.game;

/** Domain event raised whenever a game is created or changes. */
public record GameChanged(Game game) {
}
