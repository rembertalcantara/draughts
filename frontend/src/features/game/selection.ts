import type { LegalMove } from '../../api/types'

/** A move being built click by click: the piece picked and the landing squares chosen so far. */
export interface Selection {
  from: number | null
  path: number[]
}

export const NO_SELECTION: Selection = { from: null, path: [] }

export function movablePieces(moves: LegalMove[]): Set<number> {
  return new Set(moves.map((m) => m.from))
}

function startsWith(path: number[], prefix: number[]): boolean {
  return prefix.every((square, i) => path[i] === square)
}

/** Legal moves still compatible with the selection. */
export function candidates(moves: LegalMove[], selection: Selection): LegalMove[] {
  if (selection.from === null) {
    return []
  }
  return moves.filter((m) => m.from === selection.from && startsWith(m.path, selection.path))
}

/** Squares the selected piece can go to next (one jump at a time during a multi-jump). */
export function nextTargets(moves: LegalMove[], selection: Selection): Set<number> {
  const targets = new Set<number>()
  for (const move of candidates(moves, selection)) {
    const next = move.path[selection.path.length]
    if (next !== undefined) {
      targets.add(next)
    }
  }
  return targets
}

export interface ClickResult {
  selection: Selection
  /** Set when the click completes exactly one legal move, which should be submitted. */
  move?: LegalMove
}

export function clickSquare(moves: LegalMove[], selection: Selection, square: number): ClickResult {
  const movable = movablePieces(moves)
  if (selection.from === null) {
    return { selection: movable.has(square) ? { from: square, path: [] } : NO_SELECTION }
  }
  if (nextTargets(moves, selection).has(square)) {
    const path = [...selection.path, square]
    const remaining = candidates(moves, { from: selection.from, path })
    const complete = remaining.filter((m) => m.path.length === path.length)
    if (complete.length === 1 && remaining.length === 1) {
      return { selection: NO_SELECTION, move: complete[0] }
    }
    return { selection: { from: selection.from, path } }
  }
  if (selection.path.length > 0) {
    // In the middle of a multi-jump only the next landing square is accepted.
    return { selection }
  }
  if (square === selection.from) {
    return { selection: NO_SELECTION }
  }
  return { selection: movable.has(square) ? { from: square, path: [] } : NO_SELECTION }
}
