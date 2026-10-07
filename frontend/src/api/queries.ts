import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type { GameView, LegalMove } from './types'
import { applyMoveLocally } from '../features/game/optimistic'

export const keys = {
  me: ['me'] as const,
  lobby: ['lobby'] as const,
  history: ['history'] as const,
  game: (id: string) => ['game', id] as const,
}

export const useMe = () => useQuery({ queryKey: keys.me, queryFn: api.me, staleTime: Infinity })

export const useLobby = () => useQuery({ queryKey: keys.lobby, queryFn: api.lobby, refetchInterval: 5_000 })

export const useHistory = () => useQuery({ queryKey: keys.history, queryFn: api.myGames })

export const useGame = (id: string) => useQuery({ queryKey: keys.game(id), queryFn: () => api.game(id) })

/**
 * Merges a server snapshot into the cache. Broadcasts carry no `yourColor`, so the value from
 * the per-player HTTP response is kept; stale snapshots (lower version) are ignored.
 */
export function mergeGame(previous: GameView | undefined, next: GameView): GameView {
  if (previous && next.version < previous.version) {
    return previous
  }
  return { ...next, yourColor: next.yourColor ?? previous?.yourColor ?? null }
}

export function usePlayMove(id: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (move: LegalMove & { expectedVersion: number }) =>
      api.move(id, { from: move.from, path: move.path, expectedVersion: move.expectedVersion }),
    onMutate: async (move) => {
      await client.cancelQueries({ queryKey: keys.game(id) })
      const previous = client.getQueryData<GameView>(keys.game(id))
      if (previous) {
        client.setQueryData(keys.game(id), applyMoveLocally(previous, move))
      }
      return { previous }
    },
    onError: (_error, _move, context) => {
      if (context?.previous) {
        client.setQueryData(keys.game(id), context.previous)
      }
      void client.invalidateQueries({ queryKey: keys.game(id) })
    },
    onSuccess: (game) => client.setQueryData<GameView>(keys.game(id), (old) => mergeGame(old, game)),
  })
}

export function useGameAction(id: string, action: (id: string) => Promise<GameView>) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => action(id),
    onSuccess: (game) => client.setQueryData<GameView>(keys.game(id), (old) => mergeGame(old, game)),
  })
}
