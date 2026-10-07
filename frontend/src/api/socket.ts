import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { keys, mergeGame } from './queries'
import type { GameView } from './types'

function brokerUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

/** Subscribes to live updates for a game and writes them into the query cache. */
export function useGameUpdates(gameId: string) {
  const client = useQueryClient()

  useEffect(() => {
    const stomp = new Client({
      brokerURL: brokerUrl(),
      reconnectDelay: 2_000,
      onConnect: () => {
        // Catch up on anything missed while disconnected, then follow live updates.
        void client.invalidateQueries({ queryKey: keys.game(gameId) })
        stomp.subscribe(`/topic/games/${gameId}`, (message) => {
          const update = JSON.parse(message.body) as GameView
          client.setQueryData<GameView>(keys.game(gameId), (old) => mergeGame(old, update))
        })
      },
    })
    stomp.activate()
    return () => {
      void stomp.deactivate()
    }
  }, [client, gameId])
}
