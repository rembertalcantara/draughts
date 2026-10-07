import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import { useGameAction } from '../../api/queries'
import type { GameView } from '../../api/types'

export function GameControls({ game, onFlip }: { game: GameView; onFlip: () => void }) {
  const { t } = useTranslation()
  const resign = useGameAction(game.id, api.resign)
  const offerDraw = useGameAction(game.id, api.offerDraw)
  const declineDraw = useGameAction(game.id, api.declineDraw)
  const playing = game.status === 'IN_PROGRESS' && game.yourColor !== null
  const opponentOffered = game.drawOfferedBy !== null && game.drawOfferedBy !== game.yourColor
  const youOffered = game.drawOfferedBy !== null && game.drawOfferedBy === game.yourColor
  const error = resign.error ?? offerDraw.error ?? declineDraw.error

  return (
    <section className="panel controls" aria-label={t('game.controls')}>
      <button type="button" onClick={onFlip}>{t('game.flip')}</button>
      {playing && (
        <>
          {opponentOffered ? (
            <>
              <button type="button" className="primary" onClick={() => offerDraw.mutate()}>{t('game.acceptDraw')}</button>
              <button type="button" onClick={() => declineDraw.mutate()}>{t('game.declineDraw')}</button>
            </>
          ) : (
            <button type="button" disabled={youOffered || offerDraw.isPending} onClick={() => offerDraw.mutate()}>
              {youOffered ? t('game.drawOffered') : t('game.offerDraw')}
            </button>
          )}
          <button
            type="button"
            className="danger"
            disabled={resign.isPending}
            onClick={() => {
              if (window.confirm(t('game.confirmResign'))) resign.mutate()
            }}
          >
            {t('game.resign')}
          </button>
        </>
      )}
      {error && <p role="alert" className="error">{error.message}</p>}
    </section>
  )
}
