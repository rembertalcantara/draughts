import type { CreateGameRequest, GameSummary, GameView, Me, MoveRequest, Problem } from './types'

export class ApiError extends Error {
  readonly problem: Problem

  constructor(problem: Problem) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${problem.status}`)
    this.problem = problem
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    credentials: 'same-origin',
    ...init,
    headers: { Accept: 'application/json', ...(init?.body ? { 'Content-Type': 'application/json' } : {}) },
  })
  if (!response.ok) {
    let problem: Problem = { status: response.status }
    try {
      problem = { ...problem, ...((await response.json()) as Partial<Problem>) }
    } catch {
      // not a JSON body; keep the status only
    }
    throw new ApiError(problem)
  }
  return (await response.json()) as T
}

const post = <T>(path: string, body?: unknown) =>
  request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })

export const api = {
  me: () => request<Me>('/api/me'),
  rename: (displayName: string) =>
    request<Me>('/api/me', { method: 'PATCH', body: JSON.stringify({ displayName }) }),
  lobby: () => request<GameSummary[]>('/api/lobby'),
  myGames: () => request<GameSummary[]>('/api/games'),
  game: (id: string) => request<GameView>(`/api/games/${id}`),
  createGame: (body: CreateGameRequest) => post<GameView>('/api/games', body),
  join: (id: string) => post<GameView>(`/api/games/${id}/join`),
  move: (id: string, body: MoveRequest) => post<GameView>(`/api/games/${id}/moves`, body),
  resign: (id: string) => post<GameView>(`/api/games/${id}/resign`),
  offerDraw: (id: string) => post<GameView>(`/api/games/${id}/draw-offer`),
  declineDraw: (id: string) => post<GameView>(`/api/games/${id}/draw-decline`),
}
