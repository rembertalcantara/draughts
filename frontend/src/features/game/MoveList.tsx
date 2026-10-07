import { useTranslation } from 'react-i18next'
import type { PlayedMove } from '../../api/types'

/** Moves in pairs: "1. 11-15 23-19". */
export function MoveList({ moves }: { moves: PlayedMove[] }) {
  const { t } = useTranslation()
  const rows: { number: number; black?: PlayedMove; white?: PlayedMove }[] = []
  for (const move of moves) {
    const number = Math.ceil(move.ply / 2)
    let row = rows[rows.length - 1]
    if (!row || row.number !== number) {
      row = { number }
      rows.push(row)
    }
    row[move.color === 'BLACK' ? 'black' : 'white'] = move
  }
  return (
    <section className="panel" aria-labelledby="moves-heading">
      <h2 id="moves-heading">{t('game.moves')}</h2>
      {rows.length === 0 ? (
        <p className="muted">{t('game.noMoves')}</p>
      ) : (
        <ol className="move-list">
          {rows.map((row) => (
            <li key={row.number}>
              <span className="move-number">{row.number}.</span>
              <span>{row.black?.notation ?? '…'}</span>
              <span>{row.white?.notation ?? ''}</span>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
