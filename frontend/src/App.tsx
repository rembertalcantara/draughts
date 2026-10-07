import { BrowserRouter, Route, Routes } from 'react-router'
import { Layout } from './components/Layout'
import { GamePage } from './features/game/GamePage'
import { HistoryPage } from './features/history/HistoryPage'
import { LobbyPage } from './features/lobby/LobbyPage'

export function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<LobbyPage />} />
          <Route path="/games/:id" element={<GamePage />} />
          <Route path="/history" element={<HistoryPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  )
}
