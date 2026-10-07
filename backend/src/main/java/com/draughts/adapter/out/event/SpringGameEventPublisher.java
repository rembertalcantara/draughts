package com.draughts.adapter.out.event;

import com.draughts.application.port.out.GameEventPublisher;
import com.draughts.domain.game.GameChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes game events as Spring application events. Listeners use {@code @TransactionalEventListener},
 * so they only see changes that were committed.
 */
@Component
@RequiredArgsConstructor
class SpringGameEventPublisher implements GameEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(GameChanged event) {
        publisher.publishEvent(event);
    }
}
