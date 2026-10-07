package com.draughts.adapter.out.ai;

import com.draughts.application.port.out.ComputerPlayer;
import com.draughts.domain.engine.Color;
import com.draughts.domain.engine.GameState;
import com.draughts.domain.engine.Move;
import com.draughts.domain.game.Difficulty;
import org.springframework.stereotype.Component;

/** {@link ComputerPlayer} backed by the alpha-beta {@link SearchAi}. */
@Component
class SearchComputerPlayer implements ComputerPlayer {

    /** The computer accepts a draw when its evaluation is at least this far behind. */
    private static final int ACCEPTS_DRAW_BELOW = -100;

    private final SearchAi search = new SearchAi();

    @Override
    public Move chooseMove(GameState state, Difficulty difficulty) {
        return search.chooseMove(state.board(), state.turn(), SearchLimits.of(difficulty));
    }

    @Override
    public boolean acceptsDraw(GameState state, Color color) {
        return Evaluator.evaluate(state.board(), color) <= ACCEPTS_DRAW_BELOW;
    }
}
