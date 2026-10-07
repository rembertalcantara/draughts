package com.draughts.adapter.in.web;

import com.draughts.adapter.in.web.dto.ApiDtos.GameSummaryView;
import com.draughts.adapter.in.web.dto.ApiDtos.GameView;
import com.draughts.adapter.in.web.dto.ApiDtos.PlayedMoveView;
import com.draughts.adapter.in.web.identity.CurrentPlayer;
import com.draughts.application.port.in.GameQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Read-only endpoints. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class GameQueryController {

    private static final int LIST_LIMIT = 50;

    private final GameQuery games;
    private final GameViewMapper views;

    @GetMapping("/games/{id}")
    GameView get(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return views.toView(games.get(id), player);
    }

    @GetMapping("/games/{id}/moves")
    List<PlayedMoveView> moves(@CurrentPlayer UUID player, @PathVariable UUID id) {
        return get(player, id).moves();
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
}
