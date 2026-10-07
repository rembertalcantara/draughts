import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useHistory } from '../../api/queries'

export function HistoryPage() {
  const { t, i18n } = useTranslation()
  const history = useHistory()
  const date = new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium', timeStyle: 'short' })

  return (
    <section className="panel" aria-labelledby="history-heading">
      <h2 id="history-heading">{t('history.title')}</h2>
      {history.isPending && <p>{t('common.loading')}</p>}
      {history.error && <p role="alert" className="error">{history.error.message}</p>}
      {history.data?.length === 0 && <p className="muted">{t('history.empty')}</p>}
      <ul className="game-list">
        {history.data?.map((game) => {
          const opponent = game.yourColor === 'BLACK' ? game.white : game.black
          return (
            <li key={game.id}>
              <span>
                {t('history.vs')} <strong>{opponent?.name ?? t('game.waiting')}</strong>
                {game.aiDifficulty && ` (${t(`difficulty.${game.aiDifficulty}`)})`}
              </span>
              <span className="muted">{t(`result.${game.result ?? game.status}`)} · {t('history.moves', { count: game.moveCount })}</span>
              <time className="muted" dateTime={game.updatedAt}>{date.format(new Date(game.updatedAt))}</time>
              <Link to={`/games/${game.id}`}>{t('history.open')}</Link>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
