import { describe, expect, it } from 'vitest'
import type { GameView } from '../../api/types'
import { applyMoveLocally } from './optimistic'
import { coordsOf, squareAt } from './geometry'

const game = (overrides: Partial<GameView>): GameView => ({
  id: 'g',
  variant: 'ENGLISH',
  status: 'IN_PROGRESS',
  opponent: 'HUMAN',
  aiDifficulty: null,
  turn: 'BLACK',
  version: 3,
  fen: '',
  board: [],
  legalMoves: [],
  moves: [],
  black: null,
  white: null,
  yourColor: 'BLACK',
  drawOfferedBy: null,
  result: null,
  resultReason: null,
  createdAt: '',
  updatedAt: '',
  ...overrides,
})

describe('geometry', () => {
  it('matches the backend numbering', () => {
    expect(squareAt(0, 1)).toBe(1)
    expect(squareAt(0, 7)).toBe(4)
    expect(squareAt(7, 0)).toBe(29)
    expect(squareAt(0, 0)).toBeNull()
    for (let square = 1; square <= 32; square++) {
      const { row, col } = coordsOf(square)
      expect(squareAt(row, col)).toBe(square)
    }
  })
})

describe('applyMoveLocally', () => {
  it('moves the piece, removes captures, crowns and passes the turn', () => {
    const before = game({
      board: [
        { square: 22, color: 'BLACK', king: false },
        { square: 26, color: 'WHITE', king: false },
      ],
    })
    const after = applyMoveLocally(before, { from: 22, path: [31], captured: [26], notation: '22x31' })
    expect(after.board).toEqual([{ square: 31, color: 'BLACK', king: true }])
    expect(after.turn).toBe('WHITE')
    expect(after.version).toBe(4)
    expect(after.moves).toHaveLength(1)
  })
})
