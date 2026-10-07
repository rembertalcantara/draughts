package com.draughts.application.port.out;

import com.draughts.domain.game.GameChanged;

/** Announces game changes; delivery happens after the surrounding transaction commits. */
public interface GameEventPublisher {

    void publish(GameChanged event);
}
