package com.draughts.application.port.in;

import java.util.List;
import java.util.UUID;

/** Finds games that are waiting for the computer to move. */
public interface ComputerTurnQuery {

    /** In-progress games where it is the computer's turn, e.g. after a restart. */
    List<UUID> awaitingComputer(int limit);
}
