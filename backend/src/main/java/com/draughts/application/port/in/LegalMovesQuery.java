package com.draughts.application.port.in;

import com.draughts.domain.engine.Move;
import com.draughts.domain.game.Game;

import java.util.List;

/** The moves the side to move may play. */
public interface LegalMovesQuery {

    /** Legal moves for the side to move; empty unless the game is in progress. */
    List<Move> legalMoves(Game game);
}
