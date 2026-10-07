package com.draughts.adapter.in.web;

import com.draughts.adapter.in.web.dto.ApiDtos.MoveRequest;
import com.draughts.application.port.in.PlayMoveCommand;
import com.draughts.application.port.in.PlayMoveUseCase;
import com.draughts.domain.game.GameException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
class GameMessageController {

    private final PlayMoveUseCase playMove;

    @MessageMapping("/games/{id}/move")
    void move(@DestinationVariable UUID id, @Valid @Payload MoveRequest request, Principal principal) {
        if (principal == null) {
            throw GameException.forbidden("Anonymous connections cannot move");
        }
        playMove.play(new PlayMoveCommand(id, UUID.fromString(principal.getName()), request.from(), request.path(),
                request.expectedVersion()));
    }

    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    Map<String, String> handle(RuntimeException e) {
        var code = e instanceof GameException ge ? ApiExceptionHandler.slug(ge.getCode()) : "error";
        return Map.of("code", code, "detail", String.valueOf(e.getMessage()));
    }
}
