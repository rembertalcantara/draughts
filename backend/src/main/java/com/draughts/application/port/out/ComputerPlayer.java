package com.draughts.application.port.out;

import com.draughts.domain.engine.Color;
import com.draughts.domain.engine.GameState;
import com.draughts.domain.engine.Move;
import com.draughts.domain.game.Difficulty;

/** The computer opponent. */
public interface ComputerPlayer {

    /** Chooses a legal move for the side to move. */
    Move chooseMove(GameState state, Difficulty difficulty);

    /** Whether the computer, playing {@code color}, accepts a draw in this position. */
    boolean acceptsDraw(GameState state, Color color);
}
