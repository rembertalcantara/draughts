import type { TFunction } from 'i18next'
import type { GameView } from '../../api/types'

/** One-line description of where the game stands, from the viewer's point of view. */
export function describeStatus(game: GameView, t: TFunction): string {
  if (game.status === 'OPEN') {
    return t('status.waitingForOpponent')
  }
  if (game.status === 'FINISHED') {
    const reason = game.resultReason ? t(`reason.${game.resultReason}`) : ''
    if (game.result === 'DRAW') {
      return t('status.draw', { reason })
    }
    const winner = game.result === 'BLACK_WIN' ? 'BLACK' : 'WHITE'
    if (game.yourColor) {
      return t(winner === game.yourColor ? 'status.youWon' : 'status.youLost', { reason })
    }
    return t('status.won', { color: t(`color.${winner}`), reason })
  }
  if (game.yourColor === game.turn) {
    return t('status.yourTurn')
  }
  const side = game.turn === 'BLACK' ? game.black : game.white
  if (side?.computer) {
    return t('status.computerThinking')
  }
  return t('status.turnOf', { name: side?.name ?? t(`color.${game.turn}`) })
}
