package com.draughts.web;

import com.draughts.game.CreateGameCommand;
import com.draughts.game.GameService;
import com.draughts.web.dto.ApiDtos.CreateGameRequest;
import com.draughts.web.dto.ApiDtos.GameSummaryView;
import com.draughts.web.dto.ApiDtos.GameView;
import com.draughts.web.dto.ApiDtos.MoveRequest;
import com.draughts.web.dto.ApiDtos.PlayedMoveView;
import com.draughts.web.identity.CurrentPlayer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
class GameController {

    private static final int LIST_LIMIT = 50;

    private final GameService games;
    private final GameViewAssembler views;

    GameController(GameService games, GameViewAssembler views) {
        this.games = games;
        this.views = views;
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    GameView create(@CurrentPlayer UUID player, @Valid @RequestBody CreateGameRequest request) {
        var game = games.create(player, new CreateGameCommand(request.opponent(), request.color(),
                request.difficulty(), request.variant()));
        return views.toView(game, player);
    }

    @GetMapping("/games/{id}")
    GameView get(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(games.get(id), player);
    }

    /** The caller's games, most recently active first. */
    @GetMapping("/games")
    List<GameSummaryView> mine(@CurrentPlayer UUID player) {
        return views.toSummaries(games.gamesOf(player, LIST_LIMIT), player);
    }

    @GetMapping("/lobby")
    List<GameSummaryView> lobby(@CurrentPlayer UUID player) {
        return views.toSummaries(games.openGames(LIST_LIMIT), player);
    }

    @PostMapping("/games/{id}/join")
    GameView join(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(games.join(id, player), player);
    }

    @PostMapping("/games/{id}/moves")
    GameView move(@CurrentPlayer UUID player, @PathVariable UUID id, @Valid @RequestBody MoveRequest request) {
        return views.toView(games.move(id, player, request.from(), request.path(), request.expectedVersion()), player);
    }

    @GetMapping("/games/{id}/moves")
    List<PlayedMoveView> moves(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(games.get(id), player).moves();
    }

    @PostMapping("/games/{id}/resign")
    GameView resign(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(games.resign(id, player), player);
    }

    /** Offers a draw, or accepts the opponent's pending offer. */
    @PostMapping("/games/{id}/draw-offer")
    GameView offerDraw(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(games.offerDraw(id, player), player);
    }

    @PostMapping("/games/{id}/draw-decline")
    GameView declineDraw(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(games.declineDraw(id, player), player);
    }
}
