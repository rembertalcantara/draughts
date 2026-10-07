import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { NavLink, Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { keys, useMe } from '../api/queries'
import { useSettings, type Theme } from '../store/settings'

export function Layout({ children }: { children: ReactNode }) {
  const { t, i18n } = useTranslation()
  const { theme, setTheme, language, setLanguage } = useSettings()

  useEffect(() => {
    if (theme === 'system') {
      delete document.documentElement.dataset.theme
    } else {
      document.documentElement.dataset.theme = theme
    }
  }, [theme])

  useEffect(() => {
    void i18n.changeLanguage(language)
    document.documentElement.lang = language
  }, [i18n, language])

  return (
    <>
      <a className="skip-link" href="#main">{t('app.skip')}</a>
      <header className="app-header">
        <Link to="/" className="brand">{t('app.title')}</Link>
        <nav aria-label="Main">
          <NavLink to="/" end>{t('app.play')}</NavLink>
          <NavLink to="/history">{t('app.history')}</NavLink>
        </nav>
        <div className="header-tools">
          <PlayerName />
          <label>
            <span className="visually-hidden">{t('settings.theme')}</span>
            <select value={theme} onChange={(e) => setTheme(e.target.value as Theme)}>
              {(['system', 'light', 'dark'] as const).map((value) => (
                <option key={value} value={value}>{t(`settings.${value}`)}</option>
              ))}
            </select>
          </label>
          <label>
            <span className="visually-hidden">{t('settings.language')}</span>
            <select value={language} onChange={(e) => setLanguage(e.target.value)}>
              <option value="en">English</option>
              <option value="pt">Português</option>
            </select>
          </label>
        </div>
      </header>
      <main id="main">{children}</main>
    </>
  )
}

function PlayerName() {
  const { t } = useTranslation()
  const me = useMe()
  const client = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState('')
  const rename = useMutation({
    mutationFn: api.rename,
    onSuccess: (updated) => {
      client.setQueryData(keys.me, updated)
      setEditing(false)
    },
  })

  if (!me.data) {
    return null
  }
  if (!editing) {
    return (
      <button type="button" className="link" onClick={() => { setName(me.data.displayName); setEditing(true) }}
        aria-label={`${t('settings.name')}: ${me.data.displayName}. ${t('common.edit')}`}>
        {me.data.displayName}
      </button>
    )
  }
  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (name.trim()) rename.mutate(name.trim())
  }
  return (
    <form className="inline-form" onSubmit={submit}>
      <label>
        <span className="visually-hidden">{t('settings.name')}</span>
        <input value={name} maxLength={40} onChange={(e) => setName(e.target.value)} autoFocus />
      </label>
      <button type="submit" disabled={rename.isPending}>{t('common.save')}</button>
      <button type="button" onClick={() => setEditing(false)}>{t('common.cancel')}</button>
    </form>
  )
}
