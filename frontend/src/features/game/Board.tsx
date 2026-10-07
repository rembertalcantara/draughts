import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import type { Color, LegalMove, PieceView } from '../../api/types'
import { squareAt } from './geometry'
import { clickSquare, movablePieces, nextTargets, NO_SELECTION, type Selection } from './selection'

interface BoardProps {
  pieces: PieceView[]
  /** Moves the viewer may play now; empty when it is not their turn. */
  legalMoves: LegalMove[]
  /** Show the board from Black's side (Black's pieces at the bottom). */
  flipped: boolean
  lastMove?: { from: number; path: number[] }
  onMove: (move: LegalMove) => void
}

export function Board({ pieces, legalMoves, flipped, lastMove, onMove }: BoardProps) {
  const { t } = useTranslation()
  const [selection, setSelection] = useState<Selection>(NO_SELECTION)
  const [focus, setFocus] = useState({ row: 7, col: 0 })
  const gridRef = useRef<HTMLDivElement>(null)

  // Roving focus: when the board already has focus, follow the arrow keys.
  useEffect(() => {
    const grid = gridRef.current
    if (grid?.contains(document.activeElement)) {
      grid.querySelector<HTMLElement>('[tabindex="0"]')?.focus()
    }
  }, [focus])

  // Drop a selection that no longer matches the legal moves (e.g. after an update from the server).
  const [movesSeen, setMovesSeen] = useState(legalMoves)
  if (movesSeen !== legalMoves) {
    setMovesSeen(legalMoves)
    setSelection(NO_SELECTION)
  }

  const bySquare = useMemo(() => new Map(pieces.map((p) => [p.square, p])), [pieces])
  const movable = useMemo(() => movablePieces(legalMoves), [legalMoves])
  const targets = useMemo(() => nextTargets(legalMoves, selection), [legalMoves, selection])
  const mustCapture = legalMoves.length > 0 && (legalMoves[0]?.captured.length ?? 0) > 0
  const lastSquares = useMemo(() => new Set(lastMove ? [lastMove.from, ...lastMove.path] : []), [lastMove])

  const activate = (square: number) => {
    const result = clickSquare(legalMoves, selection, square)
    setSelection(result.selection)
    if (result.move) {
      onMove(result.move)
    }
  }

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    const delta: Record<string, [number, number]> = {
      ArrowUp: [-1, 0],
      ArrowDown: [1, 0],
      ArrowLeft: [0, -1],
      ArrowRight: [0, 1],
    }
    const step = delta[event.key]
    if (step) {
      event.preventDefault()
      setFocus((f) => ({
        row: Math.min(7, Math.max(0, f.row + step[0])),
        col: Math.min(7, Math.max(0, f.col + step[1])),
      }))
    } else if (event.key === 'Escape') {
      setSelection(NO_SELECTION)
    }
  }

  const label = (square: number, piece: PieceView | undefined) => {
    const content = piece
      ? t(piece.king ? 'board.king' : 'board.man', { color: t(`color.${piece.color}`) })
      : t('board.empty')
    const hints = [
      selection.from === square ? t('board.selected') : null,
      targets.has(square) ? t('board.target') : null,
      selection.from === null && movable.has(square) ? t('board.movable') : null,
    ].filter(Boolean)
    return [t('board.square', { square }), content, ...hints].join(', ')
  }

  const rows = []
  for (let displayRow = 0; displayRow < 8; displayRow++) {
    const cells = []
    for (let displayCol = 0; displayCol < 8; displayCol++) {
      const row = flipped ? 7 - displayRow : displayRow
      const col = flipped ? 7 - displayCol : displayCol
      const square = squareAt(row, col)
      const focused = focus.row === displayRow && focus.col === displayCol
      const piece = square ? bySquare.get(square) : undefined
      const classes = ['cell', square ? 'dark' : 'light']
      if (square && selection.from === square) classes.push('selected')
      if (square && selection.path.includes(square)) classes.push('waypoint')
      if (square && targets.has(square)) classes.push('target')
      if (square && selection.from === null && movable.has(square)) classes.push('movable')
      if (square && lastSquares.has(square)) classes.push('last')
      cells.push(
        <div
          key={displayCol}
          role="gridcell"
          className={classes.join(' ')}
          tabIndex={focused ? 0 : -1}
          aria-label={square ? label(square, piece) : undefined}
          aria-selected={square ? selection.from === square : undefined}
          data-square={square ?? undefined}
          onClick={() => {
            setFocus({ row: displayRow, col: displayCol })
            if (square) activate(square)
          }}
          onKeyDown={(e) => {
            if (square && (e.key === 'Enter' || e.key === ' ')) {
              e.preventDefault()
              activate(square)
            }
          }}
        >
          {square && <span className="square-number" aria-hidden="true">{square}</span>}
          {piece && <Piece color={piece.color} king={piece.king} />}
        </div>,
      )
    }
    rows.push(
      <div role="row" className="board-row" key={displayRow}>
        {cells}
      </div>,
    )
  }

  return (
    <div className="board-wrap">
      <div role="grid" aria-label={t('board.label')} className="board" onKeyDown={onKeyDown} ref={gridRef}>
        {rows}
      </div>
      <p className="board-hint" aria-live="polite">
        {mustCapture ? t('board.mustCapture') : selection.path.length > 0 ? t('board.continueJump') : ' '}
      </p>
    </div>
  )
}

function Piece({ color, king }: { color: Color; king: boolean }) {
  return (
    <svg className={`piece piece-${color.toLowerCase()}`} viewBox="0 0 100 100" aria-hidden="true">
      <circle cx="50" cy="54" r="40" className="piece-shadow" />
      <circle cx="50" cy="50" r="40" className="piece-body" />
      <circle cx="50" cy="50" r="28" className="piece-ring" />
      {king && <path className="piece-crown" d="M30 60 L33 38 L42 50 L50 34 L58 50 L67 38 L70 60 Z" />}
    </svg>
  )
}
