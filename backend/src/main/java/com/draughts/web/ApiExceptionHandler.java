package com.draughts.web;

import com.draughts.game.GameConflictException;
import com.draughts.game.GameException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/** Maps domain errors to RFC 9457 problem details with a stable {@code code} property. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(GameException.class)
    ProblemDetail handle(GameException e) {
        var status = switch (e) {
            case GameException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case GameException.IllegalMove ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case GameException.Forbidden ignored -> HttpStatus.FORBIDDEN;
            case GameException.InvalidState ignored -> HttpStatus.CONFLICT;
            case GameConflictException ignored -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        var code = codeOf(e);
        var problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setType(URI.create("urn:draughts:problem:" + code));
        problem.setProperty("code", code);
        return problem;
    }

    private static String codeOf(GameException e) {
        return switch (e) {
            case GameException.NotFound ignored -> "game-not-found";
            case GameException.IllegalMove ignored -> "illegal-move";
            case GameException.Forbidden ignored -> "forbidden";
            case GameException.InvalidState ignored -> "invalid-state";
            case GameConflictException ignored -> "version-conflict";
            default -> "error";
        };
    }
}
