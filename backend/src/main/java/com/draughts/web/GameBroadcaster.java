package com.draughts.web;

import com.draughts.game.GameChanged;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Pushes every committed game change to {@code /topic/games/{id}}. */
@Component
class GameBroadcaster {

    private final SimpMessagingTemplate messaging;
    private final GameViewAssembler views;

    GameBroadcaster(SimpMessagingTemplate messaging, GameViewAssembler views) {
        this.messaging = messaging;
        this.views = views;
    }

    @TransactionalEventListener(fallbackExecution = true)
    void onGameChanged(GameChanged event) {
        messaging.convertAndSend("/topic/games/" + event.game().id(), views.toView(event.game(), null));
    }
}
