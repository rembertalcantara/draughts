# Draughts (Checkers) — Technical Specification

## 1. Overview
A web-based draughts game with a **React** single-page frontend and a **Java 25 / Spring Boot 4** backend. Players can play locally against the computer or online against other players, with games persisted and replayable.

Rule variant for v1: **English draughts (American checkers)**, 8×8 board. Rules live behind a `RuleSet` interface so other variants (Russian, International 10×10, Brazilian) can be added later without touching transport or persistence.

## 2. Goals and Non-Goals
**Goals**
- Fully enforced, well-tested rules (mandatory capture, multi-jump, promotion).
- Server-authoritative game state: clients cannot cheat.
- Human vs. computer and human vs. human (real time).
- Persisted games with move history, replay and resume.
- Clean separation between the rules engine and everything else (web, DB, AI).

**Non-Goals (v1)**
- Other variants, tournaments, chat, native mobile apps, monetization.

## 3. Technology Stack

| Layer | Choice | Notes |
|---|---|---|
| Frontend | **React 19.x** (latest stable) + **TypeScript** | React has no formal LTS line; we pin the latest stable major and upgrade deliberately. |
| Frontend tooling | **Vite**, **Node 24 LTS** | Node LTS for build/dev only. Vitest, React Testing Library, Playwright. |
| Frontend state | **TanStack Query** (server state) + **Zustand** (UI/ephemeral state) | Avoids Redux boilerplate; WebSocket events update the Query cache. |
| Backend | **Java 25 (LTS)**, **Spring Boot 4.x** (Spring Framework 7, Jakarta EE 11) | Use records, sealed types, pattern matching, virtual threads. |
| Realtime | **WebSocket + STOMP** (Spring Messaging) | SSE is a viable simpler fallback for spectate-only. |
| Persistence | **PostgreSQL 17+** | See section 8. |
| Migrations | **Flyway** | Versioned SQL, run at startup. |
| Data access | **Spring Data JDBC** (or jOOQ) | Preferred over JPA/Hibernate: the domain is small, immutable and aggregate-shaped. |
| Cache / presence | **Redis** (optional, phase 2) | Only for horizontal scaling: pub/sub for WebSocket fan-out, matchmaking queue. |
| Security | **Spring Security 7** | Guest sessions in v1; OAuth2/OIDC login later. |
| API docs | **springdoc-openapi** | TypeScript client generated from the OpenAPI spec. |
| Build | **Maven** or Gradle (Kotlin DSL); monorepo | `backend/` + `frontend/` in one repo. |
| Observability | Spring Boot Actuator, Micrometer, OpenTelemetry | |
| Delivery | Docker, Docker Compose for local dev, GitHub Actions CI | |

> Verify exact versions against current releases when scaffolding; Spring Boot 4 was a major release (modularized starters, Jackson 3), so use the Spring Initializr to generate the baseline.

## 4. Game Rules (English Draughts)

### 4.1 Board
- 8×8, only the 32 dark squares are playable, numbered 1–32.
- Each side starts with 12 men on the three rows nearest them.

### 4.2 Pieces
- **Man**: moves/captures one square diagonally forward.
- **King**: a promoted man; moves/captures one square diagonally in any direction (non-flying).

### 4.3 Turns and captures
- Black moves first; players alternate.
- **Capturing is mandatory.** If any capture exists, only capturing moves are legal.
- **Multi-jump**: if the capturing piece can continue, it must, within the same turn.
- The player may choose between capture sequences (no majority rule).
- A piece cannot be jumped twice in a single sequence.
- A man that reaches the king row mid-capture ends its turn there.

### 4.4 End of game
- **Win**: opponent has no pieces or no legal move.
- **Draw**: agreement, 40 moves without a capture or man move, or threefold repetition.
- **Other results**: resignation, timeout (if clocks are enabled), abandonment.

## 5. Architecture

### 5.1 Style: modular monolith, hexagonal core
One deployable Spring Boot application split into modules with enforced boundaries (Spring Modulith or ArchUnit). A monolith is the right size here; microservices would add cost with no benefit.

```
draughts/
├── backend/
│   └── src/main/java/.../draughts/
│       ├── engine/      # PURE Java: board, moves, rules. No Spring, no I/O.
│       ├── ai/          # Computer opponent (depends on engine only)
│       ├── game/        # Application services: create/join/move/resign; ports
│       ├── matchmaking/ # Lobby, invitations, queue
│       ├── player/      # Identity, profile, ratings (later)
│       ├── web/         # REST controllers, WebSocket handlers, DTOs
│       └── persistence/ # Adapters implementing game ports (JDBC)
└── frontend/
    └── src/
        ├── features/    # game-board, lobby, history, settings
        ├── api/         # generated OpenAPI client + WebSocket client
        ├── components/  # shared UI
        └── store/
```

Dependency rule: `web` and `persistence` depend on `game`; `game` depends on `engine`; **`engine` depends on nothing**. This keeps the rules testable in milliseconds and reusable (e.g. by the AI or a CLI).

