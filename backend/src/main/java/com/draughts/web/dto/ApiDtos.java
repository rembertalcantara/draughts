package com.draughts.web.dto;

import com.draughts.ai.Difficulty;
import com.draughts.engine.Color;
import com.draughts.game.ColorChoice;
import com.draughts.game.GameResult;
import com.draughts.game.Lifecycle;
import com.draughts.game.Opponent;
import com.draughts.game.ResultReason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request and response bodies of the HTTP and WebSocket API. */
public final class ApiDtos {

    private ApiDtos() {
    }

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
