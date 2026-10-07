import type { GameView, LegalMove, PieceView } from '../../api/types'
import { isPromotionSquare } from './geometry'

/** Predicts the server's response to a move so the board updates instantly. */
export function applyMoveLocally(game: GameView, move: LegalMove): GameView {
  const moving = game.board.find((p) => p.square === move.from)
  if (!moving) {
    return game
  }
  const to = move.path[move.path.length - 1] ?? move.from
  const captured = new Set(move.captured)
  const board: PieceView[] = game.board
    .filter((p) => p.square !== move.from && !captured.has(p.square))
    .concat({ square: to, color: moving.color, king: moving.king || isPromotionSquare(to, moving.color) })
    .sort((a, b) => a.square - b.square)
  return {
    ...game,
    board,
    turn: game.turn === 'BLACK' ? 'WHITE' : 'BLACK',
    version: game.version + 1,
    legalMoves: [],
    drawOfferedBy: null,
    moves: [
      ...game.moves,
      { ply: game.moves.length + 1, color: game.turn, from: move.from, path: move.path, captured: move.captured, notation: move.notation },
    ],
  }
}