### 5.2 Key design decisions
1. **Server-authoritative.** The client sends a *move intent*; the server validates it with the engine, persists it and broadcasts the result. The client never decides legality.
2. **No duplicated rules in TypeScript.** The server returns `legalMoves` with each game state so the UI can highlight pieces and destinations. This removes a whole class of client/server drift. (If offline play is ever required, compile the engine to WebAssembly or port with a shared test-vector suite.)
3. **Immutable engine.** `GameState` and `Move` are Java records; `apply(state, move)` returns a new state. This makes undo, replay, AI search and testing trivial.
4. **Event-sourced game record.** A game is stored as its initial setup plus an ordered list of moves; the current board is derived (with a snapshot column for fast reads). This gives replay, auditing, repetition detection and PGN-like export for free.
5. **Optimistic concurrency.** Each game has a `version`; a move carries the version it was based on. Conflicting or double-submitted moves are rejected, which also handles reconnect races.
6. **Virtual threads** (`spring.threads.virtual.enabled=true`) for request handling; AI search runs on a bounded executor so it cannot starve request threads.
7. **Variant strategy pattern.** `RuleSet` (board geometry, move generation, promotion, draw rules) is injected per game; the game row stores `variant`.

### 5.3 Engine design (Java 25)
```java
enum Color { BLACK, WHITE }
enum PieceType { MAN, KING }
record Piece(Color color, PieceType type) {}
record Move(int from, List<Integer> path, List<Integer> captured) {}

sealed interface GameStatus permits Ongoing, Won, Drawn {}

interface RuleSet {
    GameState initial();
    List<Move> legalMoves(GameState s);   // captures only if any exist
    GameState apply(GameState s, Move m);
    GameStatus status(GameState s);
}
```
- Board representation: **bitboards** (`long`/`int` masks per color/type) for fast move generation and AI search; a readable `Board` view is derived for DTOs.
- Position hashing (Zobrist) for repetition detection and AI transposition tables.

### 5.4 Computer opponent
- Negamax with alpha-beta pruning, iterative deepening, move ordering (captures first), transposition table.
- Evaluation: material (man 100, king 160), advancement, back-row guard, center control, mobility.
- Difficulty = max depth + time budget (Easy 2, Medium 5, Hard 9 / ~1–2 s). Easy adds controlled randomness.
- Always returns a legal move before the deadline; runs on a dedicated executor.

### 5.5 Real-time flow
1. Client creates or joins a game via REST (`POST /api/games`, `POST /api/games/{id}/join`).
2. Client subscribes to `/topic/games/{id}`.
3. Client sends `/app/games/{id}/move` with `{ from, path, expectedVersion }`.
4. Server validates → persists → publishes `MoveApplied { move, state, legalMoves, version }`.
5. For AI games, the server computes and publishes the AI reply as a second event.
6. On reconnect the client fetches `GET /api/games/{id}` and resubscribes; the version number reconciles state.

### 5.6 REST API (v1 sketch)
| Method & path | Purpose |
|---|---|
| `POST /api/games` | Create a game (`opponent: AI|HUMAN`, `color`, `difficulty`, `variant`) |
| `GET /api/games/{id}` | Current state, legal moves, status |
| `POST /api/games/{id}/join` | Join an open game |
| `POST /api/games/{id}/moves` | Submit a move (REST fallback) |
| `POST /api/games/{id}/resign` / `/draw-offer` | Resign / offer or accept draw |
| `GET /api/games/{id}/moves` | Move list for replay |
| `GET /api/games?mine=true` | Game history |
| `GET /api/lobby` | Open games |

Errors use RFC 9457 `ProblemDetail`. Illegal moves return `422` with a reason code.

## 6. Frontend Design

- **Components**: `Board`, `Square`, `Piece`, `MoveList`, `GameControls`, `Lobby`, `ReplayViewer`, `ResultDialog`.
- **Rendering**: CSS Grid + SVG pieces (crisp at every size, easy theming). Canvas is unnecessary at this scale.
- **Interaction**: click/tap-to-select and drag-and-drop (e.g. `dnd-kit`); legal destinations highlighted; mandatory captures flagged; multi-jumps guided step by step, submitted as one move.
- **Optimistic UI**: the piece moves immediately and rolls back if the server rejects the move.
- **State**: server state in TanStack Query, updated by WebSocket events; local selection/animation state in Zustand or component state.
- **Responsive and accessible**: mobile-first layout; full keyboard control (arrow keys + Enter); ARIA labels per square/piece ("Black king on 14"); WCAG AA contrast; respects `prefers-reduced-motion`.
- **i18n** from the start (`react-i18next`), English and Portuguese initially.
- **Styling**: CSS Modules or Tailwind; light/dark themes.

## 7. Cross-Cutting Concerns
- **Security**: guest identity via signed, HttpOnly session cookie; CSRF protection on REST; WebSocket origin checks and per-message authorization (only the player to move may move); rate limiting on game creation and moves.
- **Validation**: Jakarta Bean Validation at the edge; the engine is the final authority on legality.
- **Testing** (see section 10).
- **Observability**: structured JSON logs with game/player correlation ids; metrics for active games, move latency, AI think time.
- **Resilience**: idempotent move submission via `expectedVersion`; clocks (if enabled) are enforced server-side with scheduled timeout checks, not trusted from clients.

