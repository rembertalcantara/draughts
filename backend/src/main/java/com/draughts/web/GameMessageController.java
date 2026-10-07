package com.draughts.web;

import com.draughts.game.GameException;
import com.draughts.game.GameService;
import com.draughts.web.dto.ApiDtos.MoveRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/** Moves sent over STOMP. The resulting state reaches every subscriber through {@link GameBroadcaster}. */
@Controller
class GameMessageController {

    private final GameService games;

    GameMessageController(GameService games) {
        this.games = games;
    }

    @MessageMapping("/games/{id}/move")
    void move(@DestinationVariable UUID id, @Valid @Payload MoveRequest request, Principal principal) {
        if (principal == null) {
            throw new GameException.Forbidden("Anonymous connections cannot move");
        }
        games.move(id, UUID.fromString(principal.getName()), request.from(), request.path(), request.expectedVersion());
    }

    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    Map<String, String> handle(RuntimeException e) {
        var code = e instanceof GameException ? e.getClass().getSimpleName() : "Error";
        return Map.of("code", code, "detail", String.valueOf(e.getMessage()));
    }
}
