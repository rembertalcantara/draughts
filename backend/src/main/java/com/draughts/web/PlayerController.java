package com.draughts.web;

import com.draughts.player.PlayerService;
import com.draughts.web.dto.ApiDtos.MeView;
import com.draughts.web.dto.ApiDtos.RenameRequest;
import com.draughts.web.identity.CurrentPlayer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
class PlayerController {

    private final PlayerService players;

    PlayerController(PlayerService players) {
        this.players = players;
    }

    @GetMapping
    MeView me(@CurrentPlayer UUID player) {
        var p = players.find(player).orElseThrow();
        return new MeView(p.id(), p.displayName());
    }

    @PatchMapping
    MeView rename(@CurrentPlayer UUID player, @Valid @RequestBody RenameRequest request) {
        var p = players.rename(player, request.displayName());
        return new MeView(p.id(), p.displayName());
    }
}
