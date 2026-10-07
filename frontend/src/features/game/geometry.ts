// Board geometry shared with the backend engine: square 1-32, row 0 is Black's back row.
// On even rows the playable squares are in columns 1,3,5,7; on odd rows in columns 0,2,4,6.

import type { Color } from '../../api/types'

export interface Coords {
  row: number
  col: number
}

export function squareAt(row: number, col: number): number | null {
  if (row < 0 || row > 7 || col < 0 || col > 7 || (row + col) % 2 === 0) {
    return null
  }
  return row * 4 + Math.floor(col / 2) + 1
}

export function coordsOf(square: number): Coords {
  const index = square - 1
  const row = Math.floor(index / 4)
  const i = index % 4
  return { row, col: row % 2 === 0 ? 2 * i + 1 : 2 * i }
}

/** True when a man of `color` landing on `square` is crowned. */
export function isPromotionSquare(square: number, color: Color): boolean {
  return color === 'BLACK' ? square >= 29 : square <= 4
}
