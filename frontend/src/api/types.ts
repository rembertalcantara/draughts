// Mirrors com.draughts.web.dto.ApiDtos on the backend.

export type Color = 'BLACK' | 'WHITE'
export type Lifecycle = 'OPEN' | 'IN_PROGRESS' | 'FINISHED'
export type Opponent = 'AI' | 'HUMAN'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type ColorChoice = Color | 'RANDOM'
export type GameResult = 'BLACK_WIN' | 'WHITE_WIN' | 'DRAW'
export type ResultReason = 'NO_PIECES' | 'NO_MOVES' | 'MOVE_LIMIT' | 'REPETITION' | 'RESIGNATION' | 'AGREEMENT'

export interface PlayerView {
  name: string
  computer: boolean
}

export interface Me {
  id: string
  displayName: string
}

export interface PieceView {
  square: number
  color: Color
  king: boolean
}

export interface LegalMove {
  from: number
  path: number[]
  captured: number[]
  notation: string
}

export interface PlayedMove {
  ply: number
  color: Color
  from: number
  path: number[]
  captured: number[]
  notation: string
}

export interface GameView {
  id: string
  variant: string
  status: Lifecycle
  opponent: Opponent
  aiDifficulty: Difficulty | null
  turn: Color
  version: number
  fen: string
  board: PieceView[]
  legalMoves: LegalMove[]
  moves: PlayedMove[]
  black: PlayerView | null
  white: PlayerView | null
  yourColor: Color | null
  drawOfferedBy: Color | null
  result: GameResult | null
  resultReason: ResultReason | null
  createdAt: string
  updatedAt: string
}

export interface GameSummary {
  id: string
  variant: string
  status: Lifecycle
  opponent: Opponent
  aiDifficulty: Difficulty | null
  black: PlayerView | null
  white: PlayerView | null
  yourColor: Color | null
  turn: Color
  result: GameResult | null
  resultReason: ResultReason | null
  moveCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateGameRequest {
  opponent: Opponent
  color: ColorChoice
  difficulty?: Difficulty
}

export interface MoveRequest {
  from: number
  path: number[]
  expectedVersion?: number
}

/** RFC 9457 problem details as returned by the backend. */
export interface Problem {
  status: number
  title?: string
  detail?: string
  code?: string
}
