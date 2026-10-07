package com.draughts.ai;

import com.draughts.engine.Board;
import com.draughts.engine.Color;
import com.draughts.engine.Move;
import com.draughts.engine.MoveGenerator;
import com.draughts.engine.Zobrist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Negamax search with alpha-beta pruning, iterative deepening, a transposition table and capture
 * extension at the horizon. Each call uses its own search state, so one instance is thread-safe.
 */
public final class SearchAi {

    static final int WIN = 1_000_000;
    private static final int ROOT_WINDOW = 200;

    private final RandomGenerator random;

    public SearchAi(RandomGenerator random) {
        this.random = random;
    }

    public SearchAi() {
        this(RandomGenerator.getDefault());
    }

    /** Chooses a move for {@code toMove}. Always returns a legal move when one exists. */
    public Move chooseMove(Board board, Color toMove, Difficulty difficulty) {
        var moves = MoveGenerator.legalMoves(board, toMove);
        if (moves.isEmpty()) {
            throw new IllegalStateException("No legal moves for " + toMove);
        }
        if (moves.size() == 1) {
            return moves.getFirst();
        }
        var search = new Search(System.nanoTime() + difficulty.timeBudget().toNanos());
        var scored = moves.stream().map(m -> new Scored(m, 0)).toList();
        for (int depth = 1; depth <= difficulty.maxDepth(); depth++) {
            try {
                scored = search.root(board, toMove, scored, depth);
            } catch (Timeout e) {
                break;
            }
            if (Math.abs(scored.getFirst().score()) >= WIN - 1000) {
                break; // forced result found
            }
        }
        int best = scored.getFirst().score();
        var candidates = scored.stream().filter(s -> s.score() >= best - difficulty.margin()).toList();
        return candidates.get(random.nextInt(candidates.size())).move();
    }

    record Scored(Move move, int score) {
    }

    private static final class Timeout extends RuntimeException {
        Timeout() {
            super(null, null, false, false);
        }
    }

    private static final class Search {

        private static final int TT_SIZE = 1 << 18;
        private static final byte EXACT = 0;
        private static final byte LOWER = 1;
        private static final byte UPPER = 2;

        private final long deadline;
        private final long[] keys = new long[TT_SIZE];
        private final int[] scores = new int[TT_SIZE];
        private final byte[] depths = new byte[TT_SIZE];
        private final byte[] flags = new byte[TT_SIZE];
        private final byte[] bestIndex = new byte[TT_SIZE];
        private long nodes;

        Search(long deadline) {
            this.deadline = deadline;
        }

        List<Scored> root(Board board, Color toMove, List<Scored> ordered, int depth) {
            var results = new ArrayList<Scored>(ordered.size());
            int alpha = -WIN - 1;
            for (var entry : ordered) {
                var child = board.apply(entry.move(), toMove);
                int score = -negamax(child, toMove.opposite(), depth - 1, -WIN - 1, -alpha, 1);
                results.add(new Scored(entry.move(), score));
                // Narrow the window only so far that every move within the random margin is scored exactly.
                alpha = Math.max(alpha, score - ROOT_WINDOW);
            }
            results.sort(Comparator.comparingInt(Scored::score).reversed());
            return results;
        }

        private int negamax(Board board, Color toMove, int depth, int alpha, int beta, int ply) {
            if ((++nodes & 1023) == 0 && System.nanoTime() > deadline) {
                throw new Timeout();
            }
            var moves = MoveGenerator.legalMoves(board, toMove);
            if (moves.isEmpty()) {
                return -WIN + ply;
            }
            boolean forcing = moves.getFirst().isCapture();
            if (depth <= 0 && !forcing) {
                return Evaluator.evaluate(board, toMove);
            }
            if (ply > 60) {
                return Evaluator.evaluate(board, toMove);
            }

            long key = Zobrist.hash(board, toMove);
            int slot = (int) (key & (TT_SIZE - 1));
            int ttBest = -1;
            if (keys[slot] == key) {
                ttBest = bestIndex[slot];
                if (depths[slot] >= depth) {
                    int s = scores[slot];
                    if (flags[slot] == EXACT
                            || (flags[slot] == LOWER && s >= beta)
                            || (flags[slot] == UPPER && s <= alpha)) {
                        return s;
                    }
                }
            }

            int originalAlpha = alpha;
            int best = -WIN - 1;
            int bestIdx = 0;
            int[] order = order(moves, ttBest);
            for (int idx : order) {
                var child = board.apply(moves.get(idx), toMove);
                int score = -negamax(child, toMove.opposite(), depth - 1, -beta, -alpha, ply + 1);
                if (score > best) {
                    best = score;
                    bestIdx = idx;
                }
                alpha = Math.max(alpha, score);
                if (alpha >= beta) {
                    break;
                }
            }

            keys[slot] = key;
            scores[slot] = best;
            depths[slot] = (byte) Math.max(0, Math.min(depth, 127));
            bestIndex[slot] = (byte) Math.min(bestIdx, 127);
            flags[slot] = best <= originalAlpha ? UPPER : best >= beta ? LOWER : EXACT;
            return best;
        }

        /** TT move first, then longer captures first. */
        private static int[] order(List<Move> moves, int ttBest) {
            var indices = new ArrayList<Integer>(moves.size());
            for (int i = 0; i < moves.size(); i++) {
                indices.add(i);
            }
            indices.sort(Comparator.<Integer>comparingInt(i -> i == ttBest ? 0 : 1)
                    .thenComparingInt(i -> -moves.get(i).captured().size()));
            return indices.stream().mapToInt(Integer::intValue).toArray();
        }
    }
}
