package com.draughts.domain.engine;

public class IllegalMoveException extends RuntimeException {

    public IllegalMoveException(String message) {
        super(message);
    }
}
