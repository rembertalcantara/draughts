import { useState, type FormEvent } from 'react'
import { useNavigate, Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import { keys, useLobby } from '../../api/queries'
import type { ColorChoice, Difficulty, GameSummary, Opponent } from '../../api/types'

export function LobbyPage() {
  return (
    <div className="lobby-layout">
      <NewGameForm />
      <OpenGames />
    </div>
  )
}

function NewGameForm() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const client = useQueryClient()
  const [opponent, setOpponent] = useState<Opponent>('AI')
  const [color, setColor] = useState<ColorChoice>('BLACK')
  const [difficulty, setDifficulty] = useState<Difficulty>('MEDIUM')
  const create = useMutation({
    mutationFn: api.createGame,
    onSuccess: (game) => {
      client.setQueryData(keys.game(game.id), game)
      void navigate(`/games/${game.id}`)
    },
  })

  const submit = (event: FormEvent) => {
    event.preventDefault()
    create.mutate({ opponent, color, difficulty: opponent === 'AI' ? difficulty : undefined })
  }

  return (
    <form className="panel" onSubmit={submit} aria-labelledby="new-game-heading">
      <h2 id="new-game-heading">{t('lobby.newGame')}</h2>
      <Choice legend={t('lobby.opponent')} name="opponent" value={opponent} onChange={setOpponent}
        options={[['AI', t('lobby.computer')], ['HUMAN', t('lobby.friend')]]} />
      <Choice legend={t('lobby.yourColor')} name="color" value={color} onChange={setColor}
        options={(['BLACK', 'WHITE', 'RANDOM'] as const).map((c) => [c, t(`color.${c}`)])} />
      {opponent === 'AI' && (
        <Choice legend={t('lobby.difficulty')} name="difficulty" value={difficulty} onChange={setDifficulty}
          options={(['EASY', 'MEDIUM', 'HARD'] as const).map((d) => [d, t(`difficulty.${d}`)])} />
      )}
      <button type="submit" className="primary" disabled={create.isPending}>{t('lobby.start')}</button>
      {create.error && <p role="alert" className="error">{create.error.message}</p>}
    </form>
  )
}

function Choice<T extends string>({ legend, name, value, options, onChange }: {
  legend: string
  name: string
  value: T
  options: [T, string][]
  onChange: (value: T) => void
}) {
  return (
    <fieldset className="choice">
      <legend>{legend}</legend>
      {options.map(([option, label]) => (
        <label key={option}>
          <input type="radio" name={name} value={option} checked={value === option} onChange={() => onChange(option)} />
          <span>{label}</span>
        </label>
      ))}
    </fieldset>
  )
}

function OpenGames() {
  const { t } = useTranslation()
  const lobby = useLobby()
  return (
    <section className="panel" aria-labelledby="open-games-heading">
      <h2 id="open-games-heading">{t('lobby.openGames')}</h2>
      {lobby.isPending && <p>{t('common.loading')}</p>}
      {lobby.error && <p role="alert" className="error">{lobby.error.message}</p>}
      {lobby.data?.length === 0 && <p className="muted">{t('lobby.noOpenGames')}</p>}
      <ul className="game-list">
        {lobby.data?.map((game) => <OpenGame key={game.id} game={game} />)}
      </ul>
    </section>
  )
}

function OpenGame({ game }: { game: GameSummary }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const join = useMutation({
    mutationFn: () => api.join(game.id),
    onSuccess: () => void navigate(`/games/${game.id}`),
  })
  const creatorColor = game.black ? 'BLACK' : 'WHITE'
  const creator = game.black ?? game.white
  return (
    <li>
      <span>{t('lobby.createdBy', { name: creator?.name, color: t(`color.${creatorColor}`) })}</span>
      {game.yourColor ? (
        <Link to={`/games/${game.id}`}>{t('lobby.yours')}</Link>
      ) : (
        <button type="button" onClick={() => join.mutate()} disabled={join.isPending}>{t('lobby.join')}</button>
      )}
      {join.error && <p role="alert" className="error">{join.error.message}</p>}
    </li>
  )
}
