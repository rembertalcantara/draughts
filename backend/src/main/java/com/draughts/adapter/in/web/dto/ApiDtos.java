package com.draughts.adapter.in.web.dto;

import com.draughts.domain.engine.Color;
import com.draughts.domain.game.ColorChoice;
import com.draughts.domain.game.Difficulty;
import com.draughts.domain.game.GameResult;
import com.draughts.domain.game.Lifecycle;
import com.draughts.domain.game.Opponent;
import com.draughts.domain.game.ResultReason;
import com.draughts.domain.player.Player;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request and response bodies of the HTTP and WebSocket API. */
@UtilityClass
public class ApiDtos {

    public record CreateGameRequest(@NotNull Opponent opponent, ColorChoice color, Difficulty difficulty,
                                    String variant) {
    }

    public record MoveRequest(
            @Min(1) @Max(32) int from,
            @NotEmpty @Size(max = 12) List<@NotNull @Min(1) @Max(32) Integer> path,
            Integer expectedVersion) {
    }

    public record RenameRequest(@NotBlank @Size(max = 40) String displayName) {
    }

    public record PlayerView(String name, boolean computer) {
    }

    public record MeView(UUID id, String displayName) {

        public static MeView of(Player player) {
            return new MeView(player.id(), player.displayName());
        }
    }

    public record PieceView(int square, Color color, boolean king) {
    }

    public record MoveView(int from, List<Integer> path, List<Integer> captured, String notation) {
    }

    public record PlayedMoveView(int ply, Color color, int from, List<Integer> path, List<Integer> captured,
                                 String notation) {
    }

    /**
     * Full game state. {@code yourColor} is filled for the requesting player on HTTP responses and is
     * {@code null} on WebSocket broadcasts, which are shared by all subscribers.
     */
    public record GameView(
            UUID id,
            String variant,
            Lifecycle status,
            Opponent opponent,
            Difficulty aiDifficulty,
            Color turn,
            int version,
            String fen,
            List<PieceView> board,
            List<MoveView> legalMoves,
            List<PlayedMoveView> moves,
            PlayerView black,
            PlayerView white,
            Color yourColor,
            Color drawOfferedBy,
            GameResult result,
            ResultReason resultReason,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record GameSummaryView(
            UUID id,
            String variant,
            Lifecycle status,
            Opponent opponent,
            Difficulty aiDifficulty,
            PlayerView black,
            PlayerView white,
            Color yourColor,
            Color turn,
            GameResult result,
            ResultReason resultReason,
            int moveCount,
            Instant createdAt,
            Instant updatedAt) {
    }
}
