package com.draughts.adapter.in.web;

import com.draughts.adapter.in.web.dto.ApiDtos.MeView;
import com.draughts.adapter.in.web.dto.ApiDtos.RenameRequest;
import com.draughts.adapter.in.web.identity.CurrentPlayer;
import com.draughts.application.port.in.PlayerUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
class PlayerController {

    private final PlayerUseCase players;

    @GetMapping
    MeView me(@CurrentPlayer UUID player) {
        return MeView.of(players.find(player).orElseThrow());
    }

    @PatchMapping
    MeView rename(@CurrentPlayer UUID player, @Valid @RequestBody RenameRequest request) {
        return MeView.of(players.rename(player, request.displayName()));
    }
}
