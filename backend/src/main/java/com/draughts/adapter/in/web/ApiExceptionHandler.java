package com.draughts.adapter.in.web;

import com.draughts.domain.game.GameException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Locale;

/** Maps domain errors to RFC 9457 problem details with a stable {@code code} property. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(GameException.class)
    ProblemDetail handle(GameException e) {
        return problem(statusOf(e.getCode()), slug(e.getCode()), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handle(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-request", e.getMessage());
    }

    /** {@code VERSION_CONFLICT} becomes {@code version-conflict}. */
    static String slug(GameException.Code code) {
        return code.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static HttpStatus statusOf(GameException.Code code) {
        return switch (code) {
            case GAME_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ILLEGAL_MOVE -> HttpStatus.UNPROCESSABLE_CONTENT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case INVALID_STATE, VERSION_CONFLICT -> HttpStatus.CONFLICT;
        };
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:draughts:problem:" + code));
        problem.setProperty("code", code);
        return problem;
    }
}
