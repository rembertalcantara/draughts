import { describe, expect, it } from 'vitest'
import type { LegalMove } from '../../api/types'
import { clickSquare, movablePieces, nextTargets, NO_SELECTION } from './selection'

const move = (from: number, path: number[], captured: number[] = []): LegalMove => ({
  from,
  path,
  captured,
  notation: [from, ...path].join(captured.length ? 'x' : '-'),
})

describe('selection', () => {
  const opening = [move(11, [15]), move(11, [16]), move(12, [16])]

  it('lists the pieces that can move', () => {
    expect([...movablePieces(opening)].sort()).toEqual([11, 12])
  })

  it('selects a movable piece and ignores others', () => {
    expect(clickSquare(opening, NO_SELECTION, 11).selection).toEqual({ from: 11, path: [] })
    expect(clickSquare(opening, NO_SELECTION, 1).selection).toEqual(NO_SELECTION)
  })

  it('completes a simple move on the destination click', () => {
    const selected = clickSquare(opening, NO_SELECTION, 11).selection
    expect(nextTargets(opening, selected)).toEqual(new Set([15, 16]))
    const result = clickSquare(opening, selected, 16)
    expect(result.move).toEqual(opening[1])
    expect(result.selection).toEqual(NO_SELECTION)
  })

  it('switches to another piece or deselects', () => {
    const selected = clickSquare(opening, NO_SELECTION, 11).selection
    expect(clickSquare(opening, selected, 12).selection).toEqual({ from: 12, path: [] })
    expect(clickSquare(opening, selected, 11).selection).toEqual(NO_SELECTION)
  })

  it('builds a multi-jump one landing square at a time', () => {
    const jumps = [move(1, [10, 19], [6, 15]), move(1, [10, 17], [6, 14])]
    let result = clickSquare(jumps, NO_SELECTION, 1)
    result = clickSquare(jumps, result.selection, 10)
    expect(result.move).toBeUndefined()
    expect(result.selection).toEqual({ from: 1, path: [10] })
    expect(nextTargets(jumps, result.selection)).toEqual(new Set([19, 17]))

    // Clicking elsewhere mid-jump keeps the partial capture.
    expect(clickSquare(jumps, result.selection, 5).selection).toEqual({ from: 1, path: [10] })

    result = clickSquare(jumps, result.selection, 17)
    expect(result.move).toEqual(jumps[1])
  })
})