## 8. Database Choice

**Recommendation: PostgreSQL.**

Why it fits this domain:
- **Relational core**: players, games, moves, ratings have clear relationships and need transactional integrity (a move append and game-version bump must be atomic).
- **JSONB**: store the board snapshot or variant-specific options without schema churn, and still index/query it.
- **Strong concurrency control**: row-level locking and `SELECT ... FOR UPDATE` / optimistic versioning for move races.
- **Mature ecosystem**: first-class Spring Data JDBC / jOOQ / Flyway support, excellent Testcontainers support, managed offerings everywhere.
- **Room to grow**: `LISTEN/NOTIFY` or logical replication if needed; extensions for analytics later.

Why not the alternatives:
- **MongoDB**: a game document looks like a natural fit, but we still need transactions across games/players and relational queries (history, leaderboards); the flexibility gain is small.
- **Redis as primary store**: great for ephemeral state but not for durable game history.
- **H2/SQLite**: fine for tests/demo, not for concurrent online play.

**Redis** is added only when scaling out to multiple backend instances (WebSocket broadcast relay, matchmaking queue, presence, rate limits). It is a cache/bus, never the source of truth.

### 8.1 Schema sketch
```sql
player(id uuid pk, display_name text, kind text, created_at timestamptz)

game(
  id uuid pk, variant text not null, status text not null,
  black_player_id uuid null references player,
  white_player_id uuid null references player,
  ai_difficulty smallint null,
  current_turn text not null,
  board_snapshot jsonb not null,       -- derived, for fast reads
  result text null, version int not null,
  created_at timestamptz, updated_at timestamptz
)

game_move(
  game_id uuid references game, ply int not null,
  from_sq smallint, path smallint[], captured smallint[],
  played_by uuid, played_at timestamptz,
  primary key (game_id, ply)
)

index game(status, created_at) where status = 'OPEN'   -- lobby
index game(black_player_id), game(white_player_id)     -- history
```
`game_move` is append-only; `game.board_snapshot` can always be rebuilt by replaying it.

## 9. Environments and Delivery
- **Local**: `docker compose up` (Postgres, optional Redis, backend, Vite dev server with proxy).
- **CI (GitHub Actions)**: backend build + tests (Testcontainers), frontend lint/typecheck/test, OpenAPI client drift check, Playwright E2E, container image build.
- **Deploy**: a single container for the backend (the built frontend can be served by Spring as static assets or by a CDN/Nginx); managed PostgreSQL. Configuration via environment variables (12-factor).
- **Java**: use a Temurin/OpenJDK 25 base image with virtual threads on; consider CDS/AOT for faster startup.

## 10. Testing Strategy
- **Engine (JUnit 5, AssertJ)**: every rule — simple moves, forced capture, multi-jump, mid-capture promotion, men cannot move backward, king movement, terminal states, draw rules.
- **Property-based (jqwik)**: random playouts never reach illegal states; piece count never increases; `apply` then replay is deterministic.
- **Known positions**: move-count (perft-style) tests against published values for the English draughts start position.
- **AI**: always returns a legal move within the budget; finds forced wins in curated positions.
- **Backend integration**: `@SpringBootTest` + Testcontainers PostgreSQL; WebSocket tests with `StompSession`; concurrency tests for racing moves.
- **Architecture tests**: ArchUnit/Spring Modulith verify `engine` has no outward dependencies.
- **Frontend**: Vitest + React Testing Library for components; MSW for API mocks; Playwright E2E (play a full game, reconnect mid-game, two browsers).
- **Accessibility**: axe checks in CI.

## 11. Milestones
1. **Engine**: rules, bitboards, exhaustive tests.
2. **Backend core**: game service, REST API, PostgreSQL + Flyway, OpenAPI.
3. **Frontend MVP**: board, play vs. AI over REST.
4. **AI**: alpha-beta search, difficulty levels.
5. **Real-time multiplayer**: WebSocket/STOMP, lobby, reconnect, draw/resign.
6. **Polish**: replay, history, i18n, accessibility, themes.
7. **Later**: auth providers, ratings, clocks, Redis scale-out, additional variants.

## 12. Risks and Open Questions
| Item | Notes |
|---|---|
| "React LTS" | React has no LTS; we track the latest stable major (19.x). Confirm that is acceptable. |
| Spring Boot 4 maturity | Newer major; pin versions and verify third-party library compatibility (e.g. Jackson 3). |
| Client-side rules | Decision is server-provided legal moves. Revisit only if offline play is required. |
| Clocks | In or out of v1? Affects server scheduling and the data model. |
| Accounts | Guest-only first, or social login from day one? |
| Additional variants | Which, and in what order? |
| Build tool | Maven vs. Gradle — team preference. |
