import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Board } from './Board'
import type { LegalMove, PieceView } from '../../api/types'

const pieces: PieceView[] = [
  { square: 11, color: 'BLACK', king: false },
  { square: 22, color: 'WHITE', king: true },
]
const moves: LegalMove[] = [{ from: 11, path: [15], captured: [], notation: '11-15' }]

describe('Board', () => {
  it('labels squares for assistive technology', () => {
    render(<Board pieces={pieces} legalMoves={moves} flipped={false} onMove={() => {}} />)
    expect(screen.getByRole('grid', { name: 'Draughts board' })).toBeInTheDocument()
    expect(screen.getByLabelText(/Square 11, Black man, can move/)).toBeInTheDocument()
    expect(screen.getByLabelText(/Square 22, White king/)).toBeInTheDocument()
  })

  it('plays a move with two clicks', async () => {
    const onMove = vi.fn()
    render(<Board pieces={pieces} legalMoves={moves} flipped={false} onMove={onMove} />)
    await userEvent.click(screen.getByLabelText(/^Square 11,/))
    expect(screen.getByLabelText(/^Square 15, empty, possible destination/)).toBeInTheDocument()
    await userEvent.click(screen.getByLabelText(/^Square 15,/))
    expect(onMove).toHaveBeenCalledWith(moves[0])
  })

  it('plays a move with the keyboard', async () => {
    const onMove = vi.fn()
    render(<Board pieces={pieces} legalMoves={moves} flipped={false} onMove={onMove} />)
    const square11 = screen.getByLabelText(/^Square 11,/)
    square11.focus()
    await userEvent.keyboard('{Enter}')
    screen.getByLabelText(/^Square 15,/).focus()
    await userEvent.keyboard('{Enter}')
    expect(onMove).toHaveBeenCalledWith(moves[0])
  })
})
