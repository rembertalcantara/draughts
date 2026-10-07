package com.draughts.adapter.in.web;

import com.draughts.adapter.in.web.dto.ApiDtos.CreateGameRequest;
import com.draughts.adapter.in.web.dto.ApiDtos.GameView;
import com.draughts.adapter.in.web.dto.ApiDtos.MoveRequest;
import com.draughts.adapter.in.web.identity.CurrentPlayer;
import com.draughts.application.port.in.CreateGameCommand;
import com.draughts.application.port.in.CreateGameUseCase;
import com.draughts.application.port.in.EndGameUseCase;
import com.draughts.application.port.in.JoinGameUseCase;
import com.draughts.application.port.in.PlayMoveCommand;
import com.draughts.application.port.in.PlayMoveUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Endpoints that change a game. */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
class GameCommandController {

    private final CreateGameUseCase createGame;
    private final JoinGameUseCase joinGame;
    private final PlayMoveUseCase playMove;
    private final EndGameUseCase endGame;
    private final GameViewMapper views;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    GameView create(@CurrentPlayer UUID player, @Valid @RequestBody CreateGameRequest request) {
        var command = CreateGameCommand.builder()
                .playerId(player)
                .opponent(request.opponent())
                .color(request.color())
                .difficulty(request.difficulty())
                .variant(request.variant())
                .build();
        return views.toView(createGame.create(command), player);
    }

    @PostMapping("/{id}/join")
    GameView join(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(joinGame.join(id, player), player);
    }

    @PostMapping("/{id}/moves")
    GameView move(@CurrentPlayer UUID player, @PathVariable UUID id, @Valid @RequestBody MoveRequest request) {
        var command = new PlayMoveCommand(id, player, request.from(), request.path(), request.expectedVersion());
        return views.toView(playMove.play(command), player);
    }

    @PostMapping("/{id}/resign")
    GameView resign(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(endGame.resign(id, player), player);
    }

    /** Offers a draw, or accepts the opponent's pending offer. */
    @PostMapping("/{id}/draw-offer")
    GameView offerDraw(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(endGame.offerDraw(id, player), player);
    }

    @PostMapping("/{id}/draw-decline")
    GameView declineDraw(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(endGame.declineDraw(id, player), player);
    }
}
