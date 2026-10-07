package com.draughts.adapter.in.web;

import com.draughts.adapter.in.web.identity.PlayerHandshakeHandler;
import com.draughts.adapter.in.web.identity.PlayerToken;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket. Clients subscribe to {@code /topic/games/{id}} for updates and may send moves
 * to {@code /app/games/{id}/move}; errors for a sender arrive on {@code /user/queue/errors}.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final PlayerToken tokens;
    private final WebProperties properties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new PlayerHandshakeHandler(tokens))
                .setAllowedOriginPatterns(properties.allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
