package com.draughts.adapter.in.web;

import com.draughts.domain.game.GameChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Pushes every committed game change to {@code /topic/games/{id}}. */
@Component
@RequiredArgsConstructor
class GameBroadcaster {

    private final SimpMessagingTemplate messaging;
    private final GameViewMapper views;

    @TransactionalEventListener(fallbackExecution = true)
    void onGameChanged(GameChanged event) {
        messaging.convertAndSend("/topic/games/" + event.game().id(), views.toView(event.game(), null));
    }
}
