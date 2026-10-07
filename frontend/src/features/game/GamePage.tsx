import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import { useGame, useGameAction, usePlayMove } from '../../api/queries'
import type { LegalMove } from '../../api/types'
import { useGameUpdates } from '../../api/socket'
import { useSettings } from '../../store/settings'
import { Board } from './Board'
import { GameControls } from './GameControls'
import { MoveList } from './MoveList'
import { describeStatus } from './status'

const NO_MOVES: LegalMove[] = []

export function GamePage() {
  const { id = '' } = useParams()
  const { t } = useTranslation()
  const game = useGame(id)
  const play = usePlayMove(id)
  const flipPreference = useSettings((s) => s.flipBoard)
  const [manualFlip, setManualFlip] = useState(false)
  useGameUpdates(id)

  if (game.isPending) {
    return <p>{t('common.loading')}</p>
  }
  if (game.isError) {
    return <p role="alert" className="error">{game.error.message}</p>
  }

  const view = game.data
  const myTurn = view.status === 'IN_PROGRESS' && view.yourColor === view.turn && !play.isPending
  // Show your own pieces at the bottom: Black starts at the top of the standard diagram.
  const flipped = (view.yourColor === 'BLACK' && flipPreference) !== manualFlip
  const last = view.moves[view.moves.length - 1]

  return (
    <div className="game-layout">
      <div>
        <PlayerTag name={(flipped ? view.white : view.black)?.name} color={flipped ? 'WHITE' : 'BLACK'} />
        <Board
          pieces={view.board}
          legalMoves={myTurn ? view.legalMoves : NO_MOVES}
          flipped={flipped}
          lastMove={last}
          onMove={(move) => play.mutate({ ...move, expectedVersion: view.version })}
        />
        <PlayerTag name={(flipped ? view.black : view.white)?.name} color={flipped ? 'BLACK' : 'WHITE'} />
      </div>
      <aside className="sidebar">
        <section className="panel">
          <p className="status" aria-live="polite">{describeStatus(view, t)}</p>
          {view.status === 'OPEN' && (view.yourColor ? <ShareLink /> : <JoinButton gameId={view.id} />)}
          {view.drawOfferedBy && view.status === 'IN_PROGRESS' && view.drawOfferedBy !== view.yourColor && (
            <p className="notice">{t('game.drawOfferReceived')}</p>
          )}
          {play.error && <p role="alert" className="error">{play.error.message}</p>}
        </section>
        <GameControls game={view} onFlip={() => setManualFlip((f) => !f)} />
        <MoveList moves={view.moves} />
      </aside>
    </div>
  )
}

function PlayerTag({ name, color }: { name?: string; color: 'BLACK' | 'WHITE' }) {
  const { t } = useTranslation()
  return (
    <div className="player-tag">
      <span className={`dot dot-${color.toLowerCase()}`} aria-hidden="true" />
      <span>{name ?? t('game.waiting')}</span>
    </div>
  )
}

function ShareLink() {
  const { t } = useTranslation()
  const [copied, setCopied] = useState(false)
  return (
    <p>
      {t('game.share')}{' '}
      <button
        type="button"
        onClick={() => {
          void navigator.clipboard?.writeText(window.location.href).then(() => setCopied(true))
        }}
      >
        {copied ? t('game.copied') : t('game.copyLink')}
      </button>
    </p>
  )
}

function JoinButton({ gameId }: { gameId: string }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const join = useGameAction(gameId, api.join)
  return (
    <>
      <button type="button" className="primary" disabled={join.isPending}
        onClick={() => join.mutate(undefined, { onSuccess: () => navigate(`/games/${gameId}`) })}>
        {t('lobby.join')}
      </button>
      {join.error && <p role="alert" className="error">{join.error.message}</p>}
    </>
  )
}
