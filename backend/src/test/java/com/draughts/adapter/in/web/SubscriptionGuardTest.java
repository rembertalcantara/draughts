package com.draughts.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionGuardTest {

    private final SubscriptionGuard guard = new SubscriptionGuard();

    private static Message<byte[]> stomp(StompCommand command, String destination) {
        var accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/topic/games/3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b",
            "/user/queue/errors"})
    void allowsOneGameTopicAndOwnErrors(String destination) {
        var message = stomp(StompCommand.SUBSCRIBE, destination);
        assertThat(guard.preSend(message, null)).isSameAs(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/topic/**",
            "/topic/games/*",
            "/topic/games/**",
            "/topic/games/not-a-uuid",
            "/topic/games/3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b/extra",
            "/queue/anything",
            "/user/queue/other"})
    void rejectsWildcardsAndOtherDestinations(String destination) {
        assertThatThrownBy(() -> guard.preSend(stomp(StompCommand.SUBSCRIBE, destination), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void ignoresOtherFrames() {
        var message = stomp(StompCommand.SEND, "/app/games/x/move");
        assertThat(guard.preSend(message, null)).isSameAs(message);
    }
}
