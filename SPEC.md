# Draughts (Checkers) Specification

## 1. Overview
A two-player draughts game. This spec defines the rules, data model, and
behavior of the game engine and its user interface. Rule variant: **English
draughts (American checkers)** on an 8×8 board, with the variant isolated
behind a rules module so others (e.g. International 10×10) can be added later.

## 2. Goals and Non-Goals
**Goals**
- Correct, fully-enforced rules (mandatory capture, multi-jumps, promotion).
- Human vs. human (local) and human vs. computer.
- Deterministic, testable engine independent of any UI.

**Non-Goals (initial version)**
- Online multiplayer, accounts, ranking.
- Other variants (Russian, International, Brazilian).

## 3. Rules (English Draughts)
### 3.1 Board
- 8×8 board, only the 32 dark squares are used.
- Squares are numbered 1–32 starting from Black's side, left to right, top to bottom.
- Each player starts with 12 pieces on the three rows nearest them.

### 3.2 Pieces
- **Man**: moves and captures one square diagonally *forward* only.
- **King**: a man that reached the far row; moves and captures one square diagonally in *any* direction (non-flying).

### 3.3 Turns
- Black moves first; players alternate.
- A move is either a simple move (to an adjacent empty diagonal square) or a capture.

### 3.4 Captures
- A capture jumps over an adjacent opposing piece to the empty square directly beyond, removing the jumped piece.
- Capturing is **mandatory**. If any capture is available, the player must make one.
- Multi-jumps: if the capturing piece can continue jumping from its landing square, it must continue within the same turn.
- The player may choose among different capture sequences (no majority rule).
- A piece may not jump the same piece twice; captured pieces are removed after the sequence completes.
- A man that reaches the king row during a capture ends its turn there (no continuing as a king in the same turn).

### 3.5 Promotion
- A man reaching the opponent's back row becomes a king.

### 3.6 End of Game
- **Win**: opponent has no pieces or no legal moves on their turn.
- **Draw**: by agreement, or automatically after 40 moves without a capture or man move, or on threefold repetition of position.

## 4. Architecture
```
draughts/
  engine/   # pure game logic, no I/O
  ai/       # computer opponent
  ui/       # rendering and input
  tests/
```
- **Engine** is pure and side-effect free: `state + move -> new state`.
- **UI** only renders state and submits moves; it never decides legality.

## 5. Data Model
```
Color      = BLACK | WHITE
PieceType  = MAN | KING
Piece      = { color: Color, type: PieceType }
Square     = 1..32
Board      = Map<Square, Piece>
Move       = { from: Square, path: Square[], captured: Square[] }
GameState  = {
  board: Board,
  turn: Color,
  halfmoveClock: number,   // moves since last capture/man move
  history: Position[],     // for repetition detection
  status: ONGOING | WIN_BLACK | WIN_WHITE | DRAW
}
```

## 6. Engine API
| Function | Description |
|---|---|
| `newGame()` | Returns the initial `GameState`. |
| `legalMoves(state)` | All legal moves for the side to move; only captures if any exist. |
| `applyMove(state, move)` | Returns new state; throws on illegal move. |
| `status(state)` | Computes ongoing / win / draw. |
| `serialize(state)` / `deserialize(str)` | Persist and restore (FEN-like notation). |

## 7. Computer Opponent
- Minimax with alpha-beta pruning and iterative deepening.
- Evaluation: material (man = 1, king = 1.5–2), advancement, back-row guard, center control, mobility.
- Difficulty levels map to search depth (e.g. Easy 2, Medium 4, Hard 8) and time limit.
- Must always return a legal move within the time budget.

## 8. User Interface
- Rendered board with highlighted selectable pieces and legal destination squares.
- Click/tap piece then destination; multi-jump sequences are guided step by step.
- Mandatory captures are visually indicated.
- Controls: new game, undo/redo, resign, offer draw, choose color/opponent/difficulty.
- Move list in standard notation; indicator for current turn and game result.
- Accessible: keyboard operable, sufficient color contrast, screen-reader labels for squares and pieces.

## 9. Testing
- Unit tests for every rule: simple moves, forced capture, multi-jump, promotion mid-capture, no-backward-move for men, king movement.
- Terminal-state tests: no pieces, blocked, draw by clock, repetition.
- Property tests: random playouts never produce illegal states; piece count never increases.
- Known-position (perft-style) tests comparing move counts to published values.
- AI tests: always legal, finds forced wins in simple positions.

## 10. Milestones
1. Engine with full rules and tests.
2. Local two-player UI.
3. Computer opponent with difficulty levels.
4. Undo/redo, save/load, polish and accessibility.
5. (Future) Additional variants and online play.

## 11. Open Questions
- Target platform/tech stack (web, terminal, desktop)?
- Which variants beyond English draughts, and in what order?
- Time controls / clocks needed?
