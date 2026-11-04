package com.draughts.adapter.in.web;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import java.util.regex.Pattern;

/**
 * Lets clients subscribe only to the topic of one specific game and to their own error queue. Without this,
 * a wildcard such as {@code /topic/**} would stream every game's updates and reveal their ids.
 */
final class SubscriptionGuard implements ChannelInterceptor {

    private static final Pattern GAME_TOPIC = Pattern.compile("/topic/games/[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");
    private static final String ERRORS_QUEUE = "/user/queue/errors";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) && !allowed(accessor.getDestination())) {
            throw new MessageDeliveryException(message, "Subscription not allowed: " + accessor.getDestination());
        }
        return message;
    }

    static boolean allowed(String destination) {
        return destination != null && (ERRORS_QUEUE.equals(destination) || GAME_TOPIC.matcher(destination).matches());
    }
}
